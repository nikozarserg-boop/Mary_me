package org.example.animation.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import org.example.animation.engine.AnimationEngine
import org.example.animation.model.Stroke
import org.example.animation.model.ToolType
import org.example.animation.model.ImageElement
import org.example.animation.io.decodeImage
import org.example.animation.ui.theme.*
import kotlin.math.*

// Кэш для декодированных изображений
private val imageCache = mutableMapOf<Long, ImageBitmap>()

@Composable
fun DrawingCanvas(engine: AnimationEngine, modifier: Modifier = Modifier) {
    val project by engine.project.collectAsState()
    val zoom by engine.zoom.collectAsState()
    val panOffset by engine.panOffset.collectAsState()
    val rotation by engine.rotation.collectAsState()
    val currentTool by engine.currentTool.collectAsState()
    val isPlaying by engine.isPlaying.collectAsState()
    val ghostFramesEnabled by engine.ghostFramesEnabled.collectAsState()
    val ghostFramesColorU by engine.ghostFramesColor.collectAsState()
    val ghostFramesColor = remember(ghostFramesColorU) { ulongToColor(ghostFramesColorU).copy(alpha = 0.4f) }

    val activeStroke by engine.activeStroke.collectAsState()

    var shapePreview by remember { mutableStateOf<ShapePreview?>(null) }
    val framesForRender = remember(project, engine.currentFrameIndex.collectAsState().value, ghostFramesEnabled) {
        engine.getFramesForRendering()
    }

    val toolCursor = when (currentTool) {
        ToolType.MOVE -> PointerIcon.Hand
        ToolType.EYEDROPPER -> PointerIcon.Crosshair
        ToolType.BUCKET_FILL -> PointerIcon.Crosshair
        else -> PointerIcon.Default
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(EditorColors.canvasBackground)
            .clipToBounds()
            .pointerHoverIcon(toolCursor)
    ) {
        var viewportSize by remember { mutableStateOf(Size.Zero) }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { coordinates ->
                    viewportSize = coordinates.size.toSize()
                }
                .pointerInput(currentTool, isPlaying) {
                    if (isPlaying) return@pointerInput

                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        
                        // Если зажато 2+ пальца — это жесты трансформации, не рисуем
                        if (awaitPointerEvent().changes.size >= 2) {
                            return@awaitEachGesture
                        }

                        val startCanvas = screenToCanvas(
                            down.position,
                            engine.panOffset.value,
                            engine.zoom.value,
                            engine.rotation.value,
                            viewportSize,
                            project.canvasWidth.toFloat(),
                            project.canvasHeight.toFloat()
                        )

                        when (currentTool) {
                            ToolType.MOVE -> {
                                var previousPosition = down.position
                                do {
                                    val event = awaitPointerEvent()
                                    val dragChange = event.changes.firstOrNull { it.id == down.id }
                                    if (dragChange != null) {
                                        val delta = dragChange.position - previousPosition
                                        engine.setPanOffset(engine.panOffset.value + delta)
                                        previousPosition = dragChange.position
                                        dragChange.consume()
                                    }
                                } while (event.changes.any { it.pressed })
                            }
                            ToolType.BUCKET_FILL -> {
                                engine.floodFillAt(startCanvas, engine.currentColor.value)
                                down.consume()
                            }
                            ToolType.EYEDROPPER -> {
                                engine.pickColorAt(startCanvas)?.let { engine.setCurrentColor(it) }
                                down.consume()
                            }
                            ToolType.BRUSH, ToolType.PENCIL, ToolType.PEN, ToolType.HARD_ERASER -> {
                                engine.startStroke(startCanvas)
                                down.consume()
                                do {
                                    val event = awaitPointerEvent()
                                    val moveChange = event.changes.firstOrNull { it.id == down.id }
                                    if (moveChange != null) {
                                        val currentPos = screenToCanvas(
                                            moveChange.position,
                                            engine.panOffset.value,
                                            engine.zoom.value,
                                            engine.rotation.value,
                                            viewportSize,
                                            project.canvasWidth.toFloat(),
                                            project.canvasHeight.toFloat()
                                        )
                                        engine.continueStroke(currentPos)
                                        moveChange.consume()
                                    }
                                } while (event.changes.any { it.pressed })
                                engine.endStroke()
                            }
                            ToolType.LINE, ToolType.RECTANGLE, ToolType.ELLIPSE -> {
                                shapePreview = ShapePreview(currentTool, startCanvas, startCanvas)
                                down.consume()
                                do {
                                    val event = awaitPointerEvent()
                                    val moveChange = event.changes.firstOrNull { it.id == down.id }
                                    if (moveChange != null) {
                                        val currentPos = screenToCanvas(
                                            moveChange.position,
                                            engine.panOffset.value,
                                            engine.zoom.value,
                                            engine.rotation.value,
                                            viewportSize,
                                            project.canvasWidth.toFloat(),
                                            project.canvasHeight.toFloat()
                                        )
                                        shapePreview = ShapePreview(currentTool, startCanvas, currentPos)
                                        moveChange.consume()
                                    }
                                } while (event.changes.any { it.pressed })
                                shapePreview?.let { engine.addShapeStroke(it.tool, it.start, it.end) }
                                shapePreview = null
                            }
                            else -> {}
                        }
                    }
                }
                .pointerInput(Unit) {
                    // Глобальные жесты трансформации (панорамирование, масштаб, поворот)
                    detectTransformGestures { centroid, pan, zoomChange, rotationChange ->
                        engine.setZoom(engine.zoom.value * zoomChange)
                        engine.setRotation(engine.rotation.value + rotationChange)
                        engine.setPanOffset(engine.panOffset.value + pan)
                    }
                }
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            if (event.type == PointerEventType.Scroll) {
                                val change = event.changes.first()
                                val factor = if (change.scrollDelta.y > 0) 0.9f else 1.1f
                                engine.setZoom(engine.zoom.value * factor)
                                change.consume()
                            }
                        }
                    }
                }
        ) {
            val cw = project.canvasWidth.toFloat()
            val ch = project.canvasHeight.toFloat()
            val cx = size.width / 2f
            val cy = size.height / 2f

            translate(left = cx + panOffset.x, top = cy + panOffset.y) {
                rotate(degrees = rotation, pivot = Offset.Zero) {
                    scale(scaleX = zoom, scaleY = zoom, pivot = Offset.Zero) {
                        translate(left = -cw/2f, top = -ch/2f) {
                            drawRect(
                                color = Color.Black.copy(alpha = 0.2f),
                                topLeft = Offset(4f/zoom, 4f/zoom),
                                size = Size(cw, ch)
                            )
                            clipRect(left = 0f, top = 0f, right = cw, bottom = ch) {
                                drawRect(color = Color.White, topLeft = Offset.Zero, size = Size(cw, ch))
                                for (frame in framesForRender) {
                                    val alpha = if (frame.isCurrent) 1f else frame.opacity
                                    for (image in frame.images) drawImageElement(image, alpha)
                                    for (stroke in frame.strokes) drawStrokeWithColor(stroke, if (frame.isCurrent) null else ghostFramesColor, alpha)
                                }
                                activeStroke?.let { drawStrokeWithColor(it) }
                                shapePreview?.let { preview -> drawShapePreview(preview, engine) }
                            }
                            drawRect(
                                color = EditorColors.canvasBorder,
                                topLeft = Offset.Zero,
                                size = Size(cw, ch),
                                style = Stroke(width = 1f/zoom)
                            )
                        }
                    }
                }
            }
        }

        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = UiDimensions.PaddingLarge.scaled())
                .shadow(12.dp.scaled(), CircleShape),
            color = EditorColors.surface.copy(alpha = 0.85f),
            shape = CircleShape,
            border = BorderStroke(1.dp.scaled(), EditorColors.divider.copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp.scaled(), vertical = 6.dp.scaled()),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp.scaled())
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ZoomButton(EditorIcons.iconRemove) { engine.setZoom(zoom * 0.8f) }
                    val zoomInteractionSource = remember { MutableInteractionSource() }
                    val zoomHovered by zoomInteractionSource.collectIsHoveredAsState()
                    Box(
                        modifier = Modifier
                            .width(54.dp.scaled())
                            .clickable(interactionSource = zoomInteractionSource, indication = null) { engine.setZoom(1f) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("${(zoom * 100).toInt()}%", color = if (zoomHovered) EditorColors.accent else EditorColors.textPrimary, style = EditorTypography.mono(), fontSize = 11.sp.scaled())
                    }
                    ZoomButton(EditorIcons.iconAdd) { engine.setZoom(zoom * 1.25f) }
                }
                Box(modifier = Modifier.width(1.dp.scaled()).height(16.dp.scaled()).background(EditorColors.divider.copy(alpha = 0.5f)))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ZoomButton(EditorIcons.iconRotateLeft) { engine.setRotation(rotation - 15f) }
                    val rotInteractionSource = remember { MutableInteractionSource() }
                    val rotHovered by rotInteractionSource.collectIsHoveredAsState()
                    Box(
                        modifier = Modifier
                            .width(48.dp.scaled())
                            .clickable(interactionSource = rotInteractionSource, indication = null) { engine.setRotation(0f); engine.setPanOffset(Offset.Zero) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("${rotation.toInt()}°", color = if (rotHovered) EditorColors.accent else EditorColors.textPrimary, style = EditorTypography.mono(), fontSize = 11.sp.scaled())
                    }
                    ZoomButton(EditorIcons.iconRotateRight) { engine.setRotation(rotation + 15f) }
                }
            }
        }
    }
}

private data class ShapePreview(val tool: ToolType, val start: Offset, val end: Offset)

@Composable
private fun ZoomButton(icon: ImageVector, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    Box(
        modifier = Modifier
            .size(28.dp.scaled())
            .clip(CircleShape)
            .background(if (isHovered) EditorColors.hover else Color.Transparent)
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(interactionSource = interactionSource, indication = null) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = if (isHovered) EditorColors.accent else EditorColors.textPrimary, modifier = Modifier.size(16.dp.scaled()))
    }
}

private fun DrawScope.drawShapePreview(preview: ShapePreview, engine: AnimationEngine) {
    val color = ulongToColor(engine.currentColor.value).copy(alpha = engine.opacity.value)
    val strokeStyle = Stroke(width = engine.brushSize.value, cap = StrokeCap.Round, join = StrokeJoin.Round)
    when (preview.tool) {
        ToolType.LINE -> drawLine(color = color, start = preview.start, end = preview.end, strokeWidth = engine.brushSize.value, cap = StrokeCap.Round)
        ToolType.RECTANGLE -> {
            val p1 = Offset(minOf(preview.start.x, preview.end.x), minOf(preview.start.y, preview.end.y))
            val p2 = Offset(maxOf(preview.start.x, preview.end.x), maxOf(preview.start.y, preview.end.y))
            drawRect(color = color, topLeft = p1, size = Size(p2.x - p1.x, p2.y - p1.y), style = strokeStyle)
        }
        ToolType.ELLIPSE -> {
            val cx = (preview.start.x + preview.end.x) / 2f
            val cy = (preview.start.y + preview.end.y) / 2f
            val rx = abs(preview.end.x - preview.start.x) / 2f
            val ry = abs(preview.end.y - preview.start.y) / 2f
            drawOval(color = color, topLeft = Offset(cx - rx, cy - ry), size = Size(rx * 2f, ry * 2f), style = strokeStyle)
        }
        else -> {}
    }
}

private fun DrawScope.drawStrokeWithColor(stroke: Stroke, overrideColor: Color? = null, alpha: Float = 1f) {
    val points = stroke.points
    if (points.isEmpty()) return
    val effectiveAlpha = alpha * stroke.opacity
    val color = if (stroke.isEraser) Color.White.copy(alpha = effectiveAlpha) else overrideColor ?: ulongToColor(stroke.color).copy(alpha = effectiveAlpha)
    if (points.size == 1) {
        drawCircle(color = color, radius = stroke.strokeWidth / 2f, center = points[0])
        return
    }
    val path = Path().apply {
        moveTo(points[0].x, points[0].y)
        for (i in 1 until points.size) lineTo(points[i].x, points[i].y)
    }
    drawPath(path = path, color = color, style = Stroke(width = stroke.strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

private fun DrawScope.drawImageElement(image: ImageElement, alpha: Float) {
    val bitmap = imageCache.getOrPut(image.id) { decodeImage(image.data) ?: return }
    withTransform({
        translate(image.x, image.y)
        rotate(image.rotation, Offset(bitmap.width / 2f, bitmap.height / 2f))
        scale(image.scale, image.scale, Offset(bitmap.width / 2f, bitmap.height / 2f))
    }) {
        drawImage(bitmap, alpha = alpha)
    }
}

private fun screenToCanvas(screenPos: Offset, panOffset: Offset, scale: Float, rotationDeg: Float, viewportSize: Size, canvasWidth: Float, canvasHeight: Float): Offset {
    val cx = viewportSize.width / 2f
    val cy = viewportSize.height / 2f
    val x = screenPos.x - (cx + panOffset.x)
    val y = screenPos.y - (cy + panOffset.y)
    val rad = -rotationDeg * PI.toFloat() / 180f
    val cosR = cos(rad)
    val sinR = sin(rad)
    val rx = x * cosR - y * sinR
    val ry = x * sinR + y * cosR
    return Offset(rx / scale + canvasWidth / 2f, ry / scale + canvasHeight / 2f)
}

private fun ulongToColor(color: ULong): Color {
    val a = ((color shr 24) and 0xFFuL).toInt() / 255f
    val r = ((color shr 16) and 0xFFuL).toInt() / 255f
    val g = ((color shr 8) and 0xFFuL).toInt() / 255f
    val b = (color and 0xFFuL).toInt() / 255f
    return Color(r, g, b, a)
}