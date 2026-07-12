package org.example.animation.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import org.example.animation.engine.AnimationEngine
import org.example.animation.localization.EditorStrings
import org.example.animation.ui.theme.EditorColors
import org.example.animation.ui.theme.EditorIcons
import org.example.animation.ui.theme.EditorShapes
import org.example.animation.ui.theme.EditorTypography

@Composable
fun LayersPanel(engine: AnimationEngine) {
    val project by engine.project.collectAsState()
    val currentLayerIndex by engine.currentLayerIndex.collectAsState()

    Column(modifier = Modifier.fillMaxSize().background(EditorColors.panelBackground)) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().background(EditorColors.panelHeader).padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                EditorStrings.observeString("panel.layers").uppercase(),
                style = EditorTypography.panelTitle
            )
            Row {
                TooltipIconButton(EditorIcons.iconAdd, EditorStrings.observeString("layer.add")) { engine.addLayer() }
                Spacer(Modifier.width(4.dp))
                TooltipIconButton(EditorIcons.iconDelete, EditorStrings.observeString("layer.remove")) { engine.removeLayer(currentLayerIndex) }
            }
        }

        Divider(color = EditorColors.dividerColor)

        // Layers List
        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
            itemsIndexed(project.layers.reversed()) { index, layer ->
                val actualIndex = project.layers.size - 1 - index
                val isSelected = actualIndex == currentLayerIndex

                LayerItem(
                    name = layer.name,
                    isSelected = isSelected,
                    isVisible = layer.isVisible,
                    isLocked = layer.isLocked,
                    onSelect = { engine.setCurrentLayer(actualIndex) },
                    onToggleVisible = { engine.setLayerVisible(actualIndex, !layer.isVisible) },
                    onToggleLocked = { engine.setLayerLocked(actualIndex, !layer.isLocked) }
                )
                Divider(color = EditorColors.dividerColor, thickness = 0.5.dp)
            }
        }
        
        // Layer Actions
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ActionButton(EditorIcons.iconArrowUp, EditorStrings.observeString("layer.up")) { engine.moveLayerUp(currentLayerIndex) }
            ActionButton(EditorIcons.iconArrowDown, EditorStrings.observeString("layer.down")) { engine.moveLayerDown(currentLayerIndex) }
        }
    }
}

@Composable
private fun LayerItem(
    name: String,
    isSelected: Boolean,
    isVisible: Boolean,
    isLocked: Boolean,
    onSelect: () -> Unit,
    onToggleVisible: () -> Unit,
    onToggleLocked: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(38.dp)
            .background(if (isSelected) EditorColors.selectionColor else Color.Transparent)
            .clickable { onSelect() }
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            if (isVisible) EditorIcons.iconVisibility else EditorIcons.iconVisibilityOff,
            null,
            tint = if (isVisible) EditorColors.textPrimary else EditorColors.textMuted,
            modifier = Modifier.size(16.dp).clickable { onToggleVisible() }
        )
        Spacer(Modifier.width(8.dp))
        Icon(
            if (isLocked) EditorIcons.iconLock else EditorIcons.iconLockOpen,
            null,
            tint = if (isLocked) EditorColors.accentOrange else EditorColors.textMuted,
            modifier = Modifier.size(16.dp).clickable { onToggleLocked() }
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = name,
            style = EditorTypography.layerName,
            color = if (isSelected) Color.White else EditorColors.textPrimary,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun TooltipIconButton(icon: ImageVector, tooltip: String, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    var showTooltip by remember { mutableStateOf(false) }

    LaunchedEffect(isHovered) {
        if (isHovered) {
            delay(600)
            showTooltip = true
        } else {
            showTooltip = false
        }
    }

    Box(contentAlignment = Alignment.Center) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.size(28.dp).hoverable(interactionSource),
            interactionSource = interactionSource
        ) {
            Icon(icon, null, tint = EditorColors.textSecondary, modifier = Modifier.size(16.dp))
        }
        
        if (showTooltip) {
            Surface(
                modifier = Modifier.offset(y = 30.dp).zIndex(100f),
                color = EditorColors.darkSurfaceLight,
                shape = RoundedCornerShape(4.dp),
                elevation = 4.dp,
                border = BorderStroke(1.dp, EditorColors.dividerColor)
            ) {
                Text(tooltip, color = Color.White, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
            }
        }
    }
}

@Composable
private fun ActionButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.height(28.dp).padding(horizontal = 2.dp),
        contentPadding = PaddingValues(0.dp),
        colors = ButtonDefaults.outlinedButtonColors(backgroundColor = EditorColors.buttonColor),
        border = BorderStroke(1.dp, EditorColors.dividerColor)
    ) {
        Icon(icon, null, tint = EditorColors.textSecondary, modifier = Modifier.size(14.dp))
    }
}
