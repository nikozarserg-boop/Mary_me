package org.example.animation.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import org.example.animation.engine.AnimationEngine
import org.example.animation.model.Stroke
import org.example.animation.model.ToolType
import org.example.animation.ui.theme.EditorColors

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun DrawingCanvas(engine: AnimationEngine, modifier: Modifier = Modifier) {
    val project by engine.project.collectAsState()
    val zoom by engine.zoom.collectAsState()
    val panOffset by engine.panOffset.collectAsState()
    val currentTool by engine.currentTool.collectAsState()
    val isPlaying by engine.isPlaying.collectAsState()

    val framesForRender = engine.getFramesForRendering()

    Box(modifier = modifier.fillMaxSize().background(EditorColors.canvasBackground), contentAlignment = Alignment.Center) {
        var canvasOffset by remember { mutableStateOf(Offset.Zero) }
        var canvasScale by remember { mutableStateOf(1f) }

        LaunchedEffect(zoom, panOffset) {
            canvasScale = zoom
            canvasOffset = panOffset
        }

        Canvas(
            modifier = Modifier.fillMaxSize()
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            val dragEvent = event.changes.firstOrNull()
                            if (event.type == PointerEventType.Scroll && dragEvent != null) {
                                val delta = dragEvent.scrollDelta.y
                                val newScale = (canvasScale * if (delta > 0) 0.9f else 1.1f).coerceIn(0.1f, 10f)
                                canvasScale = newScale
                                engine.setZoom(newScale)
                            }
                        }
                    }
                }
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoomDelta, _ ->
                        canvasScale = (canvasScale * zoomDelta).coerceIn(0.1f, 10f)
                        canvasOffset += pan
                        engine.setZoom(canvasScale)
                        engine.setPanOffset(canvasOffset)
                    }
                }
                .pointerInput(currentTool, isPlaying) {
                    if (isPlaying) return@pointerInput
                    if (currentTool == ToolType.MOVE) {
                        detectDragGestures { change, _ ->
                            change.consume()
                            canvasOffset += change.position - change.previousPosition
                            engine.setPanOffset(canvasOffset)
                        }
                    } else {
                        detectDragGestures(
                            onDragStart = { offset ->
                                val canvasSize = Size(size.width.toFloat(), size.height.toFloat())
                                val canvasPos = screenToCanvas(offset, canvasOffset, canvasScale, canvasSize)
                                engine.startStroke(canvasPos)
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                val canvasSize = Size(size.width.toFloat(), size.height.toFloat())
                                val canvasPos = screenToCanvas(change.position, canvasOffset, canvasScale, canvasSize)
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

            translate(left = cx + canvasOffset.x, top = cy + canvasOffset.y) {
                scale(scaleX = canvasScale, scaleY = canvasScale, pivot = Offset.Zero) {
                    drawCheckerboard(cw, ch)
                    drawRect(color = EditorColors.canvasBorder, topLeft = Offset.Zero, size = Size(cw, ch), style = Stroke(width = 2f))
                    val bgColor = ulongToColor(project.backgroundColor)
                    drawRect(color = bgColor, topLeft = Offset.Zero, size = Size(cw, ch))
                    for (frame in framesForRender) {
                        val alpha = if (frame.isCurrent) 1f else frame.opacity
                        for (stroke in frame.strokes) {
                            if (!frame.isCurrent) drawStrokeWithColor(stroke, EditorColors.onionSkinColor, alpha)
                            else drawStrokeWithColor(stroke, null, alpha)
                        }
                    }
                }
            }
        }

        // Кнопки зума
        Row(modifier = Modifier.align(Alignment.BottomStart).padding(8.dp).clip(RoundedCornerShape(6.dp)).background(EditorColors.darkSurface.copy(alpha = 0.85f))) {
            Box(modifier = Modifier.size(28.dp).clickable { engine.setZoom((zoom * 0.8f).coerceIn(0.1f, 10f)) }, contentAlignment = Alignment.Center) {
                Text("−", color = EditorColors.textPrimary, fontSize = 16.sp)
            }
            Box(modifier = Modifier.size(44.dp, 28.dp).clickable { engine.setZoom(1f) }, contentAlignment = Alignment.Center) {
                Text("${(zoom * 100).toInt()}%", color = EditorColors.textSecondary, fontSize = 11.sp)
            }
            Box(modifier = Modifier.size(28.dp).clickable { engine.setZoom((zoom * 1.25f).coerceIn(0.1f, 10f)) }, contentAlignment = Alignment.Center) {
                Text("+", color = EditorColors.textPrimary, fontSize = 16.sp)
            }
        }

        Text(
            text = "${project.canvasWidth} × ${project.canvasHeight}",
            color = EditorColors.textSecondary,
            modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp).clip(RoundedCornerShape(4.dp)).background(EditorColors.darkSurface.copy(alpha = 0.85f)).padding(horizontal = 6.dp, vertical = 2.dp)
        )
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

    if (stroke.isEraser) {
        drawPath(
            path = path,
            color = Color.White.copy(alpha = effectiveAlpha),
            style = Stroke(width = stroke.strokeWidth * 3, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    } else {
        drawPath(path = path, color = color, style = Stroke(width = stroke.strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
        if (stroke.strokeWidth > 8) {
            drawPath(
                path = path,
                color = color.copy(alpha = 0.2f * effectiveAlpha),
                style = Stroke(width = stroke.strokeWidth * 2f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }
    }
}

private fun DrawScope.drawCheckerboard(width: Float, height: Float) {
    val cellSize = 16f
    var y = 0f
    while (y < height) {
        var x = 0f
        while (x < width) {
            val isLight = ((x / cellSize).toInt() + (y / cellSize).toInt()) % 2 == 0
            drawRect(color = if (isLight) EditorColors.checkerLight else EditorColors.checkerDark, topLeft = Offset(x, y), size = Size(cellSize, cellSize))
            x += cellSize
        }
        y += cellSize
    }
}

private fun screenToCanvas(screenPos: Offset, canvasOffset: Offset, scale: Float, canvasSize: Size): Offset {
    val cx = canvasSize.width / 2f
    val cy = canvasSize.height / 2f
    val dx = (screenPos.x - cx - canvasOffset.x) / scale
    val dy = (screenPos.y - cy - canvasOffset.y) / scale
    return Offset(dx, dy)
}

private fun ulongToColor(color: ULong): Color {
    val a = ((color shr 24) and 0xFFuL).toInt()
    val r = ((color shr 16) and 0xFFuL).toInt()
    val g = ((color shr 8) and 0xFFuL).toInt()
    val b = (color and 0xFFuL).toInt()
    return Color(r, g, b, a)
}