package org.example.animation.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.compose.ui.unit.sp
import org.example.animation.engine.AnimationEngine
import org.example.animation.model.ToolType
import org.example.animation.ui.theme.EditorColors
import org.example.animation.ui.theme.EditorIcons

/**
 * Панель инструментов с тултипами при наведении
 */
@Composable
fun ToolsPanel(
    engine: AnimationEngine,
    modifier: Modifier = Modifier
) {
    val currentTool by engine.currentTool.collectAsState()
    val brushSize by engine.brushSize.collectAsState()
    val onionSkinEnabled by engine.onionSkinEnabled.collectAsState()
    val canUndo by engine.canUndo.collectAsState()
    val canRedo by engine.canRedo.collectAsState()

    Surface(
        modifier = modifier.width(48.dp),
        color = EditorColors.panelBackground,
        elevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // РИСОВАНИЕ
            ToolBtn(EditorIcons.iconPen, "Перо (P)", currentTool == ToolType.PEN) { engine.setCurrentTool(ToolType.PEN) }
            Spacer(Modifier.height(2.dp))
            ToolBtn(EditorIcons.iconPencil, "Карандаш (N)", currentTool == ToolType.PENCIL) { engine.setCurrentTool(ToolType.PENCIL) }
            Spacer(Modifier.height(2.dp))
            ToolBtn(EditorIcons.iconBrush, "Кисть (B)", currentTool == ToolType.BRUSH) { engine.setCurrentTool(ToolType.BRUSH) }
            Spacer(Modifier.height(2.dp))
            ToolBtn(EditorIcons.iconEraser, "Ластик (E)", currentTool == ToolType.ERASER) { engine.setCurrentTool(ToolType.ERASER) }

            Spacer(Modifier.height(4.dp))
            Divider(color = EditorColors.dividerColor, modifier = Modifier.width(32.dp))
            Spacer(Modifier.height(4.dp))

            // ФИГУРЫ
            ToolBtn(EditorIcons.iconLine, "Линия (L)", currentTool == ToolType.LINE) { engine.setCurrentTool(ToolType.LINE) }
            Spacer(Modifier.height(2.dp))
            ToolBtn(EditorIcons.iconRectangle, "Прямоугольник (R)", currentTool == ToolType.RECTANGLE) { engine.setCurrentTool(ToolType.RECTANGLE) }
            Spacer(Modifier.height(2.dp))
            ToolBtn(EditorIcons.iconEllipse, "Эллипс (O)", currentTool == ToolType.ELLIPSE) { engine.setCurrentTool(ToolType.ELLIPSE) }
            Spacer(Modifier.height(2.dp))
            ToolBtn(EditorIcons.iconFill, "Заливка (G)", currentTool == ToolType.FILL) { engine.setCurrentTool(ToolType.FILL) }

            Spacer(Modifier.height(4.dp))
            Divider(color = EditorColors.dividerColor, modifier = Modifier.width(32.dp))
            Spacer(Modifier.height(4.dp))

            // ДОПОЛНИТЕЛЬНЫЕ
            ToolBtn(EditorIcons.iconEyedropper, "Пипетка (I)", currentTool == ToolType.EYEDROPPER) { engine.setCurrentTool(ToolType.EYEDROPPER) }
            Spacer(Modifier.height(2.dp))
            ToolBtn(EditorIcons.iconSelect, "Выделение (V)", currentTool == ToolType.SELECT) { engine.setCurrentTool(ToolType.SELECT) }
            Spacer(Modifier.height(2.dp))
            ToolBtn(EditorIcons.iconMove, "Перемещение (H)", currentTool == ToolType.MOVE) { engine.setCurrentTool(ToolType.MOVE) }

            Spacer(Modifier.weight(1f))

            Divider(color = EditorColors.dividerColor, modifier = Modifier.width(32.dp))
            Spacer(Modifier.height(4.dp))

            // ОТМЕНА/ПОВТОР
            ToolBtn(EditorIcons.iconUndo, "Отменить (Ctrl+Z)", canUndo) { engine.undo() }
            Spacer(Modifier.height(2.dp))
            ToolBtn(EditorIcons.iconRedo, "Повторить (Ctrl+Y)", canRedo) { engine.redo() }

            Spacer(Modifier.height(4.dp))

            // ONION SKIN
            ToolBtn(EditorIcons.iconOnionSkin, "Onion Skin", onionSkinEnabled) { engine.setOnionSkinEnabled(!onionSkinEnabled) }

            Spacer(Modifier.height(4.dp))

            // ЗУМ
            ToolBtn(EditorIcons.iconZoomOut, "Уменьшить (Ctrl+-)", false) { engine.setZoom((engine.zoom.value * 0.8f).coerceIn(0.1f, 10f)) }
            Spacer(Modifier.height(2.dp))
            ToolBtn(EditorIcons.iconZoomIn, "Увеличить (Ctrl++)", false) { engine.setZoom((engine.zoom.value * 1.25f).coerceIn(0.1f, 10f)) }
        }
    }
}

@Composable
private fun ToolBtn(icon: ImageVector, tooltip: String, isActive: Boolean, onClick: () -> Unit) {
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
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    when {
                        isActive -> EditorColors.accentBlue.copy(alpha = 0.25f)
                        isHovered -> EditorColors.darkSurfaceVariant
                        else -> Color.Transparent
                    }
                )
                .hoverable(interactionSource)
                .clickable(indication = null, interactionSource = interactionSource) { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, tooltip, tint = if (isActive) EditorColors.accentBlue else EditorColors.textSecondary, modifier = Modifier.size(20.dp))
        }

        if (showTooltip) {
            Box(
                modifier = Modifier
                    .offset(x = 44.dp, y = 2.dp)
                    .zIndex(600f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(EditorColors.darkSurfaceVariant.copy(alpha = 0.98f))
                    .border(0.5.dp, EditorColors.dividerColor, RoundedCornerShape(6.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(text = tooltip, color = EditorColors.textPrimary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}
