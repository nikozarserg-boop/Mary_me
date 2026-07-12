package org.example.animation.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import org.example.animation.engine.AnimationEngine
import org.example.animation.localization.EditorStrings
import org.example.animation.ui.theme.*

@Composable
fun LayersPanel(engine: AnimationEngine) {
    val project by engine.project.collectAsState()
    val currentLayerIndex by engine.currentLayerIndex.collectAsState()
    val isVisible by engine.isLayersVisible.collectAsState()
    
    if (!isVisible) return

    val layers = project.layers
    val listState = rememberLazyListState()
    
    // Состояние переименования
    var editingIndex by remember { mutableStateOf<Int?>(null) }
    var editText by remember { mutableStateOf("") }
    
    // Состояние Drag & Drop
    var draggedItemIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableStateOf(0f) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Убрано дублирование заголовка панели (он теперь в GlassPanel в EditorScreen)

        // Список слоев
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 4.dp.scaled())
            ) {
                itemsIndexed(layers.reversed()) { reversedIndex, layer ->
                    val actualIndex = layers.size - 1 - reversedIndex
                    val isDragged = draggedItemIndex == actualIndex
                    
                    Box(
                        modifier = Modifier
                            .zIndex(if (isDragged) 1f else 0f)
                            .graphicsLayer {
                                if (isDragged) {
                                    translationY = dragOffset
                                    shadowElevation = 8.dp.toPx()
                                }
                            }
                            .pointerInput(actualIndex) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = { draggedItemIndex = actualIndex },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        dragOffset += dragAmount.y
                                        
                                        val threshold = 24.dp.toPx()
                                        if (dragOffset > threshold && actualIndex > 0) {
                                            engine.moveLayer(actualIndex, actualIndex - 1)
                                            draggedItemIndex = actualIndex - 1
                                            dragOffset = 0f
                                        } else if (dragOffset < -threshold && actualIndex < layers.size - 1) {
                                            engine.moveLayer(actualIndex, actualIndex + 1)
                                            draggedItemIndex = actualIndex + 1
                                            dragOffset = 0f
                                        }
                                    },
                                    onDragEnd = { draggedItemIndex = null; dragOffset = 0f },
                                    onDragCancel = { draggedItemIndex = null; dragOffset = 0f }
                                )
                            }
                    ) {
                        CompactLayerItem(
                            name = layer.name,
                            isSelected = actualIndex == currentLayerIndex,
                            isVisible = layer.isVisible,
                            isLocked = layer.isLocked,
                            isEditing = editingIndex == actualIndex,
                            editText = if (editingIndex == actualIndex) editText else layer.name,
                            onSelect = { engine.setCurrentLayer(actualIndex) },
                            onToggleVisible = { engine.setLayerVisible(actualIndex, !layer.isVisible) },
                            onToggleLocked = { engine.setLayerLocked(actualIndex, !layer.isLocked) },
                            onStartRename = {
                                editingIndex = actualIndex
                                editText = layer.name
                            },
                            onTextChange = { editText = it },
                            onConfirmRename = {
                                engine.renameLayer(actualIndex, editText)
                                editingIndex = null
                            },
                            onCancelRename = { editingIndex = null }
                        )
                    }
                }
            }
        }
        
        // Нижняя панель инструментов (Кнопки управления слоями)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.1f))
                .padding(4.dp.scaled()),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SmallFooterBtn(EditorIcons.iconAdd) { engine.addLayer() }
            Spacer(Modifier.width(8.dp.scaled()))
            SmallFooterBtn(EditorIcons.iconArrowUp) { engine.moveLayerUp(currentLayerIndex) }
            Spacer(Modifier.width(4.dp.scaled()))
            SmallFooterBtn(EditorIcons.iconArrowDown) { engine.moveLayerDown(currentLayerIndex) }
            Spacer(Modifier.width(12.dp.scaled()))
            SmallFooterBtn(EditorIcons.iconDelete) { engine.removeLayer(currentLayerIndex) }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CompactLayerItem(
    name: String,
    isSelected: Boolean,
    isVisible: Boolean,
    isLocked: Boolean,
    isEditing: Boolean = false,
    editText: String = "",
    onSelect: () -> Unit,
    onToggleVisible: () -> Unit,
    onToggleLocked: () -> Unit,
    onStartRename: () -> Unit = {},
    onTextChange: (String) -> Unit = {},
    onConfirmRename: () -> Unit = {},
    onCancelRename: () -> Unit = {}
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val textFieldInteraction = remember { MutableInteractionSource() }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(isEditing) {
        if (isEditing) focusRequester.requestFocus()
    }

    val backgroundColor = when {
        isSelected -> EditorColors.selection.copy(alpha = 0.3f)
        isHovered -> EditorColors.hover.copy(alpha = 0.5f)
        else -> Color.Transparent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(26.dp.scaled())
            .padding(horizontal = 4.dp.scaled(), vertical = 1.dp.scaled())
            .clip(RoundedCornerShape(3.dp.scaled()))
            .background(backgroundColor)
            .border(
                width = 0.8.dp.scaled(),
                color = if (isHovered) EditorColors.accent.copy(alpha = 0.4f) else Color.Transparent,
                shape = RoundedCornerShape(3.dp.scaled())
            )
            .clickable(interactionSource = interactionSource, indication = null, onClick = onSelect)
            .padding(horizontal = 6.dp.scaled()),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            if (isVisible) EditorIcons.iconVisibility else EditorIcons.iconVisibilityOff,
            null,
            tint = if (isVisible) EditorColors.textPrimary else EditorColors.textMuted,
            modifier = Modifier.size(12.dp.scaled()).clickable { onToggleVisible() }
        )
        Spacer(Modifier.width(6.dp.scaled()))
        Icon(
            if (isLocked) EditorIcons.iconLock else EditorIcons.iconLockOpen,
            null,
            tint = if (isLocked) EditorColors.accentOrange else EditorColors.textMuted,
            modifier = Modifier.size(12.dp.scaled()).clickable { onToggleLocked() }
        )
        Spacer(Modifier.width(8.dp.scaled()))

        if (isEditing) {
            TextField(
                value = editText,
                onValueChange = onTextChange,
                singleLine = true,
                colors = TextFieldDefaults.textFieldColors(
                    textColor = EditorColors.textPrimary,
                    backgroundColor = Color.Transparent,
                    cursorColor = EditorColors.accent,
                    focusedIndicatorColor = EditorColors.accent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                textStyle = EditorTypography.layerName(),
                modifier = Modifier
                    .weight(1f)
                    .height(20.dp.scaled())
                    .focusRequester(focusRequester)
                    .onKeyEvent { event ->
                        when (event.key) {
                            Key.Enter -> {
                                onConfirmRename()
                                true
                            }
                            Key.Escape -> {
                                onCancelRename()
                                true
                            }
                            else -> false
                        }
                    },
                interactionSource = textFieldInteraction
            )
        } else {
            Text(
                text = name,
                style = EditorTypography.layerName(),
                maxLines = 1,
                color = if (isSelected) EditorColors.accent else EditorColors.textPrimary,
                modifier = Modifier
                    .weight(1f)
                    .combinedClickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onSelect,
                        onDoubleClick = onStartRename
                    )
            )
        }
    }
}

@Composable
private fun SmallFooterBtn(icon: ImageVector, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    
    Box(
        modifier = Modifier
            .size(24.dp.scaled())
            .clip(RoundedCornerShape(4.dp.scaled()))
            .background(Color.White.copy(alpha = 0.03f))
            .border(
                width = 0.8.dp.scaled(),
                color = if (isHovered) EditorColors.accent.copy(alpha = 0.6f) else Color.Transparent,
                shape = RoundedCornerShape(4.dp.scaled())
            )
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon, null, 
            tint = if (isHovered) EditorColors.accent else EditorColors.textSecondary, 
            modifier = Modifier.size(14.dp.scaled())
        )
    }
}
