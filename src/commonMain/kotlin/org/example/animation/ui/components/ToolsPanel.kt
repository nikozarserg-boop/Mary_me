package org.example.animation.ui.components

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
import org.example.animation.ui.theme.EditorColors
import org.example.animation.ui.theme.EditorIcons
import org.example.animation.ui.theme.EditorShapes

@Composable
fun ToolsPanel(engine: AnimationEngine) {
    val currentTool by engine.currentTool.collectAsState()
    val canUndo by engine.canUndo.collectAsState()
    val canRedo by engine.canRedo.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(52.dp)
            .background(EditorColors.panelBackground)
            .border(1.dp, EditorColors.dividerColor)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Секция Рисования
        ToolGroup {
            ToolButton(EditorIcons.iconBrush, EditorStrings.observeString("tool.brush"), currentTool == ToolType.BRUSH) { engine.setTool(ToolType.BRUSH) }
            ToolButton(EditorIcons.iconPencil, EditorStrings.observeString("tool.pencil"), currentTool == ToolType.PENCIL) { engine.setTool(ToolType.PENCIL) }
            ToolButton(EditorIcons.iconEraser, EditorStrings.observeString("tool.eraser"), currentTool == ToolType.ERASER) { engine.setTool(ToolType.ERASER) }
        }
        
        Spacer(Modifier.height(8.dp))
        
        // Секция Фигур
        ToolGroup {
            ToolButton(EditorIcons.iconLine, EditorStrings.observeString("tool.line"), currentTool == ToolType.LINE) { engine.setTool(ToolType.LINE) }
            ToolButton(EditorIcons.iconRectangle, EditorStrings.observeString("tool.rectangle"), currentTool == ToolType.RECTANGLE) { engine.setTool(ToolType.RECTANGLE) }
            ToolButton(EditorIcons.iconEllipse, EditorStrings.observeString("tool.ellipse"), currentTool == ToolType.ELLIPSE) { engine.setTool(ToolType.ELLIPSE) }
        }

        Spacer(Modifier.height(8.dp))

        // Секция Вспомогательных
        ToolGroup {
            ToolButton(EditorIcons.iconFill, EditorStrings.observeString("tool.fill"), currentTool == ToolType.FILL) { engine.setTool(ToolType.FILL) }
            ToolButton(EditorIcons.iconEyedropper, EditorStrings.observeString("tool.eyedropper"), currentTool == ToolType.EYEDROPPER) { engine.setTool(ToolType.EYEDROPPER) }
            ToolButton(EditorIcons.iconSelect, EditorStrings.observeString("tool.select"), currentTool == ToolType.SELECT) { engine.setTool(ToolType.SELECT) }
            ToolButton(EditorIcons.iconMove, EditorStrings.observeString("tool.move"), currentTool == ToolType.MOVE) { engine.setTool(ToolType.MOVE) }
        }

        Spacer(Modifier.weight(1f))
        
        // Секция Истории (Undo/Redo)
        ToolGroup {
            ToolButton(EditorIcons.iconUndo, EditorStrings.observeString("edit.undo"), false, enabled = canUndo) { engine.undo() }
            ToolButton(EditorIcons.iconRedo, EditorStrings.observeString("edit.redo"), false, enabled = canRedo) { engine.redo() }
        }
        
        Spacer(Modifier.height(8.dp))
        
        // Кнопка очистки кадра
        ToolButton(EditorIcons.iconClearAll, EditorStrings.observeString("edit.clearFrame"), false) { engine.clearFrame() }
    }
}

@Composable
private fun ToolGroup(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .padding(horizontal = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(EditorColors.darkSurfaceVariant.copy(alpha = 0.3f))
            .padding(vertical = 4.dp),
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
    val isPressed by interactionSource.collectIsPressedAsState()
    
    // Статический профессиональный UI (согласно TODO удалены анимации)
    val backgroundColor = when {
        !enabled -> Color.Transparent
        isSelected -> EditorColors.selectionColor
        isPressed -> EditorColors.selectionColor.copy(alpha = 0.8f)
        isHovered -> EditorColors.hoverColor
        else -> Color.Transparent
    }

    var showTooltip by remember { mutableStateOf(false) }

    LaunchedEffect(isHovered) {
        if (isHovered && enabled) {
            delay(600)
            showTooltip = true
        } else {
            showTooltip = false
        }
    }

    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 2.dp)) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(EditorShapes.smallRounded)
                .background(backgroundColor)
                .pointerHoverIcon(if (enabled) PointerIcon.Hand else PointerIcon.Default)
                .clickable(
                    enabled = enabled,
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                null,
                tint = when {
                    !enabled -> EditorColors.textMuted
                    isSelected -> Color.White
                    else -> EditorColors.textSecondary
                },
                modifier = Modifier.size(20.dp)
            )
        }
        
        if (showTooltip) {
            Surface(
                modifier = Modifier.offset(x = 54.dp).zIndex(1000f),
                color = EditorColors.darkSurfaceLight,
                shape = RoundedCornerShape(4.dp),
                elevation = 8.dp,
                border = BorderStroke(1.dp, EditorColors.dividerColor)
            ) {
                Text(
                    tooltip, 
                    color = Color.White, 
                    fontSize = 11.sp, 
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                    maxLines = 1
                )
            }
        }
    }
}
