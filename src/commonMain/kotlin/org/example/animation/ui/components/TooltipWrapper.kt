package org.example.animation.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import org.example.animation.ui.theme.EditorColors

/**
 * Кнопка-инструмент с тултипом при наведении
 */
@Composable
fun TooltipToolButton(
    icon: ImageVector,
    tooltip: String,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    var showTooltip by remember { mutableStateOf(false) }

    LaunchedEffect(isHovered) {
        if (isHovered) {
            delay(600)
            showTooltip = true
        } else {
            showTooltip = false
        }
    }

    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(if (isActive) EditorColors.accentBlue.copy(alpha = 0.3f) else Color.Transparent)
                .hoverable(interactionSource)
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, tooltip, tint = if (isActive) EditorColors.accentBlue else EditorColors.textSecondary, modifier = Modifier.size(20.dp))
        }

        if (showTooltip) {
            Box(
                modifier = Modifier.offset(x = 42.dp, y = 0.dp).zIndex(500f)
                    .clip(RoundedCornerShape(4.dp))
                    .background(EditorColors.darkSurfaceVariant.copy(alpha = 0.98f))
                    .border(0.5.dp, EditorColors.dividerColor, RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(tooltip, color = EditorColors.textPrimary, fontSize = 11.sp, fontWeight = FontWeight.Normal)
            }
        }
    }
}

/**
 * Маленькая кнопка с тултипом
 */
@Composable
fun TooltipSmallButton(
    icon: ImageVector,
    tooltip: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = EditorColors.textSecondary,
    bgColor: Color = EditorColors.buttonColor
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    var showTooltip by remember { mutableStateOf(false) }

    LaunchedEffect(isHovered) {
        if (isHovered) {
            delay(600)
            showTooltip = true
        } else {
            showTooltip = false
        }
    }

    Box(modifier = modifier) {
        Box(
            modifier = Modifier.size(24.dp).clip(RoundedCornerShape(3.dp)).background(bgColor)
                .hoverable(interactionSource).clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, tooltip, tint = tint, modifier = Modifier.size(14.dp))
        }

        if (showTooltip) {
            Box(
                modifier = Modifier.offset(x = 28.dp, y = 0.dp).zIndex(500f)
                    .clip(RoundedCornerShape(4.dp))
                    .background(EditorColors.darkSurfaceVariant.copy(alpha = 0.98f))
                    .border(0.5.dp, EditorColors.dividerColor, RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(tooltip, color = EditorColors.textPrimary, fontSize = 11.sp, fontWeight = FontWeight.Normal)
            }
        }
    }
}
