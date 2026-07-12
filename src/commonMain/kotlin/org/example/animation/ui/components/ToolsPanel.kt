package org.example.animation.ui.components

import androidx.compose.animation.*
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import org.example.animation.engine.AnimationEngine
import org.example.animation.model.ToolType
import org.example.animation.localization.EditorStrings
import org.example.animation.ui.theme.*

@Composable
fun ToolsPanel(engine: AnimationEngine, onImportImage: () -> Unit = {}) {
    val currentTool by engine.currentTool.collectAsState()
    val isVisible by engine.isToolsVisible.collectAsState()
    val isCollapsed by engine.isToolsCollapsed.collectAsState()
    
    if (!isVisible) return

    val theme = LocalThemeType.current
    val isGlass = theme == ThemeType.GLASS
    val background = if (isGlass) EditorColors.glassBackground else EditorColors.panelBackground
    val shape = if (isGlass) RoundedCornerShape(10.dp.scaled()) else RoundedCornerShape(0.dp)

    Box(
        modifier = Modifier
            .fillMaxHeight()
            .width(if (isCollapsed) 32.dp.scaled() else UiDimensions.ToolBarWidth.scaled())
            .border(1.dp.scaled(), if (isGlass) EditorColors.glassBorder else EditorColors.divider)
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(shape)
                .background(background)
                .then(
                    if (isGlass) Modifier.background(
                        Brush.radialGradient(
                            colors = listOf(EditorColors.glassSheen, Color.Transparent),
                            center = Offset(0.3f, 0.1f),
                            radius = 1.0f
                        )
                    ) else Modifier
                )
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = UiDimensions.PaddingSmall.scaled()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ToolCollapseButton(
                icon = if (isCollapsed) EditorIcons.iconArrowForward else EditorIcons.iconArrowBack,
                onClick = { engine.setToolsCollapsed(!isCollapsed) }
            )

            if (!isCollapsed) {
                Divider(color = EditorColors.divider, modifier = Modifier.padding(vertical = 4.dp.scaled()))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    ToolGroup {
                        ToolButton(EditorIcons.iconBrush, EditorStrings.observeString("tool.brush"), currentTool == ToolType.BRUSH) { engine.setTool(ToolType.BRUSH) }
                        ToolButton(EditorIcons.iconPencil, EditorStrings.observeString("tool.pencil"), currentTool == ToolType.PENCIL) { engine.setTool(ToolType.PENCIL) }
                        ToolButton(EditorIcons.iconEraser, EditorStrings.observeString("tool.eraser"), currentTool == ToolType.ERASER) { engine.setTool(ToolType.ERASER) }
                    }

                    Spacer(Modifier.height(UiDimensions.PaddingSmall.scaled()))

                    ToolGroup {
                        ToolButton(EditorIcons.iconLine, EditorStrings.observeString("tool.line"), currentTool == ToolType.LINE) { engine.setTool(ToolType.LINE) }
                        ToolButton(EditorIcons.iconRectangle, EditorStrings.observeString("tool.rectangle"), currentTool == ToolType.RECTANGLE) { engine.setTool(ToolType.RECTANGLE) }
                        ToolButton(EditorIcons.iconEllipse, EditorStrings.observeString("tool.ellipse"), currentTool == ToolType.ELLIPSE) { engine.setTool(ToolType.ELLIPSE) }
                    }

                    Spacer(Modifier.height(UiDimensions.PaddingSmall.scaled()))

                    ToolGroup {
                        ToolButton(EditorIcons.iconFill, EditorStrings.observeString("tool.fill"), currentTool == ToolType.FILL) { engine.setTool(ToolType.FILL) }
                        ToolButton(EditorIcons.iconEyedropper, EditorStrings.observeString("tool.eyedropper"), currentTool == ToolType.EYEDROPPER) { engine.setTool(ToolType.EYEDROPPER) }
                        ToolButton(EditorIcons.iconSelect, EditorStrings.observeString("tool.select"), currentTool == ToolType.SELECT) { engine.setTool(ToolType.SELECT) }
                        ToolButton(EditorIcons.iconMove, EditorStrings.observeString("tool.move"), currentTool == ToolType.MOVE) { engine.setTool(ToolType.MOVE) }
                    }

                    Spacer(Modifier.height(UiDimensions.PaddingSmall.scaled()))

                    ToolGroup {
                        ToolButton(EditorIcons.iconPngIcon, EditorStrings.observeString("import.image"), false) { onImportImage() }
                    }

                    Spacer(Modifier.height(UiDimensions.PaddingSmall.scaled()))

                    ToolButton(EditorIcons.iconDeleteSweep, EditorStrings.observeString("edit.clearFrame"), false) { engine.clearFrame() }

                    Spacer(Modifier.height(UiDimensions.PaddingSmall.scaled()))

                    ToolButton(EditorIcons.iconClose, EditorStrings.observeString("cancel"), false) { engine.setToolsVisible(false) }
                }
            }
        }
    }
}

@Composable
private fun ToolCollapseButton(icon: ImageVector, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isPressed by interactionSource.collectIsPressedAsState()

    Box(
        modifier = Modifier
            .size(24.dp.scaled())
            .clip(RoundedCornerShape(4.dp.scaled()))
            .background(if (isHovered) EditorColors.hover else Color.Transparent)
            .border(
                width = 1.dp.scaled(),
                color = if (isPressed) EditorColors.accent 
                        else if (isHovered) EditorColors.accent.copy(alpha = 0.6f) 
                        else Color.Transparent,
                shape = RoundedCornerShape(4.dp.scaled())
            )
            .pointerHoverIcon(PointerIcon.Hand)
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
            modifier = Modifier.size(16.dp.scaled())
        )
    }
}

@Composable
private fun ToolGroup(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .padding(horizontal = 1.dp.scaled())
            .clip(RoundedCornerShape(3.dp.scaled()))
            .background(Color.White.copy(alpha = 0.02f))
            .padding(vertical = 1.dp.scaled()),
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content
    )
}

@Composable
private fun ToolButton(
    icon: ImageVector, 
    tooltip: String, 
    isSelected: Boolean, 
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    
    val backgroundColor = when {
        !enabled -> Color.Transparent
        isSelected -> EditorColors.accent.copy(alpha = 0.7f)
        isHovered -> EditorColors.hover
        else -> Color.Transparent
    }

    var showTooltip by remember { mutableStateOf(false) }
    LaunchedEffect(isHovered) {
        if (isHovered && enabled) { delay(400); showTooltip = true } else { showTooltip = false }
    }

    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 1.dp.scaled())) {
        Box(
            modifier = Modifier
                .size(UiDimensions.IconButtonSize.scaled())
                .clip(RoundedCornerShape(4.dp.scaled()))
                .background(backgroundColor)
                .border(
                    width = 1.dp.scaled(),
                    color = if (isHovered) EditorColors.accent.copy(alpha = 0.5f) else Color.Transparent,
                    shape = RoundedCornerShape(4.dp.scaled())
                )
                .pointerHoverIcon(if (enabled) PointerIcon.Hand else PointerIcon.Default)
                .clickable(enabled = enabled, interactionSource = interactionSource, indication = null, onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon, 
                null, 
                tint = if (isSelected) Color.White else EditorColors.textPrimary.copy(alpha = 0.6f), 
                modifier = Modifier.size(UiDimensions.IconSize.scaled())
            )
        }
        
        if (showTooltip) {
            Box(
                modifier = Modifier.offset(x = (UiDimensions.ToolBarWidth + 4.dp).scaled(), y = 0.dp).zIndex(1000f)
                    .clip(RoundedCornerShape(4.dp.scaled()))
                    .background(EditorColors.surface)
                    .border(1.dp.scaled(), EditorColors.divider, RoundedCornerShape(4.dp.scaled()))
                    .padding(horizontal = 6.dp.scaled(), vertical = 3.dp.scaled())
            ) {
                Text(tooltip, style = EditorTypography.toolText(), color = EditorColors.textPrimary, maxLines = 1)
            }
        }
    }
}