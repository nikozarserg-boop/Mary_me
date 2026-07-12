package org.example.animation.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.animation.localization.EditorStrings
import org.example.animation.ui.theme.EditorColors
import org.example.animation.ui.theme.EditorTypography
import org.example.animation.ui.theme.scaled

/**
 * Единообразный ряд кнопок для всех диалогов:
 * "Отмена" (нейтральная) и "Принять"/"Применить" (акцентная).
 * Одинаковая высота (40.dp) и выравнивание во всех окнах.
 */
@Composable
fun DialogButtonRow(
    cancelText: String = EditorStrings.observeString("cancel"),
    confirmText: String,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    cancelEnabled: Boolean = true,
    confirmEnabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp.scaled()),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier
                .height(40.dp.scaled())
                .width(120.dp.scaled())
                .clickable(
                    enabled = cancelEnabled,
                    onClick = onCancel
                ),
            color = EditorColors.divider.copy(alpha = 0.25f),
            shape = RoundedCornerShape(8.dp.scaled())
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    cancelText,
                    color = if (cancelEnabled) EditorColors.textSecondary else EditorColors.textMuted,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp.scaled()
                )
            }
        }
        Spacer(Modifier.width(12.dp.scaled()))
        Surface(
            modifier = Modifier
                .height(40.dp.scaled())
                .width(120.dp.scaled())
                .clickable(
                    enabled = confirmEnabled,
                    onClick = onConfirm
                ),
            color = if (confirmEnabled) EditorColors.accent else EditorColors.accent.copy(alpha = 0.4f),
            shape = RoundedCornerShape(8.dp.scaled())
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    confirmText,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp.scaled()
                )
            }
        }
    }
}