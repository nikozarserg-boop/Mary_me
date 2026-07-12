package org.example.animation.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
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
import org.example.animation.ui.theme.*

@Composable
fun EditorScreen(
    engine: AnimationEngine,
    uiScale: Float,
    onSave: () -> Unit,
    onLoad: () -> Unit,
    onExportGif: () -> Unit,
    onExportPng: () -> Unit,
    onExportAvi: () -> Unit,
    onExportMp4: () -> Unit,
    onNewProject: () -> Unit,
    onSettings: () -> Unit = {},
    currentTheme: ThemeType,
    onThemeChange: (ThemeType) -> Unit
) {
    val project by engine.project.collectAsState()
    val isPlaying by engine.isPlaying.collectAsState()
    val currentFrameIndex by engine.currentFrameIndex.collectAsState()
    val zoom by engine.zoom.collectAsState()
    val canUndo by engine.canUndo.collectAsState()
    val canRedo by engine.canRedo.collectAsState()

    // Состояния видимости панелей из движка
    val toolsVisible by engine.isToolsVisible.collectAsState()
    val layersVisible by engine.isLayersVisible.collectAsState()
    val colorPickerVisible by engine.isColorPickerVisible.collectAsState()
    val timelineVisible by engine.isTimelineVisible.collectAsState()
    val propertiesVisible by engine.isPropertiesVisible.collectAsState()

    var leftPanelWidth by remember { mutableStateOf(UiDimensions.MinSidePanelWidth.scaled() * 1.2f) }
    var rightPanelWidth by remember { mutableStateOf(UiDimensions.MinSidePanelWidth.scaled() * 1.4f) }
    var timelineHeight by remember { mutableStateOf(UiDimensions.MinTimelineHeight.scaled() * 1.5f) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(EditorColors.background)
    ) {
        val isCompactLayout = maxWidth < 700.dp
        Column(modifier = Modifier.fillMaxSize()) {
            // Command Bar (Верхняя панель)
            EditorMenuBar(
                engine = engine,
                projectName = project.name,
                zoom = zoom,
                isPlaying = isPlaying,
                canUndo = canUndo,
                canRedo = canRedo,
                onNew = onNewProject,
                onSave = onSave,
                onLoad = onLoad,
                onExportPng = onExportPng,
                onExportMp4 = onExportMp4,
                onSettings = onSettings,
                isCompact = isCompactLayout,
                isToolsVisible = toolsVisible,
                isLayersVisible = layersVisible,
                isColorPickerVisible = colorPickerVisible,
                isTimelineVisible = timelineVisible,
                isPropertiesVisible = propertiesVisible
            )

            Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                // Тулбар (Слева)
                if (toolsVisible) {
                    ToolsPanel(engine = engine)
                }

                // Панель свойств (Слева)
                if (propertiesVisible) {
                    GlassPanel(modifier = Modifier.width(leftPanelWidth).fillMaxHeight()) {
                        Column {
                            PanelHeader(
                                title = EditorStrings.observeString("panel.tools"),
                                onClose = { engine.setPropertiesVisible(false) }
                            )
                            ToolPropertiesPanel(engine = engine)
                        }
                    }
                    VerticalSplitter { delta -> 
                        leftPanelWidth = (leftPanelWidth + delta.dp).coerceIn(UiDimensions.MinSidePanelWidth.scaled(), UiDimensions.MaxSidePanelWidth.scaled())
                    }
                }

                // Центральная область холста
                Box(modifier = Modifier.weight(1f).fillMaxHeight().background(EditorColors.canvasBackground)) {
                    DrawingCanvas(engine = engine, modifier = Modifier.fillMaxSize())
                }

                // Слои и Цвет (Справа)
                if (layersVisible || colorPickerVisible) {
                    VerticalSplitter { delta -> 
                        rightPanelWidth = (rightPanelWidth - delta.dp).coerceIn(UiDimensions.MinSidePanelWidth.scaled(), UiDimensions.MaxSidePanelWidth.scaled())
                    }
                    GlassPanel(modifier = Modifier.width(rightPanelWidth).fillMaxHeight()) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            if (layersVisible) {
                                Box(modifier = Modifier.weight(0.6f)) {
                                    Column {
                                        PanelHeader(
                                            title = EditorStrings.observeString("panel.layers"),
                                            onClose = { engine.setLayersVisible(false) }
                                        )
                                        LayersPanel(engine = engine)
                                    }
                                }
                            }
                            if (layersVisible && colorPickerVisible) {
                                Divider(color = EditorColors.divider.copy(alpha = 0.1f))
                            }
                            if (colorPickerVisible) {
                                Box(modifier = Modifier.weight(0.4f)) {
                                    Column {
                                        PanelHeader(
                                            title = EditorStrings.observeString("panel.color"),
                                            onClose = { engine.setColorPickerVisible(false) }
                                        )
                                        ColorPicker(engine = engine, modifier = Modifier.fillMaxSize())
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Timeline (Снизу)
            if (timelineVisible) {
                HorizontalSplitter { delta -> 
                    timelineHeight = (timelineHeight - delta.dp).coerceIn(UiDimensions.MinTimelineHeight.scaled(), UiDimensions.MaxTimelineHeight.scaled())
                }
                Box(modifier = Modifier.fillMaxWidth().height(timelineHeight)) {
                    TimelinePanel(engine = engine, modifier = Modifier.fillMaxSize())
                    IconButton(
                        onClick = { engine.setTimelineVisible(false) },
                        modifier = Modifier.align(Alignment.TopEnd).padding(UiDimensions.PaddingSmall.scaled()).size(UiDimensions.IconButtonSize.scaled())
                    ) {
                        Icon(EditorIcons.iconClose, null, tint = EditorColors.textSecondary, modifier = Modifier.size(UiDimensions.IconSize.scaled()))
                    }
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxWidth().height(UiDimensions.StatusBarHeight.scaled())
                        .background(EditorColors.panelHeader)
                        .clickable { engine.setTimelineVisible(true) }
                        .pointerHoverIcon(PointerIcon.Hand), 
                    contentAlignment = Alignment.Center
                ) {
                    Icon(EditorIcons.iconKeyboardArrowUp, null, tint = EditorColors.textSecondary, modifier = Modifier.size(UiDimensions.IconSize.scaled()))
                }
            }

            EditorStatusBar(project.name, "${project.canvasWidth}×${project.canvasHeight}", currentFrameIndex + 1, project.maxFrames, project.fps, isPlaying)
        }
    }
}

@Composable
private fun PanelHeader(title: String, onClose: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().background(EditorColors.panelHeader.copy(alpha = 0.5f)).padding(horizontal = 8.dp.scaled(), vertical = 2.dp.scaled()),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title.uppercase(), style = EditorTypography.panelTitle())
        IconButton(onClick = onClose, modifier = Modifier.size(16.dp.scaled())) {
            Icon(EditorIcons.iconClose, null, tint = EditorColors.textMuted, modifier = Modifier.size(10.dp.scaled()))
        }
    }
}

@Composable
fun GlassPanel(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val theme = LocalThemeType.current
    val background = if (theme == ThemeType.GLASS) EditorColors.glassBackground else EditorColors.panelBackground
    val border = if (theme == ThemeType.GLASS) EditorColors.glassBorder else EditorColors.divider.copy(alpha = 0.1f)
    
    Box(modifier = modifier.background(background)) {
        content()
        Box(modifier = Modifier.fillMaxSize().border(0.5.dp.scaled(), border))
    }
}

@Composable
private fun VerticalSplitter(onDrag: (Float) -> Unit) {
    Box(
        modifier = Modifier.fillMaxHeight().width(4.dp.scaled())
            .pointerHoverIcon(PointerIcon.Hand)
            .pointerInput(Unit) { detectDragGestures { change, dragAmount -> change.consume(); onDrag(dragAmount.x) } }
            .zIndex(100f), 
        contentAlignment = Alignment.Center
    ) {
        Box(modifier = Modifier.fillMaxHeight().width(0.5.dp.scaled()).background(EditorColors.divider))
    }
}

@Composable
private fun HorizontalSplitter(onDrag: (Float) -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth().height(4.dp.scaled())
            .pointerHoverIcon(PointerIcon.Hand)
            .pointerInput(Unit) { detectDragGestures { change, dragAmount -> change.consume(); onDrag(dragAmount.y) } }
            .zIndex(100f), 
        contentAlignment = Alignment.Center
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(0.5.dp.scaled()).background(EditorColors.divider))
    }
}

@Composable
private fun EditorMenuBar(
    engine: AnimationEngine,
    projectName: String,
    zoom: Float,
    isPlaying: Boolean,
    canUndo: Boolean,
    canRedo: Boolean,
    onNew: () -> Unit,
    onSave: () -> Unit,
    onLoad: () -> Unit,
    onExportPng: () -> Unit,
    onExportMp4: () -> Unit,
    onSettings: () -> Unit,
    isCompact: Boolean,
    isToolsVisible: Boolean,
    isLayersVisible: Boolean,
    isColorPickerVisible: Boolean,
    isTimelineVisible: Boolean,
    isPropertiesVisible: Boolean
) {
    Surface(modifier = Modifier.fillMaxWidth().height(UiDimensions.TopBarHeight.scaled()), color = EditorColors.panelHeader, elevation = 1.dp) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = UiDimensions.PaddingMedium.scaled()), verticalAlignment = Alignment.CenterVertically) {
            Icon(EditorIcons.iconAppIcon, null, tint = EditorColors.accent, modifier = Modifier.size(UiDimensions.IconSize.scaled()))
            
            Spacer(Modifier.width(UiDimensions.PaddingMedium.scaled()))

            TopBarIconButton(EditorIcons.iconNewProject, tooltip = EditorStrings.observeString("file.new")) { onNew() }
            TopBarIconButton(EditorIcons.iconFolderOpen, tooltip = EditorStrings.observeString("file.open")) { onLoad() }
            TopBarIconButton(EditorIcons.iconSave, tooltip = EditorStrings.observeString("file.save")) { onSave() }

            Spacer(Modifier.width(UiDimensions.PaddingMedium.scaled()))
            VerticalDivider()
            Spacer(Modifier.width(UiDimensions.PaddingMedium.scaled()))

            // Меню Файл
            DropdownMenuButton(EditorStrings.observeString("menu.file"), listOf(
                EditorStrings.observeString("file.new") to onNew,
                EditorStrings.observeString("file.open") to onLoad,
                null,
                EditorStrings.observeString("file.save") to onSave,
                null,
                EditorStrings.observeString("export.png") to onExportPng,
                EditorStrings.observeString("export.mp4") to onExportMp4
            ))

            // Меню Вид (Windows)
            DropdownMenuButton(EditorStrings.observeString("menu.view"), listOf(
                EditorStrings.observeString("view.resetZoom") to { engine.setZoom(1f) },
                null,
                (if (isToolsVisible) "Hide Tools" else "Show Tools") to { engine.toggleTools() },
                (if (isLayersVisible) "Hide Layers" else "Show Layers") to { engine.toggleLayers() },
                (if (isColorPickerVisible) "Hide Color Picker" else "Show Color Picker") to { engine.toggleColorPicker() },
                (if (isTimelineVisible) "Hide Timeline" else "Show Timeline") to { engine.toggleTimeline() },
                (if (isPropertiesVisible) "Hide Properties" else "Show Properties") to { engine.toggleProperties() }
            ))

            if (!isCompact) {
                Spacer(Modifier.width(UiDimensions.PaddingLarge.scaled()))
                VerticalDivider()
                Spacer(Modifier.width(UiDimensions.PaddingMedium.scaled()))

                TopBarIconButton(EditorIcons.iconUndo, enabled = canUndo, tooltip = EditorStrings.observeString("edit.undo")) { engine.undo() }
                TopBarIconButton(EditorIcons.iconRedo, enabled = canRedo, tooltip = EditorStrings.observeString("edit.redo")) { engine.redo() }

                Spacer(Modifier.width(UiDimensions.PaddingMedium.scaled()))
                VerticalDivider()
                Spacer(Modifier.width(UiDimensions.PaddingMedium.scaled()))

                TopBarIconButton(EditorIcons.iconZoomOut, tooltip = EditorStrings.observeString("view.zoomOut")) { engine.setZoom((zoom * 0.8f).coerceIn(0.1f, 20f)) }
                Text("${(zoom * 100).toInt()}%", style = EditorTypography.mono(), color = EditorColors.textSecondary, modifier = Modifier.width(40.dp.scaled()), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                TopBarIconButton(EditorIcons.iconZoomIn, tooltip = EditorStrings.observeString("view.zoomIn")) { engine.setZoom((zoom * 1.25f).coerceIn(0.1f, 20f)) }

                Spacer(Modifier.width(UiDimensions.PaddingMedium.scaled()))
                VerticalDivider()
                Spacer(Modifier.width(UiDimensions.PaddingMedium.scaled()))

                TopBarIconButton(if (isPlaying) EditorIcons.iconPause else EditorIcons.iconPlayArrow) { 
                    engine.togglePlayback()
                }
            }

            Spacer(Modifier.weight(1f))
            
            if (!isCompact) Text(projectName, style = EditorTypography.panelTitle(), color = EditorColors.textMuted)

            Spacer(Modifier.weight(1f))

            TopBarIconButton(EditorIcons.iconSettings, tooltip = EditorStrings.observeString("settings.title")) { onSettings() }
        }
    }
}

@Composable
private fun VerticalDivider() {
    Box(modifier = Modifier.width(1.dp.scaled()).height(14.dp.scaled()).background(EditorColors.divider))
}

@Composable
private fun TopBarIconButton(icon: ImageVector, enabled: Boolean = true, tooltip: String = "", onClick: () -> Unit) {
    IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(UiDimensions.IconButtonSize.scaled())) {
        Icon(icon, null, tint = if (enabled) EditorColors.textPrimary.copy(alpha = 0.7f) else EditorColors.textMuted, modifier = Modifier.size(UiDimensions.IconSize.scaled()))
    }
}

@Composable
private fun DropdownMenuButton(title: String, items: List<Pair<String, () -> Unit>?>) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { expanded = true }, modifier = Modifier.height(26.dp.scaled()), contentPadding = PaddingValues(horizontal = 8.dp.scaled())) { 
            Text(title, style = EditorTypography.menu())
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.background(EditorColors.surface).border(1.dp.scaled(), EditorColors.divider)) { 
            items.forEach { item -> 
                if (item == null) Divider(color = EditorColors.divider) 
                else DropdownMenuItem(onClick = { expanded = false; item.second() }, modifier = Modifier.height(28.dp.scaled())) { 
                    Text(item.first, style = EditorTypography.menu()) 
                } 
            } 
        }
    }
}

@Composable
private fun EditorStatusBar(projectName: String, size: String, frame: Int, total: Int, fps: Int, playing: Boolean) {
    Surface(modifier = Modifier.fillMaxWidth().height(UiDimensions.StatusBarHeight.scaled()), color = EditorColors.panelHeader) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = UiDimensions.PaddingMedium.scaled()), verticalAlignment = Alignment.CenterVertically) {
            StatusItem(projectName, EditorIcons.iconAppIcon)
            StatusItem(size, null)
            StatusItem("${EditorStrings.observeString("status.frame")} $frame/$total", EditorIcons.iconTimeline)
            StatusItem("FPS: $fps", null)
            Spacer(Modifier.weight(1f))
            Box(modifier = Modifier.size(5.dp.scaled()).clip(RoundedCornerShape(2.5.dp.scaled())).background(if (playing) EditorColors.accentGreen else EditorColors.accentRed))
        }
    }
}

@Composable
private fun StatusItem(text: String, icon: ImageVector?) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 10.dp.scaled())) {
        if (icon != null) { Icon(icon, null, tint = EditorColors.textMuted, modifier = Modifier.size(9.dp.scaled())); Spacer(Modifier.width(3.dp.scaled())) }
        Text(text, style = EditorTypography.statusBar())
    }
}
