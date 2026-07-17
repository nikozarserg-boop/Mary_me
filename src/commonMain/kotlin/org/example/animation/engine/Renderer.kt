package org.example.animation.engine

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import org.example.animation.io.decodeImage
import org.example.animation.model.AnimationProject
import org.example.animation.model.BrushShape
import org.example.animation.model.ImageElement
import org.example.animation.model.Stroke as StrokeModel
import kotlin.math.*

object Renderer {
    private val imageCache = mutableMapOf<Long, ImageBitmap>()
    private val stampCache = mutableMapOf<Long, ImageBitmap>()

    fun renderFrame(
        drawScope: DrawScope,
        project: AnimationProject,
        frameIndex: Int,
        alpha: Float = 1f,
        backgroundColor: Color = Color.White
    ) {
        with(drawScope) {
            val cw = project.canvasWidth.toFloat()
            val ch = project.canvasHeight.toFloat()
            
            // Фон
            drawRect(color = backgroundColor, size = Size(cw, ch))

            // Слои
            for (layer in project.layers) {
                if (!layer.isVisible) continue
                val frame = layer.frames.getOrNull(frameIndex) ?: continue
                
                // Изображения
                for (image in frame.images) {
                    drawImageElement(drawScope, image, alpha)
                }

                // Штрихи
                for (stroke in frame.strokes) {
                    drawStroke(drawScope, stroke, null, alpha)
                }
            }
        }
    }

    fun drawStroke(drawScope: DrawScope, stroke: StrokeModel, ghostColor: Color? = null, alpha: Float = 1f, stamps: Map<Long, ByteArray>? = null) {
        with(drawScope) {
            val points = stroke.points
            if (points.isEmpty()) return
            val effectiveAlpha = alpha * stroke.opacity
            
            val color = if (stroke.isEraser) {
                Color.White.copy(alpha = effectiveAlpha)
            } else {
                ghostColor ?: ulongToColor(stroke.color).copy(alpha = effectiveAlpha)
            }

            if (stroke.brushShape == BrushShape.TEXTURE && stroke.stampId != null) {
                drawTexturedStroke(drawScope, stroke, color, stamps)
                return
            }

            if (points.size == 1) {
                drawCircle(color = color, radius = stroke.strokeWidth / 2f, center = points[0])
                return
            }

            val path = Path()
            path.moveTo(points[0].x, points[0].y)
            for (i in 1 until points.size) {
                path.lineTo(points[i].x, points[i].y)
            }

            drawPath(
                path = path,
                color = color,
                style = Stroke(width = stroke.strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }
    }

    private fun drawTexturedStroke(drawScope: DrawScope, stroke: StrokeModel, color: Color, stamps: Map<Long, ByteArray>?) {
        val stampId = stroke.stampId ?: return
        var bitmap = stampCache[stampId]
        
        if (bitmap == null && stamps != null) {
            stamps[stampId]?.let {
                bitmap = decodeImage(it)
                if (bitmap != null) stampCache[stampId] = bitmap!!
            }
        }

        val brushBitmap = bitmap ?: return

        with(drawScope) {
            val points = stroke.points
            if (points.isEmpty()) return

            val spacing = max(1f, stroke.spacing * stroke.strokeWidth)
            val filter = ColorFilter.tint(color)

            var lastPos = points[0]
            drawStamp(drawScope, brushBitmap, lastPos, stroke, filter)

            var distanceTraveled = 0f
            for (i in 1 until points.size) {
                val p1 = points[i - 1]
                val p2 = points[i]
                val segmentVector = p2 - p1
                val segmentLen = segmentVector.getDistance()
                if (segmentLen == 0f) continue

                var segmentPos = 0f
                while (segmentPos + (spacing - distanceTraveled) <= segmentLen) {
                    val step = spacing - distanceTraveled
                    segmentPos += step
                    val t = segmentPos / segmentLen
                    val pos = p1 + segmentVector * t
                    drawStamp(drawScope, brushBitmap, pos, stroke, filter)
                    distanceTraveled = 0f
                }
                distanceTraveled += (segmentLen - segmentPos)
            }
        }
    }

    private fun drawStamp(drawScope: DrawScope, bitmap: ImageBitmap, pos: Offset, stroke: StrokeModel, filter: ColorFilter) {
        val size = stroke.strokeWidth
        val angle = stroke.angle + (if (stroke.scatter > 0) (Math.random().toFloat() * 2 - 1) * stroke.scatter * 180 else 0f)
        val alpha = stroke.opacity * stroke.flow
        
        with(drawScope) {
            withTransform({
                translate(pos.x, pos.y)
                rotate(angle)
                scale(size / bitmap.width, size / bitmap.height * stroke.roundness)
                translate(-bitmap.width / 2f, -bitmap.height / 2f)
            }) {
                drawImage(bitmap, alpha = alpha, colorFilter = filter)
            }
        }
    }

    private fun drawImageElement(drawScope: DrawScope, image: ImageElement, alpha: Float) {
        val bitmap = imageCache.getOrPut(image.id) {
            decodeImage(image.data) ?: return
        }

        with(drawScope) {
            withTransform({
                translate(image.x, image.y)
                rotate(image.rotation, Offset(bitmap.width / 2f, bitmap.height / 2f))
                scale(image.scale, image.scale, Offset(bitmap.width / 2f, bitmap.height / 2f))
            }) {
                drawImage(bitmap, alpha = alpha)
            }
        }
    }

    fun ulongToColor(color: ULong): Color {
        val a = ((color shr 24) and 0xFFuL).toInt() / 255f
        val r = ((color shr 16) and 0xFFuL).toInt() / 255f
        val g = ((color shr 8) and 0xFFuL).toInt() / 255f
        val b = (color and 0xFFuL).toInt() / 255f
        return Color(r, g, b, a)
    }

    fun clearCaches() {
        imageCache.clear()
        stampCache.clear()
    }
}
