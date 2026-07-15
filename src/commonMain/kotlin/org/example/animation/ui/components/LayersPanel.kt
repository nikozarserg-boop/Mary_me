package org.example.animation.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.example.animation.engine.AnimationEngine
import org.example.animation.localization.EditorStrings
import org.example.animation.ui.components.tooltip.tooltipAnchor
import org.example.animation.ui.theme.*

@Composable
fun LayersPanel(engine: AnimationEngine) {
    val project by engine.project.collectAsState()
    val currentLayerIndex by engine.currentLayerIndex.collectAsState()
    
    var editingIndex by remember { mutableStateOf<Int?>(null) }
    var editText by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            project.layers.forEachIndexed { index, layer ->
                val actualIndex = project.layers.size - 1 - index
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
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.1f))
                .padding(4.dp.scaled()),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SmallFooterBtn(EditorIcons.iconAdd, tooltip = EditorStrings.observeString("layer.add")) { engine.addLayer() }
            Spacer(Modifier.width(8.dp.scaled()))
            SmallFooterBtn(EditorIcons.iconArrowUp, tooltip = EditorStrings.observeString("layer.up")) { engine.moveLayerUp(currentLayerIndex) }
            Spacer(Modifier.width(4.dp.scaled()))
            SmallFooterBtn(EditorIcons.iconArrowDown, tooltip = EditorStrings.observeString("layer.down")) { engine.moveLayerDown(currentLayerIndex) }
            Spacer(Modifier.width(12.dp.scaled()))
            SmallFooterBtn(EditorIcons.iconDelete, tooltip = EditorStrings.observeString("layer.remove")) { engine.removeLayer(currentLayerIndex) }
        }
    }
}

@Composable
private fun LayerIconButton(
    icon: ImageVector,
    tint: Color,
    tooltip: String,
    onClick: () -> Unit,
    size: androidx.compose.ui.unit.Dp = 24.dp,
    iconSize: androidx.compose.ui.unit.Dp = 16.dp
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    Box(
        modifier = Modifier
            .size(size.scaled())
            .clip(RoundedCornerShape(4.dp.scaled()))
            .background(if (isHovered) EditorColors.hover else Color.Transparent)
            .tooltipAnchor(tooltip)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon, null,
            tint = if (isHovered) EditorColors.accent else tint,
            modifier = Modifier.size(iconSize.scaled())
        )
    }
}

@Composable
private fun CompactLayerItem(
    name: String,
    isSelected: Boolean,
    isVisible: Boolean,
    isLocked: Boolean,
    isEditing: Boolean,
    editText: String,
    onSelect: () -> Unit,
    onToggleVisible: () -> Unit,
    onToggleLocked: () -> Unit,
    onStartRename: () -> Unit,
    onTextChange: (String) -> Unit,
    onConfirmRename: () -> Unit,
    onCancelRename: () -> Unit
) {
    val background = if (isSelected) EditorColors.selection.copy(alpha = 0.5f) else Color.Transparent

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp.scaled())
            .background(background)
            .padding(horizontal = 4.dp.scaled())
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxSize()
        ) {
            LayerIconButton(
                icon = if (isVisible) EditorIcons.iconVisibility else EditorIcons.iconVisibilityOff,
                tint = if (isVisible) EditorColors.textPrimary else EditorColors.textMuted,
                tooltip = EditorStrings.observeString("layer.visible"),
                onClick = onToggleVisible
            )

            LayerIconButton(
                icon = EditorIcons.iconLock,
                tint = if (isLocked) EditorColors.accent else EditorColors.textMuted.copy(alpha = 0.5f),
                tooltip = EditorStrings.observeString("layer.locked"),
                onClick = onToggleLocked
            )

            Spacer(Modifier.width(4.dp.scaled()))

            val nameInteractionSource = remember { MutableInteractionSource() }
            val nameHovered by nameInteractionSource.collectIsHoveredAsState()

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable(interactionSource = nameInteractionSource, indication = null) { onSelect() },
                contentAlignment = Alignment.CenterStart
            ) {
                if (isEditing) {
                    var textFieldValue by remember { mutableStateOf(editText) }
                    val focusRequester = remember { FocusRequester() }
                    var wasFocused by remember { mutableStateOf(false) }

                    LaunchedEffect(Unit) {
                        focusRequester.requestFocus()
                    }

                    BasicTextField(
                        value = textFieldValue,
                        onValueChange = {
                            textFieldValue = it
                            onTextChange(it)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                            .onFocusChanged { 
                                if (!it.isFocused && wasFocused) onConfirmRename()
                                wasFocused = it.isFocused
                            }
                            .onPreviewKeyEvent {
                                if (it.key == Key.Escape) {
                                    onCancelRename()
                                    true
                                } else false
                            },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { onConfirmRename() }),
                        textStyle = EditorTypography.layerName().copy(color = EditorColors.textPrimary),
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(EditorColors.accent)
                    )
                } else {
                    Text(
                        name,
                        style = EditorTypography.layerName(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = if (nameHovered || isSelected) EditorColors.textPrimary else EditorColors.textSecondary
                    )
                }
            }

            LayerIconButton(
                icon = EditorIcons.iconEdit,
                tint = EditorColors.textMuted,
                tooltip = EditorStrings.observeString("layer.rename"),
                onClick = onStartRename,
                size = 20.dp,
                iconSize = 14.dp
            )
        }
    }
}

@Composable
private fun SmallFooterBtn(icon: ImageVector, tooltip: String = "", onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    Box(
        modifier = Modifier
            .size(24.dp.scaled())
            .clip(RoundedCornerShape(4.dp.scaled()))
            .background(if (isHovered) EditorColors.hover else Color.White.copy(alpha = 0.03f))
            .tooltipAnchor(tooltip)
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
