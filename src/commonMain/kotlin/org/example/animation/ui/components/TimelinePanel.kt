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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import org.example.animation.engine.AnimationEngine
import org.example.animation.ui.theme.EditorColors
import org.example.animation.ui.theme.EditorIcons

@Composable
fun TimelinePanel(engine: AnimationEngine, modifier: Modifier = Modifier) {
    val project by engine.project.collectAsState()
    val currentFrameIndex by engine.currentFrameIndex.collectAsState()
    val currentLayerIndex by engine.currentLayerIndex.collectAsState()
    val isPlaying by engine.isPlaying.collectAsState()

    Surface(modifier = modifier.fillMaxWidth().height(180.dp), color = EditorColors.timelineBackground, elevation = 4.dp) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Заголовок
            Row(modifier = Modifier.fillMaxWidth().background(EditorColors.panelHeader).padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(EditorIcons.iconTimeline, "Таймлайн", tint = EditorColors.textSecondary, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text("ТАЙМЛАЙН", color = EditorColors.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
                Spacer(Modifier.width(16.dp))
                Text("Кадр: ${currentFrameIndex + 1}/${project.maxFrames}", color = EditorColors.textPrimary, fontSize = 11.sp)
                Spacer(Modifier.width(12.dp))
                Text("FPS: ${project.fps}", color = EditorColors.textSecondary, fontSize = 11.sp)
                Spacer(Modifier.weight(1f))
                TSmallBtn(EditorIcons.iconAdd, "Добавить кадр") { engine.addFrame() }
                Spacer(Modifier.width(4.dp))
                TSmallBtn(EditorIcons.iconContentCopy, "Дублировать кадр") { engine.duplicateFrame() }
                Spacer(Modifier.width(4.dp))
                TSmallBtn(EditorIcons.iconDelete, "Удалить кадр") { engine.removeFrame() }
                Spacer(Modifier.width(4.dp))
                TSmallBtn(EditorIcons.iconClear, "Очистить кадр") { engine.clearFrame() }
            }

            Divider(color = EditorColors.dividerColor, thickness = 1.dp)

            // Сетка кадров
            Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                Column(modifier = Modifier.width(80.dp).background(EditorColors.panelBackground).verticalScroll(rememberScrollState())) {
                    project.layers.forEachIndexed { index, layer ->
                        Box(modifier = Modifier.fillMaxWidth().height(28.dp).background(if (index == currentLayerIndex) EditorColors.selectionColor else Color.Transparent).clickable { engine.setCurrentLayer(index) }.padding(horizontal = 6.dp), contentAlignment = Alignment.CenterStart) {
                            Text(layer.name, color = EditorColors.textPrimary, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
                Box(modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState())) {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        project.layers.forEachIndexed { layerIndex, _ ->
                            Row(modifier = Modifier.height(28.dp)) {
                                for (frameIndex in 0 until maxOf(project.maxFrames, 1)) {
                                    val hasContent = layerIndex < project.layers.size && frameIndex < project.layers[layerIndex].frames.size && project.layers[layerIndex].frames[frameIndex].strokes.isNotEmpty()
                                    val isCurrent = frameIndex == currentFrameIndex && layerIndex == currentLayerIndex
                                    val bgColor = when { isCurrent -> EditorColors.timelineCellActive; hasContent -> EditorColors.timelineCellHasContent.copy(alpha = 0.3f); else -> EditorColors.timelineCell }
                                    Box(modifier = Modifier.width(24.dp).height(28.dp).border(width = if (isCurrent) 1.5.dp else 0.5.dp, color = if (isCurrent) EditorColors.accentBlue else EditorColors.dividerColor).background(bgColor).clickable { engine.setCurrentFrame(frameIndex) }, contentAlignment = Alignment.Center) {
                                        if (hasContent) Box(modifier = Modifier.size(6.dp).background(EditorColors.accentBlue, RoundedCornerShape(1.dp)))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Divider(color = EditorColors.dividerColor, thickness = 1.dp)

            // Управление
            Row(modifier = Modifier.fillMaxWidth().background(EditorColors.panelBackground).padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                TSmallBtn(EditorIcons.iconFirstPage, "Первый кадр") { engine.goToFirstFrame() }
                Spacer(Modifier.width(2.dp))
                TSmallBtn(EditorIcons.iconSkipPrevious, "Предыдущий кадр") { engine.goToPreviousFrame() }
                Spacer(Modifier.width(4.dp))
                Box(modifier = Modifier.size(32.dp).clip(RoundedCornerShape(4.dp)).background(EditorColors.accentBlue.copy(alpha = 0.3f)).clickable { engine.togglePlayback() }, contentAlignment = Alignment.Center) {
                    Icon(if (isPlaying) EditorIcons.iconPause else EditorIcons.iconPlayArrow, if (isPlaying) "Пауза" else "Воспроизвести", tint = EditorColors.accentBlue, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(4.dp))
                TSmallBtn(EditorIcons.iconSkipNext, "Следующий кадр") { engine.goToNextFrame() }
                Spacer(Modifier.width(2.dp))
                TSmallBtn(EditorIcons.iconLastPage, "Последний кадр") { engine.goToLastFrame() }
                Spacer(Modifier.width(16.dp))
                val totalMs = project.maxFrames * 1000 / project.fps; val sec = totalMs / 1000
                Text("$currentFrameIndex : ${sec / 60}:${sec % 60}", color = EditorColors.textPrimary, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 8.dp))
                Spacer(Modifier.weight(1f))
                Text("FPS:", color = EditorColors.textSecondary, fontSize = 11.sp)
                Spacer(Modifier.width(4.dp))
                var fpsText by remember(project.fps) { mutableStateOf(project.fps.toString()) }
                OutlinedTextField(
                    value = fpsText,
                    onValueChange = {
                        fpsText = it.filter { c -> c.isDigit() }.take(2)
                        val v = fpsText.toIntOrNull()
                        if (v != null && v >= 1 && v <= 60) {
                            val p = engine.project.value
                            p.fps = v
                            engine.setProject(p)
                        }
                    },
                    placeholder = { Text("24", color = EditorColors.textMuted, fontSize = 12.sp) },
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(fontSize = 12.sp, color = EditorColors.textPrimary),
                    modifier = Modifier.width(48.dp).height(28.dp),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = EditorColors.accentBlue,
                        unfocusedBorderColor = EditorColors.dividerColor,
                        cursorColor = EditorColors.accentBlue,
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
        Box(modifier = Modifier.size(24.dp).clip(RoundedCornerShape(3.dp)).background(EditorColors.buttonColor).hoverable(interactionSource).clickable { onClick() }, contentAlignment = Alignment.Center) {
            Icon(icon, tooltip, tint = EditorColors.textSecondary, modifier = Modifier.size(14.dp))
        }
        if (isHovered) {
            Box(modifier = Modifier.offset(x = 0.dp, y = (-28).dp).zIndex(10f).clip(RoundedCornerShape(4.dp)).background(EditorColors.darkSurfaceVariant.copy(alpha = 0.95f)).border(0.5.dp, EditorColors.dividerColor, RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                Text(text = tooltip, color = EditorColors.textPrimary, fontSize = 11.sp, fontWeight = FontWeight.Normal)
            }
        }
    }
}