package org.example.animation.ui.components.tooltip

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntRect
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Модификатор-anchor: компонент становится точкой привязки tooltip'а.
 *
 * Не создаёт никакого собственного layout — только сообщает [TooltipManager]
 * координаты anchor ([androidx.compose.ui.layout.LayoutCoordinates.boundsInWindow])
 * при наведении и уходе курсора. Сам tooltip рисуется глобально в [TooltipHost].
 *
 * @param text    текст tooltip'а (если пуст — tooltip не показывается)
 * @param enabled включён ли показ tooltip'а (например, false для disabled-кнопок)
 */
@Composable
fun Modifier.tooltipAnchor(
    text: String,
    enabled: Boolean = true,
    manager: TooltipManager = LocalTooltipManager.current
): Modifier {
    if (!enabled || text.isEmpty()) return this

    var bounds: Rect? = null

    return this
        .onGloballyPositioned { coordinates: LayoutCoordinates ->
            bounds = coordinates.boundsInWindow()
        }
        .pointerInput(text) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent()
                    when (event.type) {
                        PointerEventType.Enter -> bounds?.let { manager.show(text, it) }
                        PointerEventType.Exit -> manager.hide()
                        else -> {}
                    }
                }
            }
        }
}

/**
 * Единый глобальный менеджер tooltip'ов.
 *
 * Хранит:
 *  - текущий текст tooltip;
 *  - координаты anchor (boundsInWindow);
 *  - состояние visible;
 *  - задержку появления (delay).
 *
 * Компоненты являются только anchor'ами: они вызывают [show] при наведении
 * и [hide] при уходе курсора. Сам tooltip рисуется в [TooltipHost] в виде Popup
 * и НЕ участвует в обычном layout.
 */
class TooltipManager {

    // Без Main-диспетчера: на JVM Desktop нет kotlinx-coroutines-swing,
    // а Compose State-записи потокобезопасны и могут выполняться из фонового скоупа.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _state = mutableStateOf(TooltipState())
    val state: State<TooltipState> = _state

    /** Задержка появления tooltip после наведения, мс. */
    var showDelayMs: Long = 400
        set(value) {
            field = value.coerceAtLeast(0)
        }

    private var showJob: Job? = null

    // Храним последние anchor-координаты, чтобы показать tooltip сразу по истечении задержки.
    private var pendingText: String = ""
    private var pendingBounds: IntRect = IntRect.Zero

    /**
     * Запрашивает показ tooltip для anchor'а с заданными координатами.
     * Появление произойдёт через [showDelayMs], если к тому моменту курсор
     * не ушёл (не вызван [hide]).
     */
    fun show(text: String, anchorBounds: Rect) {
        pendingText = text
        pendingBounds = IntRect(
            left = anchorBounds.left.toInt(),
            top = anchorBounds.top.toInt(),
            right = anchorBounds.right.toInt(),
            bottom = anchorBounds.bottom.toInt()
        )
        showJob?.cancel()
        showJob = scope.launch {
            if (showDelayMs > 0) delay(showDelayMs)
            _state.value = TooltipState(
                text = pendingText,
                anchorBounds = pendingBounds,
                visible = true
            )
        }
    }

    /** Немедленно скрывает tooltip и отменяет отложенный показ. */
    fun hide() {
        showJob?.cancel()
        showJob = null
        pendingText = ""
        pendingBounds = IntRect.Zero
        if (_state.value.visible || _state.value.text.isNotEmpty()) {
            _state.value = TooltipState()
        }
    }
}

/**
 * CompositionLocal, предоставляющий [TooltipManager] в дереве composable.
 * Должен быть установлен один раз в корне приложения (в [App]).
 */
val LocalTooltipManager = staticCompositionLocalOf<TooltipManager> {
    error("TooltipManager not provided. Wrap your App with CompositionLocalProvider(LocalTooltipManager provides TooltipManager()).")
}