package org.example.animation.ui.components.tooltip

import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupPositionProvider

/**
 * Позиционирует tooltip относительно anchor, НЕ влияя на layout родителя.
 *
 * Логика:
 *  - по умолчанию tooltip показывается НИЖЕ anchor (с отступом [offsetY]);
 *  - если снизу не хватает места — показывается ВЫШЕ anchor;
 *  - по горизонтали выравнивается по левому краю anchor (с отступом [offsetX]),
 *    при выходе за правый край окна сдвигается влево;
 *  - учитываются границы окна ([windowSize]).
 */
class TooltipPositionProvider(
    private val anchorBounds: IntRect,
    private val offsetX: Int = 14,
    private val offsetY: Int = 12
) : PopupPositionProvider {

    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        // Используем реальные координаты anchor (передаются из TooltipManager).
        val a = this.anchorBounds

        // Горизонталь: слева от anchor + отступ, либо справа, если не влезает.
        var x = a.left + offsetX
        if (x + popupContentSize.width > windowSize.width) {
            // не влезает справа — пробуем встать правее anchor
            x = (a.right - popupContentSize.width - offsetX).coerceAtLeast(0)
        }
        if (x < 0) x = 0
        if (x + popupContentSize.width > windowSize.width) {
            x = (windowSize.width - popupContentSize.width).coerceAtLeast(0)
        }

        // Вертикаль: сначала ниже anchor.
        var y = a.bottom + offsetY
        if (y + popupContentSize.height > windowSize.height) {
            // не влезает снизу — показываем выше anchor
            y = (a.top - popupContentSize.height - offsetY).coerceAtLeast(0)
        }
        if (y < 0) y = 0
        if (y + popupContentSize.height > windowSize.height) {
            y = (windowSize.height - popupContentSize.height).coerceAtLeast(0)
        }

        return IntOffset(x, y)
    }
}