package org.example.animation.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.zIndex
import org.example.animation.engine.AnimationEngine
import org.example.animation.engine.ProjectManager
import org.example.animation.io.AppSettingsManager
import org.example.animation.io.RecentProject
import org.example.animation.localization.EditorStrings
import org.example.animation.ui.components.tooltip.tooltipAnchor
import org.example.animation.ui.theme.*

@Composable
fun GlassPanel(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val theme = LocalThemeType.current
    val isGlass = theme == ThemeType.GLASS
    val background = if (isGlass) EditorColors.glassBackground else EditorColors.panelBackground
    val border = if (isGlass) EditorColors.glassBorder else EditorColors.divider.copy(alpha = 0.1f)
    val shape = if (isGlass) RoundedCornerShape(10.dp.scaled()) else RoundedCornerShape(0.dp)

    Box(
        modifier = modifier
            .clip(shape)
            .background(background)
    ) {
        content()
        if (isGlass) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(shape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(EditorColors.glassSheen, Color.Transparent),
                            center = Offset(0.25f, 0.15f),
                            radius = 1.1f
                        )
                    )
            )
        }
        Box(modifier = Modifier.fillMaxSize().border(1.dp.scaled(), border).clip(shape))
    }
}

@Composable
private fun VerticalSplitter(onDrag: (Float) -> Unit) {
    Box(
        modifier = Modifier.width(4.dp.scaled()).fillMaxHeight()
            .pointerHoverIcon(PointerIcon.Hand)
            .pointerInput(Unit) { detectDragGestures { change, dragAmount -> change.consume(); onDrag(dragAmount.x) } }
            .zIndex(100f),
        contentAlignment = Alignment.Center
    ) {
        Box(modifier = Modifier.width(1.dp.scaled()).fillMaxHeight().background(EditorColors.divider))
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
        Box(modifier = Modifier.fillMaxWidth().height(1.dp.scaled()).background(EditorColors.divider))
    }
}

@Composable
fun EditorScreen(
    engine: AnimationEngine,
    projectManager: ProjectManager,
    uiScale: Float,
    onSave: () -> Unit,
    onLoad: () -> Unit,
    onExport: (String) -> Unit,
    onNewProject: () -> Unit,
    onSettings: () -> Unit = {},
    onImportImage: () -> Unit = {},
    currentTheme: ThemeType,
    onThemeChange: (ThemeType) -> Unit,
    onCloseTab: (Int) -> Unit
) {
    val project by engine.project.collectAsState()
    val isPlaying by engine.isPlaying.collectAsState()
    val currentFrameIndex by engine.currentFrameIndex.collectAsState()
    val zoom by engine.zoom.collectAsState()
    val rotation by engine.rotation.collectAsState()
    val canUndo by engine.canUndo.collectAsState()
    val canRedo by engine.canRedo.collectAsState()

    val toolsVisible by engine.isToolsVisible.collectAsState()
    val layersVisible by engine.isLayersVisible.collectAsState()
    val colorPickerVisible by engine.isColorPickerVisible.collectAsState()
    val timelineVisible by engine.isTimelineVisible.collectAsState()
    val propertiesVisible by engine.isPropertiesVisible.collectAsState()
    
    val toolsCollapsed by engine.isToolsCollapsed.collectAsState()
    val layersCollapsed by engine.isLayersCollapsed.collectAsState()
    val colorPickerCollapsed by engine.isColorPickerCollapsed.collectAsState()
    val timelineCollapsed by engine.isTimelineCollapsed.collectAsState()
    val propertiesCollapsed by engine.isPropertiesCollapsed.collectAsState()

    var leftPanelWidth by remember { mutableStateOf(UiDimensions.MinSidePanelWidth.scaledNonReactive() * 1.2f) }
    var rightPanelWidth by remember { mutableStateOf(UiDimensions.MinSidePanelWidth.scaledNonReactive() * 1.4f) }
    var timelineHeight by remember { mutableStateOf(UiDimensions.MinTimelineHeight.scaledNonReactive() * 1.5f) }
    
    var rightPanelSplitRatio by remember { mutableStateOf(0.5f) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(EditorColors.background)
    ) {
        val isCompactLayout = maxWidth < 750.dp.scaled()
        Column(modifier = Modifier.fillMaxSize()) {
            EditorMenuBar(
                engine = engine,
                projectName = project.name,
                zoom = zoom,
                rotation = rotation,
                isPlaying = isPlaying,
                canUndo = canUndo,
                canRedo = canRedo,
                onNew = onNewProject,
                onSave = onSave,
                onLoad = onLoad,
                onExport = onExport,
                onSettings = onSettings,
                isCompact = isCompactLayout,
                isToolsVisible = toolsVisible,
                isLayersVisible = layersVisible,
                isColorPickerVisible = colorPickerVisible,
                isTimelineVisible = timelineVisible,
                isPropertiesVisible = propertiesVisible
            )

            TabsPanel(
                projectManager = projectManager,
                onCloseRequested = onCloseTab
            )

            Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                if (toolsVisible) {
                    ToolsPanel(engine = engine, onImportImage = onImportImage)
                }

                if (propertiesVisible) {
                    GlassPanel(modifier = Modifier.width(if (propertiesCollapsed) 40.dp.scaled() else leftPanelWidth).fillMaxHeight()) {
                        Column {
                            PanelHeader(
                                title = if (propertiesCollapsed) "" else EditorStrings.observeString("panel.tools"),
                                isCollapsed = propertiesCollapsed,
                                onToggleCollapse = { engine.setPropertiesCollapsed(!propertiesCollapsed) },
                                onClose = { engine.setPropertiesVisible(false) }
                            )
                            if (!propertiesCollapsed) {
                                ToolPropertiesPanel(engine = engine)
                            }
                        }
                    }
                    if (!propertiesCollapsed) {
                        VerticalSplitter { delta -> 
                            leftPanelWidth = (leftPanelWidth + delta.dp).coerceIn(UiDimensions.MinSidePanelWidth.scaledNonReactive(), UiDimensions.MaxSidePanelWidth.scaledNonReactive())
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxHeight().background(EditorColors.canvasBackground)) {
                    DrawingCanvas(engine = engine, modifier = Modifier.fillMaxSize())
                }

                if (layersVisible || colorPickerVisible) {
                    val isRightSideCollapsed = (layersCollapsed || !layersVisible) && (colorPickerCollapsed || !colorPickerVisible)
                    
                    if (!isRightSideCollapsed) {
                        VerticalSplitter { delta -> 
                            rightPanelWidth = (rightPanelWidth - delta.dp).coerceIn(UiDimensions.MinSidePanelWidth.scaledNonReactive(), UiDimensions.MaxSidePanelWidth.scaledNonReactive())
                        }
                    }
                    
                    var rightPanelHeightPx by remember { mutableStateOf(1f) }
                    
                    GlassPanel(modifier = Modifier.width(if (isRightSideCollapsed) 40.dp.scaled() else rightPanelWidth).fillMaxHeight()) {
                        Column(
                            modifier = Modifier.fillMaxSize().onGloballyPositioned { 
                                if (it.size.height > 0) rightPanelHeightPx = it.size.height.toFloat() 
                            }
                        ) {
                            if (layersVisible) {
                                val layerWeight = when {
                                    !colorPickerVisible || colorPickerCollapsed -> 1f
                                    layersCollapsed -> 0.05f
                                    else -> rightPanelSplitRatio
                                }
                                Box(modifier = Modifier.weight(layerWeight, fill = !layersCollapsed)) {
                                    Column {
                                        PanelHeader(
                                            title = if (layersCollapsed && isRightSideCollapsed) "" else EditorStrings.observeString("panel.layers"),
                                            isCollapsed = layersCollapsed,
                                            onToggleCollapse = { engine.setLayersCollapsed(!layersCollapsed) },
                                            onClose = { engine.setLayersVisible(false) }
                                        )
                                        if (!layersCollapsed) {
                                            LayersPanel(engine = engine)
                                        }
                                    }
                                }
                            }
                            
                            if (layersVisible && colorPickerVisible && !layersCollapsed && !colorPickerCollapsed) {
                                HorizontalSplitter { delta ->
                                    val deltaRatio = delta / rightPanelHeightPx
                                    rightPanelSplitRatio = (rightPanelSplitRatio + deltaRatio).coerceIn(0.1f, 0.9f)
                                }
                            }
                            
                            if (colorPickerVisible) {
                                val colorWeight = when {
                                    !layersVisible || layersCollapsed -> 1f
                                    colorPickerCollapsed -> 0.05f
                                    else -> (1f - rightPanelSplitRatio)
                                }
                                Box(modifier = Modifier.weight(colorWeight, fill = !colorPickerCollapsed)) {
                                    Column {
                                        PanelHeader(
                                            title = if (colorPickerCollapsed && isRightSideCollapsed) "" else EditorStrings.observeString("panel.color"),
                                            isCollapsed = colorPickerCollapsed,
                                            onToggleCollapse = { engine.setColorPickerCollapsed(!colorPickerCollapsed) },
                                            onClose = { engine.setColorPickerVisible(false) }
                                        )
                                        if (!colorPickerCollapsed) {
                                            ColorPicker(engine = engine, modifier = Modifier.fillMaxSize())
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (timelineVisible) {
                if (!timelineCollapsed) {
                    HorizontalSplitter { delta -> 
                        timelineHeight = (timelineHeight - delta.dp).coerceIn(UiDimensions.MinTimelineHeight.scaledNonReactive(), UiDimensions.MaxTimelineHeight.scaledNonReactive())
                    }
                }
                
                Box(modifier = Modifier.fillMaxWidth().height(if (timelineCollapsed) 30.dp.scaled() else timelineHeight)) {
                    Column(Modifier.fillMaxSize()) {
                        val isGlass = LocalThemeType.current == ThemeType.GLASS
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(30.dp.scaled())
                                .background(if (isGlass) EditorColors.panelHeader.copy(alpha = 0.6f) else EditorColors.panelHeader)
                                .then(if (isGlass) Modifier.border(0.5.dp.scaled(), EditorColors.glassBorder) else Modifier),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            PanelStyledIconButton(
                                icon = if (timelineCollapsed) EditorIcons.iconKeyboardArrowUp else EditorIcons.iconKeyboardArrowDown,
                                onClick = { engine.setTimelineCollapsed(!timelineCollapsed) },
                                modifier = Modifier.padding(horizontal = 4.dp.scaled()),
                                tooltip = if (timelineCollapsed) EditorStrings.observeString("view.expand") else EditorStrings.observeString("view.collapse")
                            )
                            Text(EditorStrings.observeString("panel.timeline").uppercase(), style = EditorTypography.panelTitle())
                            Spacer(Modifier.weight(1f))
                            PanelStyledIconButton(
                                icon = EditorIcons.iconClose,
                                onClick = { engine.setTimelineVisible(false) },
                                modifier = Modifier.padding(horizontal = 4.dp.scaled()),
                                tooltip = EditorStrings.observeString("close")
                            )
                        }
                        if (!timelineCollapsed) {
                            TimelinePanel(engine = engine, modifier = Modifier.fillMaxSize())
                        }
                    }
                }
            } else {
                val isGlass = LocalThemeType.current == ThemeType.GLASS
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(UiDimensions.StatusBarHeight.scaled())
                        .background(if (isGlass) EditorColors.panelHeader.copy(alpha = 0.6f) else EditorColors.panelHeader)
                        .then(if (isGlass) Modifier.border(0.5.dp.scaled(), EditorColors.glassBorder) else Modifier)
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
private fun PanelHeader(
    title: String, 
    isCollapsed: Boolean = false,
    onToggleCollapse: (() -> Unit)? = null,
    onClose: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().background(EditorColors.panelHeader.copy(alpha = 0.5f)).padding(horizontal = 4.dp.scaled(), vertical = 4.dp.scaled()),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (onToggleCollapse != null) {
                PanelStyledIconButton(
                    icon = if (isCollapsed) EditorIcons.iconKeyboardArrowDown else EditorIcons.iconKeyboardArrowUp,
                    onClick = onToggleCollapse,
                    tooltip = if (isCollapsed) EditorStrings.observeString("view.expand") else EditorStrings.observeString("view.collapse")
                )
            }
            if (title.isNotEmpty()) {
                Spacer(Modifier.width(4.dp.scaled()))
                Text(title.uppercase(), style = EditorTypography.panelTitle(), maxLines = 1)
            }
        }
        PanelStyledIconButton(
            icon = EditorIcons.iconClose,
            onClick = onClose,
            tooltip = EditorStrings.observeString("close")
        )
    }
}

@Composable
private fun PanelStyledIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconSize: androidx.compose.ui.unit.Dp = 14.dp,
    tooltip: String = ""
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    Box(
        modifier = modifier
            .size(24.dp.scaled())
            .clip(RoundedCornerShape(4.dp.scaled()))
            .background(if (isHovered) EditorColors.hover else Color.Transparent)
            .pointerHoverIcon(PointerIcon.Hand)
            .tooltipAnchor(tooltip)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon, null,
            tint = if (isHovered) EditorColors.textPrimary else EditorColors.textMuted,
            modifier = Modifier.size(iconSize.scaled())
        )
    }
}

@Composable
private fun TopBarIconButton(
    icon: ImageVector, 
    enabled: Boolean = true, 
    tooltip: String = "", 
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    Box(
        modifier = Modifier
            .size(UiDimensions.IconButtonSize.scaled())
            .clip(RoundedCornerShape(4.dp.scaled()))
            .background(if (enabled && isHovered) EditorColors.hover else Color.Transparent)
            .tooltipAnchor(tooltip, enabled = enabled)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon, null, 
            tint = if (!enabled) EditorColors.textMuted 
                   else if (isHovered) EditorColors.accent 
                   else EditorColors.textPrimary.copy(alpha = 0.8f), 
            modifier = Modifier.size(UiDimensions.IconSize.scaled())
        )
    }
}

private sealed class MenuItemData {
    data class Action(val label: String, val onClick: () -> Unit) : MenuItemData()
    data class SubMenu(val label: String, val onClick: () -> Unit, val items: List<Action>) : MenuItemData()
    object Divider : MenuItemData()
}

@Composable
private fun EditorMenuBar(
    engine: AnimationEngine,
    projectName: String,
    zoom: Float,
    rotation: Float,
    isPlaying: Boolean,
    canUndo: Boolean,
    canRedo: Boolean,
    onNew: () -> Unit,
    onSave: () -> Unit,
    onLoad: () -> Unit,
    onExport: (String) -> Unit,
    onSettings: () -> Unit,
    isCompact: Boolean,
    isToolsVisible: Boolean,
    isLayersVisible: Boolean,
    isColorPickerVisible: Boolean,
    isTimelineVisible: Boolean,
    isPropertiesVisible: Boolean
) {
    val theme = LocalThemeType.current
    val isGlass = theme == ThemeType.GLASS
    val barColor = if (isGlass) EditorColors.panelHeader.copy(alpha = 0.7f) else EditorColors.panelHeader

    val exportItems = remember {
        listOf(
            MenuItemData.Action("PNG") { onExport("png") },
            MenuItemData.Action("JPG") { onExport("jpg") },
            MenuItemData.Action("WEBP") { onExport("webp") },
            MenuItemData.Action("GIF") { onExport("gif") },
            MenuItemData.Action("APNG") { onExport("apng") },
            MenuItemData.Action("MP4") { onExport("mp4") },
            MenuItemData.Action("WEBM") { onExport("webm") },
            MenuItemData.Action("MOV") { onExport("mov") },
            MenuItemData.Action("MKV") { onExport("mkv") },
            MenuItemData.Action("AVI") { onExport("avi") }
        )
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(UiDimensions.TopBarHeight.scaled())
            .then(if (isGlass) Modifier.border(0.5.dp.scaled(), EditorColors.glassBorder) else Modifier),
        color = barColor,
        elevation = if (isGlass) 0.dp.scaled() else 1.dp.scaled()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = UiDimensions.PaddingMedium.scaled()), 
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(EditorIcons.iconAppIcon, null, tint = EditorColors.accent, modifier = Modifier.size(UiDimensions.IconSize.scaled()))
            
            Spacer(Modifier.width(UiDimensions.PaddingMedium.scaled()))

            TopBarIconButton(EditorIcons.iconNewProject, tooltip = EditorStrings.observeString("file.new")) { onNew() }
            TopBarIconButton(EditorIcons.iconFolderOpen, tooltip = EditorStrings.observeString("file.open")) { onLoad() }
            TopBarIconButton(EditorIcons.iconSave, tooltip = EditorStrings.observeString("file.save")) { onSave() }
            
            TopBarIconButton(EditorIcons.iconExport, tooltip = EditorStrings.observeString("file.export")) { onExport("png") }

            Spacer(Modifier.width(UiDimensions.PaddingMedium.scaled()))
            VerticalDivider()
            Spacer(Modifier.width(UiDimensions.PaddingMedium.scaled()))

            DropdownMenuButton(title = EditorStrings.observeString("menu.file"), items = listOf(
                MenuItemData.Action(EditorStrings.observeString("file.new"), onNew),
                MenuItemData.Action(EditorStrings.observeString("file.open"), onLoad),
                MenuItemData.Divider,
                MenuItemData.Action(EditorStrings.observeString("file.save"), onSave),
                MenuItemData.Divider,
                MenuItemData.SubMenu(
                    label = EditorStrings.observeString("file.export"),
                    onClick = { onExport("png") },
                    items = exportItems
                )
            ))

            DropdownMenuButton(title = EditorStrings.observeString("menu.view"), items = listOf(
                MenuItemData.Action(EditorStrings.observeString("view.resetZoom")) { engine.setZoom(1f) },
                MenuItemData.Divider,
                MenuItemData.Action(if (isToolsVisible) EditorStrings.observeString("view.hideTools") else EditorStrings.observeString("view.showTools")) { engine.toggleTools() },
                MenuItemData.Action(if (isLayersVisible) EditorStrings.observeString("view.hideLayers") else EditorStrings.observeString("view.showLayers")) { engine.toggleLayers() },
                MenuItemData.Action(if (isColorPickerVisible) EditorStrings.observeString("view.hideColor") else EditorStrings.observeString("view.showColor")) { engine.toggleColorPicker() },
                MenuItemData.Action(if (isTimelineVisible) EditorStrings.observeString("view.hideTimeline") else EditorStrings.observeString("view.showTimeline")) { engine.toggleTimeline() },
                MenuItemData.Action(if (isPropertiesVisible) EditorStrings.observeString("view.hideProperties") else EditorStrings.observeString("view.showProperties")) { engine.toggleProperties() }
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
                
                // Значение маштаба без обводки и рипла
                val zoomSource = remember { MutableInteractionSource() }
                val zoomHovered by zoomSource.collectIsHoveredAsState()
                Box(
                    modifier = Modifier
                        .width(45.dp.scaled())
                        .fillMaxHeight()
                        .clickable(interactionSource = zoomSource, indication = null) { engine.setZoom(1f) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "${(zoom * 100).toInt()}%", 
                        style = EditorTypography.mono(), 
                        color = if (zoomHovered) EditorColors.accent else EditorColors.textSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }

                TopBarIconButton(EditorIcons.iconZoomIn, tooltip = EditorStrings.observeString("view.zoomIn")) { engine.setZoom((zoom * 1.25f).coerceIn(0.1f, 20f)) }

                Spacer(Modifier.width(UiDimensions.PaddingMedium.scaled()))
                VerticalDivider()
                Spacer(Modifier.width(UiDimensions.PaddingMedium.scaled()))

                TopBarIconButton(EditorIcons.iconRotateLeft, tooltip = EditorStrings.observeString("view.rotateLeft")) { engine.setRotation(rotation - 15f) }
                
                // Значение поворота без обводки и рипла
                val rotSource = remember { MutableInteractionSource() }
                val rotHovered by rotSource.collectIsHoveredAsState()
                Box(
                    modifier = Modifier
                        .width(40.dp.scaled())
                        .fillMaxHeight()
                        .clickable(interactionSource = rotSource, indication = null) { engine.setRotation(0f); engine.setPanOffset(Offset.Zero) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "${rotation.toInt()}°", 
                        style = EditorTypography.mono(), 
                        color = if (rotHovered) EditorColors.accent else EditorColors.textSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }

                TopBarIconButton(EditorIcons.iconRotateRight, tooltip = EditorStrings.observeString("view.rotateRight")) { engine.setRotation(rotation + 15f) }

                Spacer(Modifier.width(UiDimensions.PaddingMedium.scaled()))
                VerticalDivider()
                Spacer(Modifier.width(UiDimensions.PaddingMedium.scaled()))

                TopBarIconButton(if (isPlaying) EditorIcons.iconPause else EditorIcons.iconPlayArrow, tooltip = if (isPlaying) EditorStrings.observeString("pause") else EditorStrings.observeString("play")) { 
                    engine.togglePlayback()
                }
            }

            Spacer(Modifier.weight(1f))
            
            if (!isCompact) Text(projectName, style = EditorTypography.panelTitle(), color = EditorColors.textMuted, maxLines = 1)

            Spacer(Modifier.weight(1f))

            TopBarIconButton(EditorIcons.iconSettings, tooltip = EditorStrings.observeString("settings.title")) { onSettings() }
        }
    }
}

@Composable
private fun VerticalDivider() {
    Box(modifier = Modifier.width(1.dp.scaled()).height(16.dp.scaled()).background(EditorColors.divider))
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun DropdownMenuButton(
    title: String? = null,
    icon: ImageVector? = null,
    tooltip: String = "",
    items: List<MenuItemData?>,
    onMainClick: (() -> Unit)? = null
) {
    var expanded by remember { mutableStateOf(false) }
    var activeSubMenuLabel by remember { mutableStateOf<String?>(null) }
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    
    Box(
        modifier = Modifier
    ) {
        if (icon != null) {
            TopBarIconButton(icon, tooltip = tooltip) { 
                expanded = !expanded
                onMainClick?.invoke()
            }
        } else {
            Box(
                modifier = Modifier
                    .height(28.dp.scaled())
                    .clip(RoundedCornerShape(4.dp.scaled()))
                    .background(if (isHovered) EditorColors.hover else Color.Transparent)
                    .clickable(interactionSource = interactionSource, indication = null) { expanded = !expanded }
                    .padding(horizontal = 8.dp.scaled()),
                contentAlignment = Alignment.Center
            ) { 
                Text(title ?: "", style = EditorTypography.menu(), color = if (isHovered) EditorColors.accent else EditorColors.textPrimary)
            }
        }

        DropdownMenu(
            expanded = expanded, 
            onDismissRequest = { 
                expanded = false 
                activeSubMenuLabel = null
            }, 
            modifier = Modifier
                .width(IntrinsicSize.Max)
                .background(EditorColors.surface)
                .border(1.dp.scaled(), EditorColors.divider)
        ) { 
            items.forEach { item -> 
                when (item) {
                    is MenuItemData.Divider -> Divider(color = EditorColors.divider)
                    is MenuItemData.Action -> {
                        DropdownMenuItem(
                            onClick = { 
                                expanded = false
                                activeSubMenuLabel = null
                                item.onClick() 
                            }, 
                            modifier = Modifier
                                .height(32.dp.scaled())
                                .width(IntrinsicSize.Max)
                        ) { 
                            Text(item.label, style = EditorTypography.menu()) 
                        }
                    }
                    is MenuItemData.SubMenu -> {
                        DropdownMenuItem(
                            onClick = {
                                expanded = false
                                activeSubMenuLabel = null
                                item.onClick()
                            },
                            modifier = Modifier
                                .height(32.dp.scaled())
                                .width(IntrinsicSize.Max)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(item.label, style = EditorTypography.menu())
                                Icon(
                                    EditorIcons.iconKeyboardArrowRight,
                                    null,
                                    modifier = Modifier
                                        .size(16.dp.scaled())
                                        .clickable { activeSubMenuLabel = if (activeSubMenuLabel == item.label) null else item.label },
                                    tint = EditorColors.textMuted
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = activeSubMenuLabel == item.label,
                            onDismissRequest = { activeSubMenuLabel = null },
                            offset = DpOffset(x = 120.dp.scaled(), y = (-32).dp.scaled()),
                            modifier = Modifier
                                .width(IntrinsicSize.Max)
                                .background(EditorColors.surface)
                                .border(1.dp.scaled(), EditorColors.divider)
                        ) {
                            item.items.forEach { subItem ->
                                DropdownMenuItem(
                                    onClick = {
                                        activeSubMenuLabel = null
                                        expanded = false
                                        subItem.onClick()
                                    },
                                    modifier = Modifier
                                        .height(32.dp.scaled())
                                        .width(IntrinsicSize.Max)
                                ) {
                                    Text(subItem.label, style = EditorTypography.menu())
                                }
                            }
                        }
                    }
                    null -> {}
                }
            } 
        }
    }
}

@Composable
private fun EditorStatusBar(projectName: String, size: String, frame: Int, total: Int, fps: Int, playing: Boolean) {
    val theme = LocalThemeType.current
    val isGlass = theme == ThemeType.GLASS
    val barColor = if (isGlass) EditorColors.panelHeader.copy(alpha = 0.7f) else EditorColors.panelHeader

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(UiDimensions.StatusBarHeight.scaled())
            .then(if (isGlass) Modifier.border(0.5.dp.scaled(), EditorColors.glassBorder) else Modifier),
        color = barColor,
        elevation = if (isGlass) 0.dp.scaled() else 1.dp.scaled()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = UiDimensions.PaddingMedium.scaled()), 
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatusItem(projectName, EditorIcons.iconAppIcon)
            StatusItem(size, null)
            StatusItem("${EditorStrings.observeString("status.frame")} $frame/$total", EditorIcons.iconTimeline)
            StatusItem("${EditorStrings.observeString("status.fps")}: $fps", null)
            Spacer(Modifier.weight(1f))
            Box(modifier = Modifier.size(6.dp.scaled()).clip(RoundedCornerShape(3.dp.scaled())).background(if (playing) EditorColors.accentGreen else EditorColors.accentRed))
        }
    }
}

@Composable
private fun StatusItem(text: String, icon: ImageVector?) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 12.dp.scaled())) {
        if (icon != null) { Icon(icon, null, tint = EditorColors.textMuted, modifier = Modifier.size(10.dp.scaled())); Spacer(Modifier.width(4.dp.scaled())) }
        Text(text, style = EditorTypography.statusBar(), maxLines = 1)
    }
}