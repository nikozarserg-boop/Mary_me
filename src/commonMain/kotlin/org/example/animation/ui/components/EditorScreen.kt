package org.example.animation.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import org.example.animation.engine.AnimationEngine
import org.example.animation.io.AppSettingsManager
import org.example.animation.io.RecentProject
import org.example.animation.localization.EditorStrings
import org.example.animation.ui.theme.EditorColors
import org.example.animation.ui.theme.EditorIcons
import org.example.animation.ui.theme.EditorShapes
import org.example.animation.ui.theme.EditorTypography

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

    var leftPanelWidth by remember { mutableStateOf(220.dp) }
    var rightPanelWidth by remember { mutableStateOf(300.dp) }
    var timelineHeight by remember { mutableStateOf(200.dp) }
    
    var isLeftPanelVisible by remember { mutableStateOf(true) }
    var isRightPanelVisible by remember { mutableStateOf(true) }
    var isTimelineVisible by remember { mutableStateOf(true) }

    val density = LocalDensity.current

    LaunchedEffect(Unit) {
        engine.setPanOffset(Offset.Zero)
        engine.setZoom(1f)
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(EditorColors.darkBackground)
            .graphicsLayer {
                scaleX = uiScale
                scaleY = uiScale
                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0f)
            }
    ) {
        val screenWidth = maxWidth
        val isCompact = screenWidth < 850.dp

        Column(modifier = Modifier.fillMaxSize()) {
            EditorMenuBar(
                projectName = project.name,
                onNew = onNewProject,
                onSave = onSave,
                onLoad = onLoad,
                onExportGif = onExportGif,
                onExportPng = onExportPng,
                onExportAvi = onExportAvi,
                onSettings = onSettings,
                isCompact = isCompact,
                recentProjects = AppSettingsManager.getRecentProjects()
            )

            Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                ToolsPanel(engine = engine)

                if (!isCompact && isLeftPanelVisible) {
                    Column(modifier = Modifier.width(leftPanelWidth).fillMaxHeight().background(EditorColors.panelBackground)) {
                        ToolPropertiesPanel(engine = engine)
                    }
                    VerticalSplitter { delta -> with(density) { leftPanelWidth = (leftPanelWidth + delta.toDp()).coerceIn(150.dp, 400.dp) } }
                }

                Box(modifier = Modifier.weight(1f).fillMaxHeight().background(EditorColors.canvasBackground)) {
                    DrawingCanvas(engine = engine, modifier = Modifier.fillMaxSize())
                    
                    if (!isCompact) {
                        SideCollapseButton(isVisible = isLeftPanelVisible, isLeft = true, modifier = Modifier.align(Alignment.CenterStart), onClick = { isLeftPanelVisible = !isLeftPanelVisible })
                        SideCollapseButton(isVisible = isRightPanelVisible, isLeft = false, modifier = Modifier.align(Alignment.CenterEnd), onClick = { isRightPanelVisible = !isRightPanelVisible })
                    }
                }

                if (!isCompact && isRightPanelVisible) {
                    VerticalSplitter { delta -> with(density) { rightPanelWidth = (rightPanelWidth - delta.toDp()).coerceIn(240.dp, 600.dp) } }
                    Column(modifier = Modifier.width(rightPanelWidth).fillMaxHeight().background(EditorColors.panelBackground)) {
                        Box(modifier = Modifier.weight(0.6f)) { LayersPanel(engine = engine) }
                        Divider(color = EditorColors.dividerColor)
                        Box(modifier = Modifier.weight(0.4f)) { ColorPicker(engine = engine, modifier = Modifier.fillMaxSize()) }
                    }
                }
            }

            if (isTimelineVisible) {
                HorizontalSplitter { delta -> with(density) { timelineHeight = (timelineHeight - delta.toDp()).coerceIn(120.dp, 500.dp) } }
                TimelinePanel(engine = engine, modifier = Modifier.fillMaxWidth().height(if (isCompact) 150.dp else timelineHeight))
            } else {
                Box(modifier = Modifier.fillMaxWidth().height(24.dp).background(EditorColors.panelHeader).clickable { isTimelineVisible = true }.pointerHoverIcon(PointerIcon.Hand), contentAlignment = Alignment.Center) {
                    Icon(EditorIcons.iconKeyboardArrowUp, null, tint = EditorColors.textSecondary, modifier = Modifier.size(16.dp))
                }
            }

            EditorStatusBar(projectName = project.name, canvasSize = "${project.canvasWidth}×${project.canvasHeight}", currentFrame = currentFrameIndex + 1, totalFrames = project.maxFrames, fps = project.fps, isPlaying = isPlaying, layerCount = project.layerCount, isCompact = isCompact)
        }
    }
}

@Composable
private fun VerticalSplitter(onDrag: (Float) -> Unit) {
    Box(modifier = Modifier.fillMaxHeight().width(6.dp).pointerHoverIcon(PointerIcon.Hand).pointerInput(Unit) { detectDragGestures { change, dragAmount -> change.consume(); onDrag(dragAmount.x) } }.zIndex(100f), contentAlignment = Alignment.Center) {
        Box(modifier = Modifier.fillMaxHeight().width(1.dp).background(EditorColors.dividerColor))
    }
}

@Composable
private fun HorizontalSplitter(onDrag: (Float) -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().height(6.dp).pointerHoverIcon(PointerIcon.Hand).pointerInput(Unit) { detectDragGestures { change, dragAmount -> change.consume(); onDrag(dragAmount.y) } }.zIndex(100f), contentAlignment = Alignment.Center) {
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(EditorColors.dividerColor))
    }
}

@Composable
private fun SideCollapseButton(isVisible: Boolean, isLeft: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(modifier = modifier.fillMaxHeight().width(14.dp).background(EditorColors.darkSurface.copy(alpha = 0.4f)).clickable { onClick() }.pointerHoverIcon(PointerIcon.Hand), contentAlignment = Alignment.Center) {
        Icon(if (isVisible) (if (isLeft) EditorIcons.iconArrowBack else EditorIcons.iconArrowForward) else (if (isLeft) EditorIcons.iconArrowForward else EditorIcons.iconArrowBack), null, tint = EditorColors.textSecondary, modifier = Modifier.size(10.dp))
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
    onSettings: () -> Unit, 
    isCompact: Boolean,
    recentProjects: List<RecentProject>
) {
    Surface(modifier = Modifier.fillMaxWidth(), color = EditorColors.panelHeader, elevation = 4.dp) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(EditorIcons.iconAppIcon, "Logo", tint = EditorColors.accentBlue, modifier = Modifier.size(24.dp).padding(end = 6.dp).pointerHoverIcon(PointerIcon.Hand))
            
            DropdownMenuButton(EditorStrings.observeString("menu.file"), listOf(
                EditorStrings.observeString("file.new") to onNew,
                EditorStrings.observeString("file.open") to onLoad,
                null,
                EditorStrings.observeString("file.save") to onSave
            ))

            if (recentProjects.isNotEmpty() && !isCompact) {
                var expandedRecent by remember { mutableStateOf(false) }
                Box {
                    TextButton(onClick = { expandedRecent = true }, modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)) {
                        Text(EditorStrings.observeString("file.recent"), fontSize = 13.sp, color = EditorColors.textSecondary)
                    }
                    DropdownMenu(expanded = expandedRecent, onDismissRequest = { expandedRecent = false }, modifier = Modifier.background(EditorColors.darkSurface)) {
                        recentProjects.forEach { project ->
                            DropdownMenuItem(onClick = { expandedRecent = false }) {
                                Column {
                                    Text(project.name, color = EditorColors.textPrimary, fontSize = 12.sp)
                                    Text(project.path, color = EditorColors.textMuted, fontSize = 9.sp)
                                }
                            }
                        }
                    }
                }
            }
            
            if (!isCompact) {
                DropdownMenuButton(EditorStrings.observeString("export.title"), listOf(
                    EditorStrings.observeString("file.exportGif") to onExportGif,
                    EditorStrings.observeString("file.exportPng") to onExportPng,
                    EditorStrings.observeString("file.exportAvi") to onExportAvi
                ))
            }

            Spacer(Modifier.weight(1f))
            TopBarIcon(EditorIcons.iconSettings, EditorStrings.observeString("settings.title"), onClick = onSettings)
        }
    }
}

@Composable
private fun TopBarIcon(icon: ImageVector, tooltip: String, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    IconButton(onClick = onClick, modifier = Modifier.size(36.dp).clip(EditorShapes.buttonRounded).background(if (isHovered) EditorColors.buttonHoverColor else Color.Transparent).pointerHoverIcon(PointerIcon.Hand), interactionSource = interactionSource) { Icon(icon, tooltip, tint = if (isHovered) EditorColors.accentBlue else EditorColors.textSecondary, modifier = Modifier.size(20.dp)) }
}

@Composable
private fun DropdownMenuButton(title: String, items: List<Pair<String, () -> Unit>?>) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { expanded = true }, colors = ButtonDefaults.textButtonColors(contentColor = EditorColors.textPrimary), modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)) { Text(title, fontSize = 13.sp, fontWeight = FontWeight.Normal) }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.background(EditorColors.darkSurface).border(1.dp, EditorColors.dividerColor)) { items.forEach { item -> if (item == null) Divider(color = EditorColors.dividerColor, modifier = Modifier.padding(vertical = 4.dp)) else DropdownMenuItem(onClick = { expanded = false; item.second() }, modifier = Modifier.height(36.dp).pointerHoverIcon(PointerIcon.Hand)) { Text(item.first, color = EditorColors.textPrimary, fontSize = 12.sp) } } }
    }
}

@Composable
private fun EditorStatusBar(projectName: String, canvasSize: String, currentFrame: Int, totalFrames: Int, fps: Int, isPlaying: Boolean, layerCount: Int, isCompact: Boolean) {
    Surface(modifier = Modifier.fillMaxWidth(), color = EditorColors.panelHeader, elevation = 8.dp) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            if (!isCompact) StatusItem(projectName, EditorIcons.iconAppIcon)
            StatusItem(canvasSize, null)
            StatusItem("${EditorStrings.observeString("status.frame")} $currentFrame/$totalFrames", EditorIcons.iconTimeline)
            if (!isCompact) StatusItem("FPS: $fps", null)
            Spacer(Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(if (isPlaying) EditorColors.accentGreen else EditorColors.accentRed))
                if (!isCompact) { Spacer(Modifier.width(6.dp)); Text(text = if (isPlaying) EditorStrings.observeString("status.playing") else EditorStrings.observeString("status.stopped"), color = EditorColors.textSecondary, fontSize = 11.sp) }
            }
        }
    }
}

@Composable
private fun StatusItem(text: String, icon: ImageVector? = null) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 12.dp)) {
        if (icon != null) { Icon(icon, null, tint = EditorColors.textMuted, modifier = Modifier.size(12.dp)); Spacer(Modifier.width(4.dp)) }
        Text(text = text, color = EditorColors.textSecondary, fontSize = 11.sp)
    }
}
