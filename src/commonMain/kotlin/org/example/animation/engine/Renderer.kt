package org.example.animation.engine

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import org.example.animation.io.decodeImage
import org.example.animation.model.AnimationProject
import org.example.animation.model.ImageElement
import org.example.animation.model.Stroke as StrokeModel

object Renderer {
    private val imageCache = mutableMapOf<Long, ImageBitmap>()

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
            
            // Background
            drawRect(color = backgroundColor, size = Size(cw, ch))

            // Layers
            for (layer in project.layers) {
                if (!layer.isVisible) continue
                val frame = layer.frames.getOrNull(frameIndex) ?: continue
                
                // Images
                for (image in frame.images) {
                    drawImageElement(drawScope, image, alpha)
                }

                // Strokes
                for (stroke in frame.strokes) {
                    drawStroke(drawScope, stroke, alpha)
                }
            }
        }
    }

    private fun drawStroke(drawScope: DrawScope, stroke: StrokeModel, alpha: Float) {
        with(drawScope) {
            val points = stroke.points
            if (points.isEmpty()) return
            val effectiveAlpha = alpha * stroke.opacity
            
            val color = if (stroke.isEraser) {
                Color.White.copy(alpha = effectiveAlpha)
            } else {
                ulongToColor(stroke.color).copy(alpha = effectiveAlpha)
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
}
