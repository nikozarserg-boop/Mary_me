package org.example.animation.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import org.example.animation.engine.AnimationEngine
import org.example.animation.model.Stroke
import org.example.animation.model.ToolType
import org.example.animation.ui.theme.EditorColors

@Composable
fun DrawingCanvas(engine: AnimationEngine, modifier: Modifier = Modifier) {
    val project by engine.project.collectAsState()
    val zoom by engine.zoom.collectAsState()
    val panOffset by engine.panOffset.collectAsState()
    val currentTool by engine.currentTool.collectAsState()
    val isPlaying by engine.isPlaying.collectAsState()

    val framesForRender = engine.getFramesForRendering()

    // Выбор курсора в зависимости от инструмента
    val toolCursor = when (currentTool) {
        ToolType.MOVE -> PointerIcon.Hand
        ToolType.EYEDROPPER -> PointerIcon.Crosshair
        ToolType.SELECT -> PointerIcon.Text // Или какой-то другой
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
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            if (event.type == PointerEventType.Scroll) {
                                val change = event.changes.first()
                                val zoomFactor = if (change.scrollDelta.y > 0) 0.9f else 1.1f
                                engine.setZoom((zoom * zoomFactor).coerceIn(0.1f, 10f))
                                change.consume()
                            }
                        }
                    }
                }
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoomDelta, _ ->
                        engine.setZoom((zoom * zoomDelta).coerceIn(0.1f, 10f))
                        engine.setPanOffset(panOffset + pan)
                    }
                }
                .pointerInput(currentTool, isPlaying, zoom, panOffset, viewportSize) {
                    if (isPlaying) return@pointerInput
                    
                    if (currentTool == ToolType.MOVE) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            engine.setPanOffset(panOffset + dragAmount)
                        }
                    } else {
                        detectDragGestures(
                            onDragStart = { offset ->
                                val canvasPos = screenToCanvas(offset, panOffset, zoom, viewportSize)
                                engine.startStroke(canvasPos)
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                val canvasPos = screenToCanvas(change.position, panOffset, zoom, viewportSize)
                                engine.continueStroke(canvasPos)
                            },
                            onDragEnd = { engine.endStroke() }
                        )
                    }
                }
        ) {
            val cw = project.canvasWidth.toFloat()
            val ch = project.canvasHeight.toFloat()
            
            val cx = size.width / 2f
            val cy = size.height / 2f

            drawBackgroundCheckerboard(size, zoom, panOffset)

            translate(left = cx + panOffset.x, top = cy + panOffset.y) {
                scale(scaleX = zoom, scaleY = zoom, pivot = Offset.Zero) {
                    translate(left = -cw/2f, top = -ch/2f) {
                        drawRect(color = Color.White, topLeft = Offset.Zero, size = Size(cw, ch))
                        drawRect(
                            color = EditorColors.canvasBorder, 
                            topLeft = Offset.Zero, 
                            size = Size(cw, ch), 
                            style = Stroke(width = 1f/zoom)
                        )
                        
                        for (frame in framesForRender) {
                            val alpha = if (frame.isCurrent) 1f else frame.opacity
                            for (stroke in frame.strokes) {
                                drawStrokeWithColor(stroke, if (frame.isCurrent) null else EditorColors.onionSkinColor, alpha)
                            }
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(EditorColors.darkSurface.copy(alpha = 0.9f))
                .border(1.dp, EditorColors.dividerColor, RoundedCornerShape(8.dp)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ZoomButton("−") { engine.setZoom((zoom * 0.8f).coerceIn(0.1f, 10f)) }
            Box(
                modifier = Modifier
                    .width(54.dp)
                    .pointerHoverIcon(PointerIcon.Hand)
                    .clickable { 
                        engine.setZoom(1f) 
                        engine.setPanOffset(Offset.Zero)
                    }, 
                contentAlignment = Alignment.Center
            ) {
                Text("${(zoom * 100).toInt()}%", color = EditorColors.textPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            ZoomButton("+") { engine.setZoom((zoom * 1.25f).coerceIn(0.1f, 10f)) }
        }
    }
}

@Composable
private fun ZoomButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = EditorColors.textPrimary, fontSize = 18.sp)
    }
}

private fun DrawScope.drawStrokeWithColor(stroke: Stroke, overrideColor: Color? = null, alpha: Float = 1f) {
    val points = stroke.points
    if (points.size < 2) return

    val effectiveAlpha = alpha * stroke.opacity
    val color = overrideColor ?: ulongToColor(stroke.color).copy(alpha = effectiveAlpha)

    val path = Path()
    path.moveTo(points[0].x, points[0].y)
    for (i in 1 until points.size) path.lineTo(points[i].x, points[i].y)

    drawPath(
        path = path, 
        color = color, 
        style = Stroke(
            width = stroke.strokeWidth, 
            cap = StrokeCap.Round, 
            join = StrokeJoin.Round
        )
    )
}

private fun DrawScope.drawBackgroundCheckerboard(viewSize: Size, zoom: Float, pan: Offset) {
    val cellSize = 16f
    for (y in 0 until (viewSize.height / cellSize).toInt() + 1) {
        for (x in 0 until (viewSize.width / cellSize).toInt() + 1) {
            if ((x + y) % 2 == 0) {
                drawRect(
                    color = EditorColors.checkerLight.copy(alpha = 0.2f),
                    topLeft = Offset(x * cellSize, y * cellSize),
                    size = Size(cellSize, cellSize)
                )
            }
        }
    }
}

private fun screenToCanvas(screenPos: Offset, panOffset: Offset, scale: Float, viewportSize: Size): Offset {
    val cx = viewportSize.width / 2f
    val cy = viewportSize.height / 2f
    return Offset(
        (screenPos.x - cx - panOffset.x) / scale,
        (screenPos.y - cy - panOffset.y) / scale
    )
}

private fun ulongToColor(color: ULong): Color {
    val a = ((color shr 24) and 0xFFuL).toInt() / 255f
    val r = ((color shr 16) and 0xFFuL).toInt() / 255f
    val g = ((color shr 8) and 0xFFuL).toInt() / 255f
    val b = (color and 0xFFuL).toInt() / 255f
    return Color(r, g, b, a)
}
