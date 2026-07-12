package org.example.animation.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import org.example.animation.engine.AnimationEngine
import org.example.animation.localization.EditorStrings
import org.example.animation.ui.theme.EditorColors
import org.example.animation.ui.theme.EditorIcons

@Composable
fun TimelinePanel(engine: AnimationEngine, modifier: Modifier = Modifier) {
    val project by engine.project.collectAsState()
    val currentFrameIndex by engine.currentFrameIndex.collectAsState()
    val currentLayerIndex by engine.currentLayerIndex.collectAsState()
    val isPlaying by engine.isPlaying.collectAsState()

    Surface(modifier = modifier.fillMaxWidth(), color = EditorColors.timelineBackground, elevation = 4.dp) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header - Fully Localized
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(EditorColors.panelHeader)
                    .padding(horizontal = 8.dp, vertical = 4.dp), 
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(EditorIcons.iconTimeline, null, tint = EditorColors.textSecondary, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    EditorStrings.observeString("panel.timeline").uppercase(), 
                    color = EditorColors.textSecondary, 
                    fontSize = 11.sp, 
                    fontWeight = FontWeight.SemiBold, 
                    letterSpacing = 1.sp
                )
                Spacer(Modifier.width(16.dp))
                Text(
                    "${EditorStrings.observeString("status.frame")}: ${currentFrameIndex + 1}/${project.maxFrames}", 
                    color = EditorColors.textPrimary, 
                    fontSize = 11.sp
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    "${EditorStrings.observeString("timeline.fps")}: ${project.fps}", 
                    color = EditorColors.textSecondary, 
                    fontSize = 11.sp
                )
                Spacer(Modifier.weight(1f))
                
                TSmallBtn(EditorIcons.iconAdd, EditorStrings.observeString("frame.add")) { engine.addFrame() }
                Spacer(Modifier.width(4.dp))
                TSmallBtn(EditorIcons.iconContentCopy, EditorStrings.observeString("frame.duplicate")) { engine.duplicateFrame() }
                Spacer(Modifier.width(4.dp))
                TSmallBtn(EditorIcons.iconDelete, EditorStrings.observeString("frame.delete")) { engine.removeFrame() }
                Spacer(Modifier.width(4.dp))
                TSmallBtn(EditorIcons.iconClear, EditorStrings.observeString("frame.clear")) { engine.clearFrame() }
            }

            Divider(color = EditorColors.dividerColor, thickness = 1.dp)

            // Frames Grid
            Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .width(100.dp)
                        .background(EditorColors.panelBackground)
                        .verticalScroll(rememberScrollState())
                ) {
                    project.layers.forEachIndexed { index, layer ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(32.dp)
                                .background(if (index == currentLayerIndex) EditorColors.selectionColor else Color.Transparent)
                                .pointerHoverIcon(PointerIcon.Hand)
                                .clickable { engine.setCurrentLayer(index) }
                                .padding(horizontal = 8.dp), 
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(layer.name, color = EditorColors.textPrimary, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
                
                Box(modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState())) {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        project.layers.forEachIndexed { layerIndex, _ ->
                            Row(modifier = Modifier.height(32.dp)) {
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
                                            .width(28.dp)
                                            .fillMaxHeight()
                                            .border(width = if (isCurrent) 1.5.dp else 0.5.dp, color = if (isCurrent) EditorColors.accentBlue else EditorColors.dividerColor)
                                            .background(bgColor)
                                            .pointerHoverIcon(PointerIcon.Hand)
                                            .clickable { 
                                                engine.setCurrentFrame(frameIndex)
                                                engine.setCurrentLayer(layerIndex)
                                            }, 
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (hasContent) Box(modifier = Modifier.size(8.dp).background(EditorColors.accentBlue, RoundedCornerShape(2.dp)))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Divider(color = EditorColors.dividerColor, thickness = 1.dp)

            // Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(EditorColors.panelBackground)
                    .padding(horizontal = 8.dp, vertical = 6.dp), 
                verticalAlignment = Alignment.CenterVertically
            ) {
                TSmallBtn(EditorIcons.iconFirstPage, EditorStrings.observeString("frame.first")) { engine.goToFirstFrame() }
                Spacer(Modifier.width(2.dp))
                TSmallBtn(EditorIcons.iconSkipPrevious, EditorStrings.observeString("frame.prev")) { engine.goToPreviousFrame() }
                Spacer(Modifier.width(8.dp))
                
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(EditorColors.accentBlue.copy(alpha = 0.2f))
                        .pointerHoverIcon(PointerIcon.Hand)
                        .clickable { engine.togglePlayback() }, 
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (isPlaying) EditorIcons.iconPause else EditorIcons.iconPlayArrow, 
                        null, 
                        tint = EditorColors.accentBlue, 
                        modifier = Modifier.size(22.dp)
                    )
                }
                
                Spacer(Modifier.width(8.dp))
                TSmallBtn(EditorIcons.iconSkipNext, EditorStrings.observeString("frame.next")) { engine.goToNextFrame() }
                Spacer(Modifier.width(2.dp))
                TSmallBtn(EditorIcons.iconLastPage, EditorStrings.observeString("frame.last")) { engine.goToLastFrame() }
                
                Spacer(Modifier.width(24.dp))
                
                val totalMs = project.maxFrames * 1000 / project.fps
                val sec = totalMs / 1000
                Text(
                    "${currentFrameIndex + 1} | ${sec / 60}:${(sec % 60).toString().padStart(2, '0')}", 
                    color = EditorColors.textPrimary, 
                    fontSize = 13.sp, 
                    modifier = Modifier.padding(horizontal = 8.dp),
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
                
                Spacer(Modifier.weight(1f))
                
                Text(EditorStrings.observeString("timeline.fps"), color = EditorColors.textSecondary, fontSize = 11.sp)
                Spacer(Modifier.width(8.dp))
                
                var fpsText by remember(project.fps) { mutableStateOf(project.fps.toString()) }
                OutlinedTextField(
                    value = fpsText,
                    onValueChange = {
                        val filtered = it.filter { c -> c.isDigit() }.take(3)
                        fpsText = filtered
                        val v = filtered.toIntOrNull()
                        if (v != null && v in 1..240) {
                            val p = project.copy()
                            p.fps = v
                            engine.setProject(p)
                        }
                    },
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(fontSize = 12.sp, color = EditorColors.textPrimary),
                    modifier = Modifier.width(60.dp).height(32.dp),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = EditorColors.accentBlue,
                        unfocusedBorderColor = EditorColors.dividerColor,
                        backgroundColor = EditorColors.darkSurfaceVariant
                    )
                )
            }
        }
    }
}

@Composable
private fun TSmallBtn(icon: ImageVector, tooltip: String, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    
    Box {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(if (isHovered) EditorColors.buttonHoverColor else EditorColors.buttonColor)
                .pointerHoverIcon(PointerIcon.Hand)
                .hoverable(interactionSource)
                .clickable { onClick() }, 
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = EditorColors.textSecondary, modifier = Modifier.size(16.dp))
        }
        if (isHovered) {
            Surface(
                modifier = Modifier
                    .offset(y = (-36).dp)
                    .zIndex(1000f),
                color = EditorColors.darkSurfaceLight,
                shape = RoundedCornerShape(4.dp),
                elevation = 6.dp,
                border = BorderStroke(1.dp, EditorColors.dividerColor)
            ) {
                Text(
                    text = tooltip, 
                    color = Color.White, 
                    fontSize = 11.sp, 
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}
