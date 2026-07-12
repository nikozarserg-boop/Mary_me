package org.example.animation.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.animation.engine.AnimationEngine
import org.example.animation.ui.theme.EditorColors

/**
 * Палитра цветов
 */
@Composable
fun ColorPicker(
    engine: AnimationEngine,
    modifier: Modifier = Modifier
) {
    val currentColor by engine.currentColor.collectAsState()
    val brushSize by engine.brushSize.collectAsState()
    val opacity by engine.opacity.collectAsState()

    Surface(
        modifier = modifier.width(220.dp),
        color = EditorColors.panelBackground,
        elevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(8.dp)
        ) {
            // Заголовок
            Text(
                text = "ЦВЕТ",
                color = EditorColors.textSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Текущий цвет
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(ulongToColor(currentColor))
                        .border(1.dp, EditorColors.canvasBorder, RoundedCornerShape(6.dp))
                )

                Spacer(Modifier.width(8.dp))

                Column {
                    Text(
                        text = "#${currentColor.toString(16).padStart(8, '0').uppercase().takeLast(6)}",
                        color = EditorColors.textPrimary,
                        fontSize = 12.sp
                    )
                    Text(
                        text = "Прозрачность: ${(opacity * 100).toInt()}%",
                        color = EditorColors.textSecondary,
                        fontSize = 10.sp
                    )
                }
            }

            // Палитра предустановленных цветов
            Text(
                text = "ПАЛИТРА",
                color = EditorColors.textMuted,
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            val presetColors = listOf(
                0xFF000000uL, 0xFF333333uL, 0xFF666666uL, 0xFF999999uL, 0xFFCCCCCCuL, 0xFFFFFFFFuL,
                0xFFFF0000uL, 0xFFFF4444uL, 0xFFFF6B6BuL, 0xFFFF9999uL, 0xFFFFCCCCuL, 0xFFFFE0E0uL,
                0xFFFF8800uL, 0xFFFFAA33uL, 0xFFFFCC66uL, 0xFFFFDD99uL, 0xFFFFEECCuL, 0xFFFFF3E0uL,
                0xFFFFFF00uL, 0xFFFFDD00uL, 0xFFFFCC00uL, 0xFFFFBB33uL, 0xFFFFAA00uL, 0xFFFF8800uL,
                0xFF00FF00uL, 0xFF44CC44uL, 0xFF66BB66uL, 0xFF88CC88uL, 0xFFAAEEAAuL, 0xFFCCFFCCuL,
                0xFF00CCFFuL, 0xFF3399FFuL, 0xFF409EFFuL, 0xFF66B3FFuL, 0xFF99CCFFuL, 0xFFCCE5FFuL,
                0xFF0000FFuL, 0xFF3333FFuL, 0xFF6666FFuL, 0xFF8888FFuL, 0xFFAAAAFFuL, 0xFFCCCCFFuL,
                0xFF9900FFuL, 0xFFAA44FFuL, 0xFFBB66FFuL, 0xFFCC88FFuL, 0xFFDDAAFFuL, 0xFFEECCFFuL,
                0xFFFF00FFuL, 0xFFFF44FFuL, 0xFFFF66FFuL, 0xFFFF88FFuL, 0xFFFFAAFFuL, 0xFFFFCCFFuL,
                0xFF880000uL, 0xFF994400uL, 0xFF888800uL, 0xFF008800uL, 0xFF004488uL, 0xFF440088uL
            )

            // Сетка цветов
            val rows = presetColors.chunked(6)
            Column {
                rows.forEach { rowColors ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        rowColors.forEach { color ->
                            val isSelected = color == currentColor
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(ulongToColor(color))
                                    .border(
                                        width = if (isSelected) 2.dp else 0.5.dp,
                                        color = if (isSelected) EditorColors.accentBlue
                                        else EditorColors.dividerColor,
                                        shape = RoundedCornerShape(3.dp)
                                    )
                                    .clickable { engine.setCurrentColor(color) }
                            )
                        }
                    }
                    Spacer(Modifier.height(3.dp))
                }
            }

            Spacer(Modifier.height(12.dp))

            // Размер кисти
            Text(
                text = "РАЗМЕР КИСТИ",
                color = EditorColors.textMuted,
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            Slider(
                value = brushSize,
                onValueChange = { engine.setBrushSize(it) },
                valueRange = 1f..100f,
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor = EditorColors.accentBlue,
                    activeTrackColor = EditorColors.accentBlue,
                    inactiveTrackColor = EditorColors.darkSurfaceVariant
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("1", color = EditorColors.textMuted, fontSize = 9.sp)
                Text("${brushSize.toInt()}", color = EditorColors.textPrimary, fontSize = 10.sp)
                Text("100", color = EditorColors.textMuted, fontSize = 9.sp)
            }

            Spacer(Modifier.height(8.dp))

            // Прозрачность
            Text(
                text = "ПРОЗРАЧНОСТЬ",
                color = EditorColors.textMuted,
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            Slider(
                value = opacity,
                onValueChange = { engine.setOpacity(it) },
                valueRange = 0f..1f,
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor = EditorColors.accentBlue,
                    activeTrackColor = EditorColors.accentBlue,
                    inactiveTrackColor = EditorColors.darkSurfaceVariant
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("0%", color = EditorColors.textMuted, fontSize = 9.sp)
                Text("${(opacity * 100).toInt()}%", color = EditorColors.textPrimary, fontSize = 10.sp)
                Text("100%", color = EditorColors.textMuted, fontSize = 9.sp)
            }
        }
    }
}

private fun ulongToColor(color: ULong): Color {
    val a = ((color shr 24) and 0xFFuL).toInt()
    val r = ((color shr 16) and 0xFFuL).toInt()
    val g = ((color shr 8) and 0xFFuL).toInt()
    val b = (color and 0xFFuL).toInt()
    return Color(r, g, b, a)
}