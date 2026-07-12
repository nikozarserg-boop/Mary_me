package org.example.animation.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import org.example.animation.engine.AnimationEngine
import org.example.animation.ui.theme.EditorColors
import org.example.animation.ui.theme.EditorIcons

@Composable
fun EditorScreen(
    engine: AnimationEngine,
    uiScale: Float,
    onSave: () -> Unit,
    onLoad: () -> Unit,
    onExportGif: () -> Unit,
    onExportPng: () -> Unit,
    onExportAvi: () -> Unit,
    onNewProject: () -> Unit,
    onSettings: () -> Unit = {}
) {
    val project by engine.project.collectAsState()
    val isPlaying by engine.isPlaying.collectAsState()
    val currentFrameIndex by engine.currentFrameIndex.collectAsState()

    LaunchedEffect(Unit) {
        engine.setPanOffset(Offset.Zero)
        engine.setZoom(1f)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                scaleX = uiScale
                scaleY = uiScale
            }
            .background(EditorColors.darkBackground)
    ) {
        EditorMenuBar(
            projectName = project.name,
            onNew = onNewProject,
            onSave = onSave,
            onLoad = onLoad,
            onExportGif = onExportGif,
            onExportPng = onExportPng,
            onExportAvi = onExportAvi,
            onSettings = onSettings
        )

        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
            ToolsPanel(engine = engine)

            DrawingCanvas(
                engine = engine,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )

            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(IntrinsicSize.Min)
            ) {
                LayersPanel(engine = engine)
                ColorPicker(engine = engine, modifier = Modifier.fillMaxWidth().weight(1f))
            }
        }

        TimelinePanel(engine = engine, modifier = Modifier.fillMaxWidth())

        EditorStatusBar(
            projectName = project.name,
            canvasSize = "${project.canvasWidth}×${project.canvasHeight}",
            currentFrame = currentFrameIndex + 1,
            totalFrames = project.maxFrames,
            fps = project.fps,
            isPlaying = isPlaying,
            layerCount = project.layerCount
        )
    }
}

@Composable
private fun EditorMenuBar(
    projectName: String,
    onNew: () -> Unit,
    onSave: () -> Unit,
    onLoad: () -> Unit,
    onExportGif: () -> Unit,
    onExportPng: () -> Unit,
    onExportAvi: () -> Unit,
    onSettings: () -> Unit
) {
    Surface(modifier = Modifier.fillMaxWidth(), color = EditorColors.panelBackground, elevation = 2.dp) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            // Логотип
            Icon(EditorIcons.iconAppIcon, "MaryMe Animator", tint = EditorColors.accentBlue, modifier = Modifier.size(20.dp).padding(end = 6.dp))
            Text("MaryMe", color = EditorColors.accentBlue, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 12.dp))

            Divider(modifier = Modifier.height(24.dp).width(1.dp).padding(horizontal = 4.dp), color = EditorColors.dividerColor)

            // Быстрые кнопки
            TopBarIcon(EditorIcons.iconNewProject, "Новый проект", onClick = onNew)
            TopBarIcon(EditorIcons.iconFolderOpen, "Открыть проект", onClick = onLoad)
            TopBarIcon(EditorIcons.iconCheck, "Сохранить", onClick = onSave)
            Spacer(Modifier.width(4.dp))
            TopBarIcon(EditorIcons.iconGifIcon, "Экспорт GIF", onClick = onExportGif)
            TopBarIcon(EditorIcons.iconPngIcon, "Экспорт PNG", onClick = onExportPng)
            TopBarIcon(EditorIcons.iconAviIcon, "Экспорт AVI", onClick = onExportAvi)

            Divider(modifier = Modifier.height(24.dp).width(1.dp).padding(horizontal = 4.dp), color = EditorColors.dividerColor)

            // Меню
            DropdownMenuButton("Файл", listOf(
                "Новый проект" to onNew,
                "Открыть проект..." to onLoad,
                null,
                "Сохранить проект" to onSave
            ))
            DropdownMenuButton("Экспорт", listOf(
                "Экспорт в GIF..." to onExportGif,
                "Экспорт в PNG..." to onExportPng,
                "Экспорт в AVI..." to onExportAvi
            ))

            Spacer(Modifier.weight(1f))

            Text(projectName, color = EditorColors.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(horizontal = 8.dp))

            TopBarIcon(EditorIcons.iconSettings, "Настройки", onClick = onSettings)
        }
    }
}

@Composable
private fun TopBarIcon(icon: ImageVector, tooltip: String, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    var showTooltip by remember { mutableStateOf(false) }

    LaunchedEffect(isHovered) {
        if (isHovered) {
            delay(500)
            showTooltip = true
        } else {
            showTooltip = false
        }
    }

    Box {
        Box(modifier = Modifier.size(32.dp).clip(RoundedCornerShape(6.dp)).background(if (isHovered) EditorColors.darkSurfaceVariant else Color.Transparent).hoverable(interactionSource).clickable(indication = null, interactionSource = interactionSource) { onClick() }, contentAlignment = Alignment.Center) {
            Icon(icon, tooltip, tint = EditorColors.textSecondary, modifier = Modifier.size(18.dp))
        }
        if (showTooltip) {
            Box(modifier = Modifier.offset(y = 38.dp).zIndex(600f).clip(RoundedCornerShape(6.dp)).background(EditorColors.darkSurfaceVariant.copy(alpha = 0.98f)).border(0.5.dp, EditorColors.dividerColor, RoundedCornerShape(6.dp)).padding(horizontal = 10.dp, vertical = 5.dp)) {
                Text(tooltip, color = EditorColors.textPrimary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun DropdownMenuButton(title: String, items: List<Pair<String, () -> Unit>?>) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Text(title, color = EditorColors.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.Normal, modifier = Modifier.clip(RoundedCornerShape(4.dp)).clickable { expanded = true }.padding(horizontal = 10.dp, vertical = 6.dp))
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.background(EditorColors.darkSurface)) {
            items.forEach { item ->
                if (item == null) Divider(color = EditorColors.dividerColor)
                else DropdownMenuItem(onClick = { expanded = false; item.second() }, modifier = Modifier.background(EditorColors.darkSurface)) {
                    Text(item.first, color = EditorColors.textPrimary, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun EditorStatusBar(projectName: String, canvasSize: String, currentFrame: Int, totalFrames: Int, fps: Int, isPlaying: Boolean, layerCount: Int) {
    Surface(modifier = Modifier.fillMaxWidth(), color = EditorColors.panelHeader, elevation = 2.dp) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
            StatusItem(projectName)
            StatusItem(canvasSize)
            StatusItem("Кадр $currentFrame/$totalFrames")
            StatusItem("FPS: $fps")
            StatusItem("Слоёв: $layerCount")
            StatusItem(if (isPlaying) "▶ Воспроизведение" else "■ Стоп", color = if (isPlaying) EditorColors.accentGreen else EditorColors.textMuted)
            Spacer(Modifier.weight(1f))
            StatusItem("${projectName}.maryme")
        }
    }
}

@Composable
private fun StatusItem(text: String, color: Color = EditorColors.textSecondary) {
    Text(text = " $text ", color = color, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 3.dp).clip(RoundedCornerShape(4.dp)).background(EditorColors.darkSurfaceVariant.copy(alpha = 0.7f)).padding(horizontal = 8.dp, vertical = 3.dp))
}
