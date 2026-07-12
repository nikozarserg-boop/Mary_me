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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
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
    
    // Состояние Drag & Drop
    var draggedItemIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableStateOf(0f) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(EditorColors.panelHeader.copy(alpha = 0.5f))
                .padding(horizontal = 8.dp.scaled(), vertical = 4.dp.scaled()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                EditorStrings.observeString("panel.layers").uppercase(),
                style = EditorTypography.panelTitle(),
                color = EditorColors.textSecondary
            )
            Row {
                SmallHeaderBtn(EditorIcons.iconAdd) { engine.addLayer() }
                SmallHeaderBtn(EditorIcons.iconClose) { engine.setLayersVisible(false) }
            }
        }

        Divider(color = EditorColors.divider.copy(alpha = 0.3f))

        // Layers List
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize()
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
                            onSelect = { engine.setCurrentLayer(actualIndex) },
                            onToggleVisible = { engine.setLayerVisible(actualIndex, !layer.isVisible) },
                            onToggleLocked = { engine.setLayerLocked(actualIndex, !layer.isLocked) }
                        )
                    }
                }
            }
        }
        
        // Footer
        Row(
            modifier = Modifier.fillMaxWidth().background(Color.Black.copy(alpha = 0.1f)).padding(2.dp.scaled()),
            horizontalArrangement = Arrangement.Center
        ) {
            SmallFooterBtn(EditorIcons.iconArrowUp) { engine.moveLayerUp(currentLayerIndex) }
            Spacer(Modifier.width(4.dp.scaled()))
            SmallFooterBtn(EditorIcons.iconArrowDown) { engine.moveLayerDown(currentLayerIndex) }
            Spacer(Modifier.width(4.dp.scaled()))
            SmallFooterBtn(EditorIcons.iconDelete) { engine.removeLayer(currentLayerIndex) }
        }
    }
}

@Composable
private fun CompactLayerItem(
    name: String,
    isSelected: Boolean,
    isVisible: Boolean,
    isLocked: Boolean,
    onSelect: () -> Unit,
    onToggleVisible: () -> Unit,
    onToggleLocked: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    
    val backgroundColor = when {
        isSelected -> EditorColors.selection.copy(alpha = 0.3f)
        isHovered -> EditorColors.hover.copy(alpha = 0.5f)
        else -> Color.Transparent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(24.dp.scaled())
            .padding(horizontal = 2.dp.scaled(), vertical = 0.5.dp.scaled())
            .clip(RoundedCornerShape(2.dp.scaled()))
            .background(backgroundColor)
            .border(
                width = 1.dp.scaled(),
                color = if (isHovered) EditorColors.accent.copy(alpha = 0.4f) else Color.Transparent,
                shape = RoundedCornerShape(2.dp.scaled())
            )
            .clickable(interactionSource = interactionSource, indication = null, onClick = onSelect)
            .padding(horizontal = 4.dp.scaled()),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            if (isVisible) EditorIcons.iconVisibility else EditorIcons.iconVisibilityOff,
            null,
            tint = if (isVisible) EditorColors.textPrimary else EditorColors.textMuted,
            modifier = Modifier.size(10.dp.scaled()).clickable { onToggleVisible() }
        )
        Spacer(Modifier.width(4.dp.scaled()))
        Icon(
            if (isLocked) EditorIcons.iconLock else EditorIcons.iconLockOpen,
            null,
            tint = if (isLocked) EditorColors.accentOrange else EditorColors.textMuted,
            modifier = Modifier.size(10.dp.scaled()).clickable { onToggleLocked() }
        )
        Spacer(Modifier.width(6.dp.scaled()))
        
        Text(
            text = name,
            style = EditorTypography.layerName(),
            maxLines = 1,
            color = if (isSelected) EditorColors.accent else EditorColors.textPrimary,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SmallHeaderBtn(icon: ImageVector, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(18.dp.scaled())) {
        Icon(icon, null, tint = EditorColors.textSecondary, modifier = Modifier.size(12.dp.scaled()))
    }
}

@Composable
private fun SmallFooterBtn(icon: ImageVector, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(20.dp.scaled()).background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(2.dp.scaled()))) {
        Icon(icon, null, tint = EditorColors.textSecondary, modifier = Modifier.size(12.dp.scaled()))
    }
}
