package org.example.animation.ui.components.tooltip

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntRect

/**
 * Состояние глобального tooltip'а.
 *
 * @param text        текст, который нужно показать
 * @param anchorBounds экранные координаты anchor (относительно окна), [IntRect.Zero] если скрыт
 * @param visible     видим ли tooltip в данный момент
 */
data class TooltipState(
    val text: String = "",
    val anchorBounds: IntRect = IntRect.Zero,
    val visible: Boolean = false
)