package org.example.animation.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import org.example.animation.engine.AnimationEngine
import org.example.animation.localization.EditorStrings
import org.example.animation.ui.components.tooltip.tooltipAnchor
import org.example.animation.ui.theme.*
import kotlin.math.roundToInt

@Composable
fun TimelinePanel(engine: AnimationEngine, modifier: Modifier = Modifier) {
    val project by engine.project.collectAsState()
    val currentFrameIndex by engine.currentFrameIndex.collectAsState()
    val currentLayerIndex by engine.currentLayerIndex.collectAsState()
    val isPlaying by engine.isPlaying.collectAsState()

    // Состояние drag&drop для кадров
    var dragState by remember { mutableStateOf<FrameDragState?>(null) }

    Surface(modifier = modifier.fillMaxWidth(), color = EditorColors.timelineBackground, elevation = 4.dp.scaled()) {
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
                val cellPx = 28f
                val layerHeaderHeightPx = 32f

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState())
                ) {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        project.layers.forEachIndexed { layerIndex, _ ->
                            Row(modifier = Modifier.height(32.dp.scaled())) {
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
                                            .width(28.dp.scaled())
                                            .fillMaxHeight()
                                            .border(
                                                width = if (isCurrent) 1.5.dp.scaled() else 0.5.dp.scaled(),
                                                color = when {
                                                    isDropTarget -> EditorColors.accent
                                                    isCurrent -> EditorColors.accent
                                                    else -> EditorColors.divider
                                                }
                                            )
                                            .background(
                                                if (isDragSource) EditorColors.accent.copy(alpha = 0.15f)
                                                else if (isDropTarget) EditorColors.accent.copy(alpha = 0.3f)
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
                                                            val newHoverLayer = (st.hoverLayer + (dragAmount.y / layerHeaderHeightPx).roundToInt())
                                                                .coerceIn(0, project.layers.lastIndex)
                                                            val newHoverFrame = (st.hoverFrame + (dragAmount.x / cellPx).roundToInt())
                                                                .coerceIn(0, project.maxFrames - 1)
                                                            dragState = st.copy(
                                                                offsetX = st.offsetX + dragAmount.x,
                                                                offsetY = st.offsetY + dragAmount.y,
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
                                                                // отпустили на месте — просто выбираем
                                                                engine.setCurrentFrame(st.fromFrame)
                                                                engine.setCurrentLayer(st.fromLayer)
                                                            }
                                                        }
                                                        dragState = null
                                                    },
                                                    onDragCancel = { dragState = null }
                                                )
                                            }
                                            // Визуальный «отклеенный» кадр, который держат мышкой
                                            .then(
                                                if (isDragSource) {
                                                    Modifier
                                                        .offset {
                                                            IntOffset(
                                                                dragState?.offsetX?.roundToInt() ?: 0,
                                                                dragState?.offsetY?.roundToInt() ?: 0
                                                            )
                                                        }
                                                        .zIndex(10f)
                                                        .shadow(8.dp.scaled(), RoundedCornerShape(4.dp.scaled()))
                                                        .graphicsLayer {
                                                            scaleX = 1.15f
                                                            scaleY = 1.15f
                                                            alpha = 0.9f
                                                        }
                                                } else Modifier
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (hasContent && !isDragSource) {
                                            Box(modifier = Modifier.size(8.dp.scaled()).background(EditorColors.accent, RoundedCornerShape(2.dp.scaled())))
                                        }
                                    }
                                }
                            }
                        }
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

                var fpsText by remember(project.fps) { mutableStateOf(project.fps.toString()) }
                OutlinedTextField(
                    value = fpsText,
                    onValueChange = {
                        val filtered = it.filter { c -> c.isDigit() }.take(3)
                        fpsText = filtered
                        val v = filtered.toIntOrNull()
                        if (v != null && v in 1..240) {
                            engine.setFps(v)
                        }
                    },
                    singleLine = true,
                    textStyle = EditorTypography.mono(),
                    modifier = Modifier.width(70.dp.scaled()).height(32.dp.scaled()),
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