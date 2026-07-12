package org.example.animation.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import org.example.animation.engine.AnimationEngine
import org.example.animation.localization.EditorStrings
import org.example.animation.ui.theme.*

@Composable
fun TimelinePanel(engine: AnimationEngine, modifier: Modifier = Modifier) {
    val project by engine.project.collectAsState()
    val currentFrameIndex by engine.currentFrameIndex.collectAsState()
    val currentLayerIndex by engine.currentLayerIndex.collectAsState()
    val isPlaying by engine.isPlaying.collectAsState()

    Surface(modifier = modifier.fillMaxWidth(), color = EditorColors.timelineBackground, elevation = 4.dp.scaled()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
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

            // Frames Grid
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

                Box(modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState())) {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        project.layers.forEachIndexed { layerIndex, _ ->
                            Row(modifier = Modifier.height(32.dp.scaled())) {
                                for (frameIndex in 0 until maxOf(project.maxFrames, 1)) {
                                    val hasContent = layerIndex < project.layers.size &&
                                            frameIndex < project.layers[layerIndex].frames.size &&
                                            project.layers[layerIndex].frames[frameIndex].strokes.isNotEmpty()
                                    val isCurrent = frameIndex == currentFrameIndex && layerIndex == currentLayerIndex

                                    val bgColor = when {
                                        isCurrent -> EditorColors.timelineCellActive
                                        hasContent -> EditorColors.timelineCellHasContent.copy(alpha = 0.3f)
                                        else -> EditorColors.timelineCell
                                    }

                                    Box(
                                        modifier = Modifier
                                            .width(28.dp.scaled())
                                            .fillMaxHeight()
                                            .border(width = if (isCurrent) 1.5.dp.scaled() else 0.5.dp.scaled(), color = if (isCurrent) EditorColors.accent else EditorColors.divider)
                                            .background(bgColor)
                                            .pointerHoverIcon(PointerIcon.Hand)
                                            .clickable {
                                                engine.setCurrentFrame(frameIndex)
                                                engine.setCurrentLayer(layerIndex)
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (hasContent) Box(modifier = Modifier.size(8.dp.scaled()).background(EditorColors.accent, RoundedCornerShape(2.dp.scaled())))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Divider(color = EditorColors.divider, thickness = 1.dp.scaled())

            // Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(EditorColors.panelBackground)
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp.scaled(), vertical = 6.dp.scaled()),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TControlBtn(EditorIcons.iconFirstPage) { engine.goToFirstFrame() }
                Spacer(Modifier.width(2.dp.scaled()))
                TControlBtn(EditorIcons.iconSkipPrevious) { engine.goToPreviousFrame() }
                Spacer(Modifier.width(8.dp.scaled()))

                PlayPauseBtn(isPlaying) { engine.togglePlayback() }

                Spacer(Modifier.width(8.dp.scaled()))
                TControlBtn(EditorIcons.iconSkipNext) { engine.goToNextFrame() }
                Spacer(Modifier.width(2.dp.scaled()))
                TControlBtn(EditorIcons.iconLastPage) { engine.goToLastFrame() }

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

                // Кнопки управления кадрами (правый нижний угол) с анимацией
                val frameBtnAnimSpec = spring<Float>(dampingRatio = 0.5f, stiffness = 350f)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TimelineFrameActionBtn(
                        icon = EditorIcons.iconAdd,
                        tooltip = EditorStrings.observeString("frame.add"),
                        animSpec = frameBtnAnimSpec,
                        iconTint = EditorColors.accent
                    ) { engine.addFrame() }
                    Spacer(Modifier.width(4.dp.scaled()))
                    TimelineFrameActionBtn(
                        icon = EditorIcons.iconContentCopy,
                        tooltip = EditorStrings.observeString("frame.duplicate"),
                        animSpec = frameBtnAnimSpec,
                        iconTint = EditorColors.accent
                    ) { engine.duplicateFrame() }
                    Spacer(Modifier.width(4.dp.scaled()))
                    TimelineFrameActionBtn(
                        icon = EditorIcons.iconDelete,
                        tooltip = EditorStrings.observeString("frame.delete"),
                        animSpec = frameBtnAnimSpec,
                        iconTint = EditorColors.accentRed
                    ) { engine.removeFrame() }
                    Spacer(Modifier.width(4.dp.scaled()))
                    TimelineFrameActionBtn(
                        icon = EditorIcons.iconClear,
                        tooltip = EditorStrings.observeString("frame.clear"),
                        animSpec = frameBtnAnimSpec,
                        iconTint = EditorColors.accentOrange
                    ) { engine.clearFrame() }
                }
            }
        }
    }
}

@Composable
private fun TControlBtn(icon: ImageVector, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else if (isHovered) 1.2f else 1.0f,
        animationSpec = spring(dampingRatio = 0.4f, stiffness = 400f)
    )

    val backgroundColor by animateColorAsState(
        targetValue = when {
            isPressed -> EditorColors.accent.copy(alpha = 0.3f)
            isHovered -> EditorColors.hover
            else -> EditorColors.surfaceVariant.copy(alpha = 0.2f)
        },
        animationSpec = tween(150)
    )

    Box(
        modifier = Modifier
            .size(28.dp.scaled())
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .clip(RoundedCornerShape(6.dp.scaled()))
            .background(backgroundColor)
            .border(
                width = 0.8.dp.scaled(),
                color = if (isHovered) EditorColors.accent.copy(alpha = 0.4f) else Color.Transparent,
                shape = RoundedCornerShape(6.dp.scaled())
            )
            .pointerHoverIcon(PointerIcon.Hand)
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

@Composable
private fun TimelineFrameActionBtn(
    icon: ImageVector,
    tooltip: String,
    animSpec: androidx.compose.animation.core.SpringSpec<Float>,
    iconTint: Color,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.85f else if (isHovered) 1.25f else 1.0f,
        animationSpec = animSpec
    )

    val backgroundColor by animateColorAsState(
        targetValue = when {
            isPressed -> iconTint.copy(alpha = 0.35f)
            isHovered -> iconTint.copy(alpha = 0.2f)
            else -> EditorColors.surfaceVariant.copy(alpha = 0.15f)
        },
        animationSpec = tween(180)
    )

    Box(
        modifier = Modifier
            .size(30.dp.scaled())
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .clip(RoundedCornerShape(6.dp.scaled()))
            .background(backgroundColor)
            .border(
                width = 0.8.dp.scaled(),
                color = if (isHovered) iconTint.copy(alpha = 0.5f) else Color.Transparent,
                shape = RoundedCornerShape(6.dp.scaled())
            )
            .pointerHoverIcon(PointerIcon.Hand)
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

@Composable
private fun PlayPauseBtn(isPlaying: Boolean, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.85f else if (isHovered) 1.15f else 1.0f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 300f)
    )

    val backgroundColor by animateColorAsState(
        targetValue = if (isPlaying) EditorColors.accent.copy(alpha = 0.15f) else EditorColors.accent.copy(alpha = 0.25f),
        animationSpec = tween(200)
    )

    Box(
        modifier = Modifier
            .size(36.dp.scaled())
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .clip(RoundedCornerShape(8.dp.scaled()))
            .background(backgroundColor)
            .border(
                width = 1.dp.scaled(),
                color = if (isHovered) EditorColors.accent.copy(alpha = 0.6f) else EditorColors.accent.copy(alpha = 0.3f),
                shape = RoundedCornerShape(8.dp.scaled())
            )
            .pointerHoverIcon(PointerIcon.Hand)
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