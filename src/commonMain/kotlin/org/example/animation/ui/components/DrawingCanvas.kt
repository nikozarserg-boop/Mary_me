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
import org.example.animation.engine.Renderer
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
    val ghostFramesColor = remember(ghostFramesColorU) { Renderer.ulongToColor(ghostFramesColorU).copy(alpha = 0.4f) }

    val activeStroke by engine.activeStroke.collectAsState()
    val stamps = remember(engine.brushes.collectAsState().value) { engine.getStampsMap() }

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

                    // Состояние для текущего жеста
                    var gestureActive = false
                    var isTransformGesture = false
                    var previousPan = Offset.Zero
                    var previousZoom = 1f
                    var previousRotation = 0f
                    var previousPinchDistance = 0f
                    var previousPinchAngle = 0f
                    var activePointerId: PointerId? = null
                    var strokeStarted = false
                    var startCanvasPos = Offset.Zero

                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Main)

                            // Обработка скролла (колёсико мыши)
                            if (event.type == PointerEventType.Scroll) {
                                val change = event.changes.firstOrNull() ?: continue
                                val factor = if (change.scrollDelta.y > 0) 0.9f else 1.1f
                                engine.setZoom(engine.zoom.value * factor)
                                change.consume()
                                continue
                            }

                            if (event.type != PointerEventType.Move &&
                                event.type != PointerEventType.Press &&
                                event.type != PointerEventType.Release) continue

                            val activeChanges = event.changes.filter { it.pressed }
                            val pointerCount = activeChanges.size

                            // Начало взаимодействия
                            if (pointerCount == 1 && !gestureActive) {
                                gestureActive = true
                                isTransformGesture = false
                                strokeStarted = false
                                val change = activeChanges.first()
                                activePointerId = change.id

                                startCanvasPos = screenToCanvas(
                                    change.position,
                                    engine.panOffset.value,
                                    engine.zoom.value,
                                    engine.rotation.value,
                                    viewportSize,
                                    project.canvasWidth.toFloat(),
                                    project.canvasHeight.toFloat()
                                )
                                previousPan = change.position

                                when (currentTool) {
                                    ToolType.MOVE -> {
                                        engine.setPanOffset(engine.panOffset.value)
                                        change.consume()
                                    }
                                    ToolType.BUCKET_FILL -> {
                                        engine.floodFillAt(startCanvasPos, engine.currentColor.value)
                                        change.consume()
                                    }
                                    ToolType.EYEDROPPER -> {
                                        engine.pickColorAt(startCanvasPos)?.let { engine.setCurrentColor(it) }
                                        change.consume()
                                    }
                                    ToolType.BRUSH, ToolType.PENCIL, ToolType.PEN, ToolType.HARD_ERASER -> {
                                        engine.startStroke(startCanvasPos)
                                        strokeStarted = true
                                        change.consume()
                                    }
                                    ToolType.LINE, ToolType.RECTANGLE, ToolType.ELLIPSE -> {
                                        shapePreview = ShapePreview(currentTool, startCanvasPos, startCanvasPos)
                                        change.consume()
                                    }
                                    else -> {}
                                }
                                continue
                            }

                            // Переход с 1 пальца на 2+ — отменяем рисование, начинаем трансформацию
                            if (pointerCount >= 2 && gestureActive && !isTransformGesture) {
                                isTransformGesture = true
                                previousZoom = engine.zoom.value
                                previousRotation = engine.rotation.value

                                // Отменяем текущий штрих, если он был
                                if (strokeStarted) {
                                    engine.endStroke()
                                    strokeStarted = false
                                }
                                shapePreview = null

                                val positions = activeChanges.map { it.position }
                                val centroid = Offset(
                                    positions.sumOf { it.x.toDouble() }.toFloat() / positions.size,
                                    positions.sumOf { it.y.toDouble() }.toFloat() / positions.size
                                )
                                previousPan = centroid

                                if (positions.size >= 2) {
                                    previousPinchDistance = (positions[1] - positions[0]).getDistance()
                                    previousPinchAngle = atan2(
                                        (positions[1].y - positions[0].y).toDouble(),
                                        (positions[1].x - positions[0].x).toDouble()
                                    ).toFloat()
                                }

                                // Потребляем все изменения
                                activeChanges.forEach { it.consume() }
                                continue
                            }

                            // Трансформация двумя пальцами
                            if (isTransformGesture && pointerCount >= 2) {
                                val positions = activeChanges.map { it.position }
                                val centroid = Offset(
                                    positions.sumOf { it.x.toDouble() }.toFloat() / positions.size,
                                    positions.sumOf { it.y.toDouble() }.toFloat() / positions.size
                                )

                                // Масштаб — по расстоянию между пальцами
                                if (positions.size >= 2 && previousPinchDistance > 0f) {
                                    val dist = (positions[1] - positions[0]).getDistance()
                                    val zoomChange = dist / previousPinchDistance
                                    engine.setZoom(previousZoom * zoomChange)
                                }

                                // Поворот — по углу между пальцами
                                if (positions.size >= 2) {
                                    val angle = atan2(
                                        (positions[1].y - positions[0].y).toDouble(),
                                        (positions[1].x - positions[0].x).toDouble()
                                    ).toFloat()
                                    val deltaDeg = Math.toDegrees((angle - previousPinchAngle).toDouble()).toFloat()
                                    engine.setRotation(previousRotation + deltaDeg)
                                }

                                // Панорамирование
                                val panDelta = centroid - previousPan
                                engine.setPanOffset(engine.panOffset.value + panDelta)
                                previousPan = centroid

                                activeChanges.forEach { it.consume() }
                                continue
                            }

                            // Рисование/перемещение одним пальцем
                            if (!isTransformGesture && gestureActive && pointerCount == 1) {
                                val change = activeChanges.firstOrNull { it.id == activePointerId }
                                if (change != null) {
                                    when (currentTool) {
                                        ToolType.MOVE -> {
                                            val delta = change.position - previousPan
                                            engine.setPanOffset(engine.panOffset.value + delta)
                                            previousPan = change.position
                                            change.consume()
                                        }
                                        ToolType.BRUSH, ToolType.PENCIL, ToolType.PEN, ToolType.HARD_ERASER -> {
                                            if (strokeStarted) {
                                                val currentPos = screenToCanvas(
                                                    change.position,
                                                    engine.panOffset.value,
                                                    engine.zoom.value,
                                                    engine.rotation.value,
                                                    viewportSize,
                                                    project.canvasWidth.toFloat(),
                                                    project.canvasHeight.toFloat()
                                                )
                                                engine.continueStroke(currentPos)
                                                change.consume()
                                            }
                                        }
                                        ToolType.LINE, ToolType.RECTANGLE, ToolType.ELLIPSE -> {
                                            val currentPos = screenToCanvas(
                                                change.position,
                                                engine.panOffset.value,
                                                engine.zoom.value,
                                                engine.rotation.value,
                                                viewportSize,
                                                project.canvasWidth.toFloat(),
                                                project.canvasHeight.toFloat()
                                            )
                                            shapePreview = ShapePreview(currentTool, startCanvasPos, currentPos)
                                            change.consume()
                                        }
                                        else -> {}
                                    }
                                }
                                continue
                            }

                            // Завершение взаимодействия
                            if (pointerCount == 0 && gestureActive) {
                                if (strokeStarted) {
                                    engine.endStroke()
                                    strokeStarted = false
                                }
                                if (!isTransformGesture) {
                                    shapePreview?.let {
                                        if (currentTool in listOf(ToolType.LINE, ToolType.RECTANGLE, ToolType.ELLIPSE)) {
                                            engine.addShapeStroke(it.tool, it.start, it.end)
                                        }
                                    }
                                }
                                shapePreview = null
                                gestureActive = false
                                isTransformGesture = false
                                activePointerId = null
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
                                    for (stroke in frame.strokes) Renderer.drawStroke(this, stroke, if (frame.isCurrent) null else ghostFramesColor, alpha, stamps)
                                }
                                activeStroke?.let { Renderer.drawStroke(this, it, null, 1f, stamps) }
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
    val color = Renderer.ulongToColor(engine.currentColor.value).copy(alpha = engine.opacity.value)
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
