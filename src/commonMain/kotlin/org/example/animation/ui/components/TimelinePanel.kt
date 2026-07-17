package org.example.animation.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import org.example.animation.engine.AnimationEngine
import org.example.animation.localization.EditorStrings
import org.example.animation.ui.components.tooltip.tooltipAnchor
import org.example.animation.ui.theme.*
import androidx.compose.ui.input.key.*
import kotlin.math.roundToInt

@Composable
fun TimelinePanel(engine: AnimationEngine, modifier: Modifier = Modifier) {
    val focusRequester = remember { FocusRequester() }
    
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
    
    val project by engine.project.collectAsState()
    val currentFrameIndex by engine.currentFrameIndex.collectAsState()
    val currentLayerIndex by engine.currentLayerIndex.collectAsState()
    val isPlaying by engine.isPlaying.collectAsState()

    // Состояние drag&drop для кадров
    var dragState by remember { mutableStateOf<FrameDragState?>(null) }

    Surface(modifier = modifier.fillMaxWidth().onKeyEvent { event ->
        if (event.type == KeyEventType.KeyUp &&
            (event.key == Key.DirectionLeft || event.key == Key.DirectionRight)) {
            when (event.key) {
                Key.DirectionLeft -> engine.goToPreviousFrame()
                Key.DirectionRight -> engine.goToNextFrame()
                else -> {}
            }
            true
        } else false
    }.focusRequester(focusRequester).focusable(), color = EditorColors.timelineBackground, elevation = 4.dp.scaled()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(EditorColors.panelHeader)
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp.scaled(), vertical = 4.dp.scaled()),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(EditorIcons.iconTimeline, null, tint = EditorColors.textSecondary, modifier = Modifier.size(14.dp.scaled()))
                Spacer(Modifier.width(6.dp.scaled()))
                Text(
                    EditorStrings.observeString("panel.timeline").uppercase(),
                    style = EditorTypography.panelTitle()
                )
                Spacer(Modifier.width(16.dp.scaled()))
                Text(
                    "${EditorStrings.observeString("status.frame")}: ${currentFrameIndex + 1}/${project.maxFrames}",
                    style = EditorTypography.body(),
                    color = EditorColors.textPrimary
                )
                Spacer(Modifier.width(12.dp.scaled()))
                Text(
                    "${EditorStrings.observeString("status.fps")}: ${project.fps}",
                    style = EditorTypography.caption()
                )
                Spacer(Modifier.weight(1f))
            }

            Divider(color = EditorColors.divider, thickness = 1.dp.scaled())

            Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .width(100.dp.scaled())
                        .background(EditorColors.panelBackground)
                        .verticalScroll(rememberScrollState())
                ) {
                    project.layers.forEachIndexed { index, layer ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(32.dp.scaled())
                                .background(if (index == currentLayerIndex) EditorColors.selection.copy(alpha = 0.3f) else Color.Transparent)
                                .pointerHoverIcon(PointerIcon.Hand)
                                .clickable { engine.setCurrentLayer(index) }
                                .padding(horizontal = 8.dp.scaled()),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                layer.name,
                                style = EditorTypography.layerName(),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Область сетки кадров
                val density = LocalDensity.current
                val cellWidth = 28.dp.scaled()
                val cellHeight = 32.dp.scaled()
                val cellWidthPx = with(density) { cellWidth.toPx() }
                val cellHeightPx = with(density) { cellHeight.toPx() }
                val horizontalScrollState = rememberScrollState()
                val verticalScrollState = rememberScrollState()

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(horizontalScrollState)
                ) {
                    Column(modifier = Modifier.verticalScroll(verticalScrollState)) {
                        project.layers.forEachIndexed { layerIndex, _ ->
                            Row(modifier = Modifier.height(cellHeight).zIndex(if (dragState?.fromLayer == layerIndex) 10f else 1f)) {
                                for (frameIndex in 0 until maxOf(project.maxFrames, 1)) {
                                    val hasContent = layerIndex < project.layers.size &&
                                            frameIndex < project.layers[layerIndex].frames.size &&
                                            project.layers[layerIndex].frames[frameIndex].strokes.isNotEmpty()
                                    val isCurrent = frameIndex == currentFrameIndex && layerIndex == currentLayerIndex
                                    val isDragSource =
                                        dragState?.fromLayer == layerIndex && dragState?.fromFrame == frameIndex
                                    val isDropTarget = dragState?.hoverLayer == layerIndex &&
                                            dragState?.hoverFrame == frameIndex && !isDragSource

                                    val bgColor = when {
                                        isCurrent -> EditorColors.timelineCellActive
                                        hasContent -> EditorColors.timelineCellHasContent.copy(alpha = 0.3f)
                                        else -> EditorColors.timelineCell
                                    }

                                    Box(
                                        modifier = Modifier
                                            .zIndex(if (isDragSource) 100f else 1f)
                                            .width(cellWidth)
                                            .fillMaxHeight()
                                            // Сначала применяем смещение, если это перетаскиваемый объект
                                            .then(
                                                if (isDragSource) {
                                                    Modifier
                                                        .offset {
                                                            IntOffset(
                                                                dragState?.offsetX?.roundToInt() ?: 0,
                                                                dragState?.offsetY?.roundToInt() ?: 0
                                                            )
                                                        }
                                                        .shadow(12.dp.scaled(), RoundedCornerShape(4.dp.scaled()))
                                                        .graphicsLayer {
                                                            scaleX = 1.1f
                                                            scaleY = 1.1f
                                                            alpha = 0.9f
                                                        }
                                                } else Modifier
                                            )
                                            .border(
                                                width = if (isCurrent || isDropTarget) 1.5.dp.scaled() else 0.5.dp.scaled(),
                                                color = when {
                                                    isDropTarget -> EditorColors.accent
                                                    isCurrent -> EditorColors.accent
                                                    else -> EditorColors.divider
                                                }
                                            )
                                            .background(
                                                if (isDragSource) EditorColors.accent.copy(alpha = 0.8f)
                                                else if (isDropTarget) EditorColors.accent.copy(alpha = 0.4f)
                                                else bgColor
                                            )
                                            .pointerHoverIcon(PointerIcon.Hand)
                                            .clickable {
                                                engine.setCurrentFrame(frameIndex)
                                                engine.setCurrentLayer(layerIndex)
                                            }
                                            .pointerInput(layerIndex, frameIndex) {
                                                detectDragGestures(
                                                    onDragStart = {
                                                        dragState = FrameDragState(
                                                            fromLayer = layerIndex,
                                                            fromFrame = frameIndex,
                                                            hoverLayer = layerIndex,
                                                            hoverFrame = frameIndex
                                                        )
                                                    },
                                                    onDrag = { change, dragAmount ->
                                                        change.consume()
                                                        dragState?.let { st ->
                                                            val totalOffsetX = st.offsetX + dragAmount.x
                                                            val totalOffsetY = st.offsetY + dragAmount.y
                                                            
                                                            // Вычисляем к какой ячейке мы ближе всего относительно старта
                                                            val frameDiff = (totalOffsetX / cellWidthPx).roundToInt()
                                                            val layerDiff = (totalOffsetY / cellHeightPx).roundToInt()
                                                            
                                                            val newHoverLayer = (st.fromLayer + layerDiff)
                                                                .coerceIn(0, project.layers.lastIndex)
                                                            val newHoverFrame = (st.fromFrame + frameDiff)
                                                                .coerceIn(0, project.maxFrames - 1)
                                                                
                                                            dragState = st.copy(
                                                                offsetX = totalOffsetX,
                                                                offsetY = totalOffsetY,
                                                                hoverLayer = newHoverLayer,
                                                                hoverFrame = newHoverFrame
                                                            )
                                                        }
                                                    },
                                                    onDragEnd = {
                                                        dragState?.let { st ->
                                                            if (st.hoverLayer != st.fromLayer || st.hoverFrame != st.fromFrame) {
                                                                engine.moveFrameToLayer(
                                                                    fromLayer = st.fromLayer,
                                                                    fromFrame = st.fromFrame,
                                                                    toLayer = st.hoverLayer,
                                                                    toFrame = st.hoverFrame
                                                                )
                                                            } else {
                                                                engine.setCurrentFrame(st.fromFrame)
                                                                engine.setCurrentLayer(st.fromLayer)
                                                            }
                                                        }
                                                        dragState = null
                                                    },
                                                    onDragCancel = { dragState = null }
                                                )
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (hasContent) {
                                            Box(modifier = Modifier
                                                .size(8.dp.scaled())
                                                .background(
                                                    if (isDragSource) Color.White else EditorColors.accent, 
                                                    RoundedCornerShape(2.dp.scaled())
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Вертикальный скроллбар
                val totalContentHeight = (project.layers.size * cellHeightPx)
                val viewportHeight = with(density) { (cellHeight * 15).toPx() }
                if (totalContentHeight > viewportHeight) {
                    Box(
                        modifier = Modifier
                            .width(10.dp.scaled())
                            .fillMaxHeight()
                            .background(EditorColors.panelBackground)
                            .padding(vertical = 2.dp.scaled(), horizontal = 1.dp.scaled())
                    ) {
                        val thumbHeight = (viewportHeight / totalContentHeight * viewportHeight).coerceAtLeast(with(density) { 20.dp.toPx() })
                        val thumbY = if (verticalScrollState.maxValue > 0) {
                            (verticalScrollState.value / verticalScrollState.maxValue.toFloat() * (viewportHeight - thumbHeight))
                        } else 0f
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(with(density) { thumbHeight.toDp() })
                                .offset(y = with(density) { thumbY.toDp() })
                                .background(
                                    EditorColors.textSecondary.copy(alpha = 0.4f),
                                    RoundedCornerShape(5.dp.scaled())
                                )
                        )
                    }
                }

                // Горизонтальный скроллбар
                val totalContentWidth = (maxOf(project.maxFrames, 1) * cellWidthPx)
                val viewportWidth = with(density) { (cellWidth * 20).toPx() }
                if (totalContentWidth > viewportWidth) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp.scaled())
                            .background(EditorColors.panelBackground)
                            .padding(horizontal = 2.dp.scaled(), vertical = 1.dp.scaled())
                    ) {
                        val thumbWidth = (viewportWidth / totalContentWidth * viewportWidth).coerceAtLeast(with(density) { 20.dp.toPx() })
                        val thumbX = if (horizontalScrollState.maxValue > 0) {
                            (horizontalScrollState.value / horizontalScrollState.maxValue.toFloat() * (viewportWidth - thumbWidth))
                        } else 0f
                        Box(
                            modifier = Modifier
                                .width(with(density) { thumbWidth.toDp() })
                                .fillMaxHeight()
                                .offset(x = with(density) { thumbX.toDp() })
                                .background(
                                    EditorColors.textSecondary.copy(alpha = 0.4f),
                                    RoundedCornerShape(5.dp.scaled())
                                )
                        )
                    }
                }
            }

            Divider(color = EditorColors.divider, thickness = 1.dp.scaled())

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(EditorColors.panelBackground)
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp.scaled(), vertical = 6.dp.scaled()),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TControlBtn(EditorIcons.iconFirstPage, tooltip = EditorStrings.observeString("frame.first")) { engine.goToFirstFrame() }
                Spacer(Modifier.width(2.dp.scaled()))
                TControlBtn(EditorIcons.iconSkipPrevious, tooltip = EditorStrings.observeString("frame.prev")) { engine.goToPreviousFrame() }
                Spacer(Modifier.width(8.dp.scaled()))

                PlayPauseBtn(isPlaying) { engine.togglePlayback() }

                Spacer(Modifier.width(8.dp.scaled()))
                TControlBtn(EditorIcons.iconSkipNext, tooltip = EditorStrings.observeString("frame.next")) { engine.goToNextFrame() }
                Spacer(Modifier.width(2.dp.scaled()))
                TControlBtn(EditorIcons.iconLastPage, tooltip = EditorStrings.observeString("frame.last")) { engine.goToLastFrame() }

                Spacer(Modifier.width(24.dp.scaled()))

                val totalMs = project.maxFrames * 1000 / project.fps.coerceAtLeast(1)
                val sec = totalMs / 1000
                Text(
                    "${currentFrameIndex + 1} | ${sec / 60}:${(sec % 60).toString().padStart(2, '0')}",
                    style = EditorTypography.mono(),
                    modifier = Modifier.padding(horizontal = 8.dp.scaled())
                )

                Spacer(Modifier.weight(1f))

                Text(EditorStrings.observeString("status.fps"), style = EditorTypography.caption())
                Spacer(Modifier.width(8.dp.scaled()))

                var fpsText by remember { mutableStateOf(project.fps.toString()) }
                var fpsFocused by remember { mutableStateOf(false) }
                var wasFocused by remember { mutableStateOf(false) }

                LaunchedEffect(project.fps) {
                    if (!fpsFocused) {
                        fpsText = project.fps.toString()
                    }
                }

                OutlinedTextField(
                    value = fpsText,
                    onValueChange = {
                        if (it.isEmpty() || it.all { c -> c.isDigit() }) {
                            fpsText = it.take(3)
                        }
                    },
                    singleLine = true,
                    textStyle = EditorTypography.mono(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        val v = fpsText.toIntOrNull()
                        if (v != null && v in 1..120) {
                            engine.setFps(v)
                            fpsText = v.toString()
                        } else {
                            fpsText = project.fps.toString()
                        }
                    }),
                    modifier = Modifier
                        .width(70.dp.scaled())
                        .height(32.dp.scaled())
                        .onFocusChanged { state ->
                            fpsFocused = state.isFocused
                            if (!state.isFocused && wasFocused) {
                                val v = fpsText.toIntOrNull()
                                if (v != null && v in 1..120) {
                                    engine.setFps(v)
                                    fpsText = v.toString()
                                } else {
                                    fpsText = project.fps.toString()
                                }
                            }
                            wasFocused = state.isFocused
                        },
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = EditorColors.accent,
                        unfocusedBorderColor = EditorColors.divider,
                        backgroundColor = EditorColors.surfaceVariant.copy(alpha = 0.3f)
                    )
                )

                Spacer(Modifier.width(16.dp.scaled()))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    TimelineFrameActionBtn(
                        icon = EditorIcons.iconContentCopy,
                        tooltip = EditorStrings.observeString("frame.copy"),
                        iconTint = EditorColors.accent
                    ) { engine.copyFrame() }
                    Spacer(Modifier.width(4.dp.scaled()))
                    TimelineFrameActionBtn(
                        icon = EditorIcons.iconContentPaste,
                        tooltip = EditorStrings.observeString("frame.paste"),
                        iconTint = EditorColors.accent
                    ) { engine.pasteFrame() }
                    Spacer(Modifier.width(4.dp.scaled()))
                    TimelineFrameActionBtn(
                        icon = EditorIcons.iconAdd,
                        tooltip = EditorStrings.observeString("frame.add"),
                        iconTint = EditorColors.accent
                    ) { engine.addFrame() }
                    Spacer(Modifier.width(4.dp.scaled()))
                    TimelineFrameActionBtn(
                        icon = EditorIcons.iconContentCopy,
                        tooltip = EditorStrings.observeString("frame.duplicate"),
                        iconTint = EditorColors.accent
                    ) { engine.duplicateFrame() }
                    Spacer(Modifier.width(4.dp.scaled()))
                    TimelineFrameActionBtn(
                        icon = EditorIcons.iconDelete,
                        tooltip = EditorStrings.observeString("frame.delete"),
                        iconTint = EditorColors.accentRed
                    ) { engine.removeFrame() }
                    Spacer(Modifier.width(4.dp.scaled()))
                    TimelineFrameActionBtn(
                        icon = EditorIcons.iconClear,
                        tooltip = EditorStrings.observeString("frame.clear"),
                        iconTint = EditorColors.accentOrange
                    ) { engine.clearFrame() }
                }
            }
        }
    }
}

private data class FrameDragState(
    val fromLayer: Int,
    val fromFrame: Int,
    val hoverLayer: Int,
    val hoverFrame: Int,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f
)

@Composable
private fun TControlBtn(icon: ImageVector, tooltip: String = "", onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isPressed by interactionSource.collectIsPressedAsState()

    val backgroundColor = when {
        isPressed -> EditorColors.accent.copy(alpha = 0.3f)
        isHovered -> EditorColors.hover
        else -> EditorColors.surfaceVariant.copy(alpha = 0.2f)
    }

    Box {
        Box(
            modifier = Modifier
                .size(28.dp.scaled())
                .clip(RoundedCornerShape(6.dp.scaled()))
                .background(backgroundColor)
                .border(
                    width = 0.8.dp.scaled(),
                    color = if (isHovered) EditorColors.accent.copy(alpha = 0.4f) else Color.Transparent,
                    shape = RoundedCornerShape(6.dp.scaled())
                )
                .pointerHoverIcon(PointerIcon.Hand)
                .tooltipAnchor(tooltip)
                .hoverable(interactionSource)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon, null,
                tint = if (isHovered) EditorColors.accent else EditorColors.textSecondary,
                modifier = Modifier.size(16.dp.scaled())
            )
        }
    }
}

@Composable
private fun TimelineFrameActionBtn(
    icon: ImageVector,
    tooltip: String,
    iconTint: Color,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isPressed by interactionSource.collectIsPressedAsState()

    val backgroundColor = when {
        isPressed -> iconTint.copy(alpha = 0.35f)
        isHovered -> iconTint.copy(alpha = 0.2f)
        else -> EditorColors.surfaceVariant.copy(alpha = 0.15f)
    }

    Box(contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(30.dp.scaled())
                .clip(RoundedCornerShape(6.dp.scaled()))
                .background(backgroundColor)
                .border(
                    width = 0.8.dp.scaled(),
                    color = if (isHovered) iconTint.copy(alpha = 0.5f) else Color.Transparent,
                    shape = RoundedCornerShape(6.dp.scaled())
                )
                .pointerHoverIcon(PointerIcon.Hand)
                .tooltipAnchor(tooltip)
                .hoverable(interactionSource)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon, null,
                tint = if (isHovered) iconTint else EditorColors.textSecondary,
                modifier = Modifier.size(16.dp.scaled())
            )
        }
    }
}

@Composable
private fun PlayPauseBtn(isPlaying: Boolean, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isPressed by interactionSource.collectIsPressedAsState()

    val backgroundColor = if (isPlaying) EditorColors.accent.copy(alpha = 0.15f) else EditorColors.accent.copy(alpha = 0.25f)

    Box(contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(36.dp.scaled())
                .clip(RoundedCornerShape(8.dp.scaled()))
                .background(backgroundColor)
                .border(
                    width = 1.dp.scaled(),
                    color = if (isHovered) EditorColors.accent.copy(alpha = 0.6f) else EditorColors.accent.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(8.dp.scaled())
                )
                .pointerHoverIcon(PointerIcon.Hand)
                .tooltipAnchor(if (isPlaying) EditorStrings.observeString("pause") else EditorStrings.observeString("play"))
                .hoverable(interactionSource)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (isPlaying) EditorIcons.iconPause else EditorIcons.iconPlayArrow,
                null,
                tint = EditorColors.accent,
                modifier = Modifier.size(22.dp.scaled())
            )
        }
    }
}
