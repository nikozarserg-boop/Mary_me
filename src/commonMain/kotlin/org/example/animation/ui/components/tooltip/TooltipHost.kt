package org.example.animation.ui.components.tooltip

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import org.example.animation.ui.theme.EditorColors
import org.example.animation.ui.theme.EditorTypography
import org.example.animation.ui.theme.scaled

/**
 * Глобальный слой tooltip'ов.
 *
 * Должен быть размещён ровно один раз в корне приложения, ПОВЕРХ основного
 * контента (но внутри [LocalTooltipManager]):
 *
 * ```
 * Box(Modifier.fillMaxSize()) {
 *     Surface(Modifier.fillMaxSize()) { AppContent() }
 *     TooltipHost()   // Popup, НЕ влияет на layout
 * }
 * ```
 *
 * Tooltip рисуется через [Popup] поверх интерфейса, поэтому не вызывает
 * пересчёта layout, смещения кнопок, изменения размеров панелей и scroll.
 */
@Composable
fun TooltipHost() {
    val manager = LocalTooltipManager.current
    val tooltipState = manager.state.value

    if (tooltipState.visible && tooltipState.text.isNotEmpty()) {
        Popup(
            popupPositionProvider = TooltipPositionProvider(tooltipState.anchorBounds),
            onDismissRequest = { manager.hide() }
        ) {
            Surface(
                color = EditorColors.surface,
                shape = RoundedCornerShape(4.dp.scaled()),
                elevation = 4.dp.scaled()
            ) {
                Text(
                    tooltipState.text,
                    style = EditorTypography.toolText(),
                    color = EditorColors.textPrimary,
                    maxLines = 1,
                    modifier = Modifier
                        .border(1.dp.scaled(), EditorColors.divider, RoundedCornerShape(4.dp.scaled()))
                        .padding(horizontal = 6.dp.scaled(), vertical = 3.dp.scaled())
                )
            }
        }
    }
}