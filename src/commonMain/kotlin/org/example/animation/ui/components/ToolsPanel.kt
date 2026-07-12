package org.example.animation.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import org.example.animation.engine.AnimationEngine
import org.example.animation.model.ToolType
import org.example.animation.localization.EditorStrings
import org.example.animation.ui.theme.*

@Composable
fun ToolsPanel(engine: AnimationEngine) {
    val currentTool by engine.currentTool.collectAsState()
    val isVisible by engine.isToolsVisible.collectAsState()
    
    if (!isVisible) return

    val theme = LocalThemeType.current
    val background = if (theme == ThemeType.GLASS) EditorColors.glassBackground else EditorColors.panelBackground

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(UiDimensions.ToolBarWidth.scaled())
            .background(background)
            .border(if (theme == ThemeType.GLASS) 0.dp else 1.dp.scaled(), EditorColors.divider)
            .padding(vertical = UiDimensions.PaddingSmall.scaled()),
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

        Spacer(Modifier.weight(1f))
        
        ToolButton(EditorIcons.iconDeleteSweep, EditorStrings.observeString("edit.clearFrame"), false) { engine.clearFrame() }
        
        Spacer(Modifier.height(UiDimensions.PaddingSmall.scaled()))
        
        ToolButton(EditorIcons.iconClose, EditorStrings.observeString("cancel"), false) { engine.setToolsVisible(false) }
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
    
    val scale by animateFloatAsState(
        targetValue = if (isHovered && enabled) 1.15f else 1.0f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 300f)
    )

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
                .graphicsLayer(scaleX = scale, scaleY = scale)
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
            Surface(
                modifier = Modifier.offset(x = (UiDimensions.ToolBarWidth + 4.dp).scaled()).zIndex(1000f),
                color = EditorColors.surface,
                shape = RoundedCornerShape(4.dp.scaled()),
                elevation = 4.dp.scaled(),
                border = BorderStroke(1.dp.scaled(), EditorColors.divider)
            ) {
                Text(
                    tooltip, 
                    style = EditorTypography.toolText(),
                    color = EditorColors.textPrimary,
                    modifier = Modifier.padding(horizontal = 6.dp.scaled(), vertical = 3.dp.scaled()), 
                    maxLines = 1
                )
            }
        }
    }
}
