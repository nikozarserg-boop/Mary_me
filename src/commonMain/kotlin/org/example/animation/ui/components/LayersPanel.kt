package org.example.animation.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
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

@OptIn(ExperimentalFoundationApi::class)
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
            val eyeInteraction = remember { MutableInteractionSource() }
            val eyeHovered by eyeInteraction.collectIsHoveredAsState()

            IconButton(
                onClick = onToggleVisible,
                modifier = Modifier
                    .size(24.dp.scaled())
                    .hoverable(eyeInteraction)
                    .tooltipAnchor(EditorStrings.observeString("layer.visible")),
                content = {
                    Icon(
                        if (isVisible) EditorIcons.iconVisibility else EditorIcons.iconVisibilityOff,
                        null,
                        tint = if (isVisible) EditorColors.textPrimary else EditorColors.textMuted,
                        modifier = Modifier.size(16.dp.scaled())
                    )
                }
            )

            val lockInteraction = remember { MutableInteractionSource() }
            val lockHovered by lockInteraction.collectIsHoveredAsState()

            IconButton(
                onClick = onToggleLocked,
                modifier = Modifier
                    .size(24.dp.scaled())
                    .hoverable(lockInteraction)
                    .tooltipAnchor(EditorStrings.observeString("layer.locked")),
                content = {
                    Icon(
                        EditorIcons.iconLock,
                        null,
                        tint = if (isLocked) EditorColors.accent else EditorColors.textMuted.copy(alpha = 0.5f),
                        modifier = Modifier.size(16.dp.scaled())
                    )
                }
            )

            Spacer(Modifier.width(4.dp.scaled()))

            Box(modifier = Modifier.weight(1f).fillMaxHeight().clickable { onSelect() }) {
                if (isEditing) {
                    var textFieldValue by remember { mutableStateOf(editText) }
                    var hasFocus by remember { mutableStateOf(false) }

                    BasicTextField(
                        value = textFieldValue,
                        onValueChange = {
                            textFieldValue = it
                            onTextChange(it)
                        },
                        modifier = Modifier.fillMaxSize(),
                        textStyle = EditorTypography.layerName().copy(color = if (isSelected) EditorColors.textPrimary else EditorColors.textSecondary)
                    )

                    LaunchedEffect(Unit) {
                        delay(100)
                        hasFocus = true
                    }

                    LaunchedEffect(textFieldValue) {
                        if (textFieldValue.isEmpty()) {
                            onCancelRename()
                        }
                    }
                } else {
                    Text(
                        name,
                        style = EditorTypography.layerName(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = if (isSelected) EditorColors.textPrimary else EditorColors.textSecondary
                    )
                }
            }

            IconButton(
                onClick = onStartRename,
                modifier = Modifier
                    .size(20.dp.scaled())
                    .tooltipAnchor(EditorStrings.observeString("layer.rename"))
            ) {
                Icon(EditorIcons.iconEdit, null, tint = EditorColors.textMuted, modifier = Modifier.size(14.dp.scaled()))
            }
        }
    }
}

@Composable
private fun SmallFooterBtn(icon: ImageVector, tooltip: String = "", onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    Box(contentAlignment = Alignment.Center) {
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
}
