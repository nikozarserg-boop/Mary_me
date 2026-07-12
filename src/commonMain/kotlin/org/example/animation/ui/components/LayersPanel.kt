package org.example.animation.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.vector.ImageVector
import org.example.animation.engine.AnimationEngine
import org.example.animation.ui.theme.EditorColors
import org.example.animation.ui.theme.EditorIcons

@Composable
fun LayersPanel(engine: AnimationEngine, modifier: Modifier = Modifier) {
    val project by engine.project.collectAsState()
    val currentLayerIndex by engine.currentLayerIndex.collectAsState()

    Surface(modifier = modifier.width(220.dp), color = EditorColors.panelBackground, elevation = 2.dp) {
        Column(modifier = Modifier.fillMaxHeight()) {
            Box(modifier = Modifier.fillMaxWidth().background(EditorColors.panelHeader).padding(horizontal = 8.dp, vertical = 6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(EditorIcons.iconLayers, "Слои", tint = EditorColors.textSecondary, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(text = "СЛОИ", color = EditorColors.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
                }
            }
            Divider(color = EditorColors.dividerColor, thickness = 1.dp)
            Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                project.layers.forEachIndexed { index, layer ->
                    LayerItem(
                        name = layer.name,
                        isVisible = layer.isVisible,
                        opacity = layer.opacity,
                        isLocked = layer.isLocked,
                        isSelected = index == currentLayerIndex,
                        onClick = { engine.setCurrentLayer(index) },
                        onVisibilityChange = { engine.setLayerVisibility(index, it) },
                        onLockChange = { engine.setLayerLocked(index, it) },
                        onRename = { engine.setLayerName(index, it) }
                    )
                }
            }
            Divider(color = EditorColors.dividerColor, thickness = 1.dp)
            Row(modifier = Modifier.fillMaxWidth().padding(4.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                SmallIconBtn(EditorIcons.iconAdd, "Добавить слой") { engine.addLayer() }
                SmallIconBtn(EditorIcons.iconRemove, "Удалить слой") { engine.removeLayer(currentLayerIndex) }
                SmallIconBtn(EditorIcons.iconArrowUpward, "Вверх") { if (currentLayerIndex > 0) engine.moveLayer(currentLayerIndex, currentLayerIndex - 1) }
                SmallIconBtn(EditorIcons.iconArrowDownward, "Вниз") { if (currentLayerIndex < project.layers.size - 1) engine.moveLayer(currentLayerIndex, currentLayerIndex + 1) }
            }
        }
    }
}

@Composable
private fun LayerItem(
    name: String, isVisible: Boolean, opacity: Float, isLocked: Boolean, isSelected: Boolean,
    onClick: () -> Unit,
    onVisibilityChange: (Boolean) -> Unit,
    onLockChange: (Boolean) -> Unit,
    onRename: (String) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    var isRenaming by remember { mutableStateOf(false) }
    var renameValue by remember { mutableStateOf(TextFieldValue(name)) }

    if (isRenaming) {
        AlertDialog(
            onDismissRequest = { isRenaming = false },
            title = { Text("Имя слоя") },
            text = {
                OutlinedTextField(
                    value = renameValue,
                    onValueChange = { renameValue = it },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val newName = renameValue.text.trim()
                        if (newName.isNotEmpty() && newName != name) onRename(newName)
                        isRenaming = false
                    }
                ) {
                    Text("ОК")
                }
            },
            dismissButton = {
                TextButton(onClick = { isRenaming = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    Column(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(if (isSelected || isHovered) EditorColors.darkSurfaceVariant else Color.Transparent)
            .clickable(indication = null, interactionSource = interactionSource) { onClick() }
            .padding(4.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onVisibilityChange(!isVisible) }, modifier = Modifier.size(24.dp)) {
                Icon(
                    if (isVisible) EditorIcons.iconVisibility else EditorIcons.iconVisibilityOff,
                    "Видимость",
                    tint = if (isVisible) EditorColors.textPrimary else EditorColors.textMuted,
                    modifier = Modifier.size(16.dp)
                )
            }
            IconButton(onClick = { onLockChange(!isLocked) }, modifier = Modifier.size(20.dp)) {
                Icon(
                    if (isLocked) EditorIcons.iconLock else EditorIcons.iconLockOpen,
                    "Блокировка",
                    tint = if (isLocked) EditorColors.accentOrange else EditorColors.textMuted,
                    modifier = Modifier.size(14.dp)
                )
            }
            Spacer(Modifier.width(4.dp))

            Text(
                text = name,
                color = if (isLocked) EditorColors.textMuted else EditorColors.textPrimary,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .clickable {
                        renameValue = TextFieldValue(name)
                        isRenaming = true
                    }
            )

            Text(
                text = "${(opacity * 100).toInt()}%",
                color = EditorColors.textMuted,
                fontSize = 9.sp,
                modifier = Modifier.padding(start = 2.dp)
            )
        }
    }
}

@Composable
private fun SmallIconBtn(icon: ImageVector, description: String, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(28.dp)) {
        Icon(icon, description, tint = EditorColors.textSecondary, modifier = Modifier.size(16.dp))
    }
}