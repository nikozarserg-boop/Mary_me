package org.example.animation.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.example.animation.ui.theme.EditorColors
import org.example.animation.ui.theme.EditorTypography
import org.example.animation.ui.theme.scaled

// Глобальное состояние tooltip'а
data class TooltipState(
    val text: String = "",
    val anchorBounds: IntRect = IntRect.Zero,
    val visible: Boolean = false
)

class TooltipManager {
    private val _state = mutableStateOf(TooltipState())
    val state: State<TooltipState> = _state
    
    fun show(text: String, bounds: Rect) {
        _state.value = TooltipState(
            text = text,
            anchorBounds = IntRect(
                left = bounds.left.toInt(),
                top = bounds.top.toInt(),
                right = bounds.right.toInt(),
                bottom = bounds.bottom.toInt()
            ),
            visible = true
        )
    }
    
    fun hide() {
        _state.value = TooltipState()
    }
}

val LocalTooltipManager = staticCompositionLocalOf<TooltipManager> {
    error("TooltipManager not provided")
}

class TooltipPositionProvider(
    private val anchorBounds: IntRect,
    private val offsetX: Int = 16,
    private val offsetY: Int = 20
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        var x = anchorBounds.left + offsetX
        var y = anchorBounds.bottom + offsetY
        
        // Проверяем границы окна
        if (x + popupContentSize.width > windowSize.width) {
            x = (anchorBounds.right - popupContentSize.width - offsetX).coerceAtLeast(0)
        }
        if (y + popupContentSize.height > windowSize.height) {
            y = (anchorBounds.top - popupContentSize.height - offsetY).coerceAtLeast(0)
        }
        
        return IntOffset(x, y)
    }
}

@Composable
fun TooltipHost() {
    val tooltipState by LocalTooltipManager.current.state
    
    if (tooltipState.visible && tooltipState.text.isNotEmpty()) {
        Popup(
            popupPositionProvider = TooltipPositionProvider(tooltipState.anchorBounds),
            onDismissRequest = { }
        ) {
            Surface(
                color = EditorColors.surface,
                shape = RoundedCornerShape(4.dp.scaled())
            ) {
                Text(
                    tooltipState.text,
                    style = EditorTypography.toolText(),
                    color = EditorColors.textPrimary,
                    modifier = Modifier.padding(horizontal = 6.dp.scaled(), vertical = 3.dp.scaled())
                )
            }
        }
    }
}

@Composable
fun TooltipBox(
    tooltip: String,
    delayMs: Long = 400,
    content: @Composable () -> Unit
) {
    val tooltipManager = LocalTooltipManager.current
    val scope = rememberCoroutineScope()
    var job by remember { mutableStateOf<Job?>(null) }
    var bounds by remember { mutableStateOf<Rect?>(null) }
    
    Box(
        modifier = Modifier
            .onGloballyPositioned { bounds = it.boundsInRoot() }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        when (event.type) {
                            androidx.compose.ui.input.pointer.PointerEventType.Enter -> {
                                job?.cancel()
                                job = scope.launch {
                                    delay(delayMs)
                                    bounds?.let { tooltipManager.show(tooltip, it) }
                                }
                            }
                            androidx.compose.ui.input.pointer.PointerEventType.Exit -> {
                                job?.cancel()
                                tooltipManager.hide()
                            }
                            else -> {}
                        }
                    }
                }
            }
    ) {
        content()
    }
}
