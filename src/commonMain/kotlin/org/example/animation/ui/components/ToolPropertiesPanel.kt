package org.example.animation.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.animation.engine.AnimationEngine
import org.example.animation.model.ToolType
import org.example.animation.localization.EditorStrings
import org.example.animation.ui.theme.*
import org.example.animation.io.decodeImage
import org.example.animation.model.BrushPreset
import org.example.animation.model.BrushShape
import kotlin.math.roundToInt

@Composable
fun ToolPropertiesPanel(
    engine: AnimationEngine,
    onImportBrush: (() -> Unit)? = null,
    onExportBrush: (() -> Unit)? = null
) {
    val currentTool by engine.currentTool.collectAsState()
    val brushSize by engine.brushSize.collectAsState()
    val opacity by engine.opacity.collectAsState()
    val smoothingLevel by engine.smoothingLevel.collectAsState()
    val antiAliasing by engine.antiAliasingEnabled.collectAsState()
    val brushes by engine.brushes.collectAsState()
    val currentBrushIndex by engine.currentBrushIndex.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(EditorColors.panelBackground)
            .verticalScroll(rememberScrollState())
            .padding(UiDimensions.PaddingMedium.scaled())
    ) {
        // Иконка и название инструмента
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(UiDimensions.IconButtonSize.scaled())
                    .clip(RoundedCornerShape(4.dp.scaled()))
                    .background(EditorColors.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    getToolIcon(currentTool),
                    null,
                    tint = EditorColors.accent,
                    modifier = Modifier.size(UiDimensions.IconSize.scaled())
                )
            }
            Spacer(Modifier.width(UiDimensions.PaddingMedium.scaled()))
            Text(
                text = EditorStrings.observeString(currentTool.localizationKey),
                style = EditorTypography.body().copy(fontWeight = FontWeight.Medium)
            )
        }

        Spacer(Modifier.height(UiDimensions.PaddingLarge.scaled()))

        // Свойства для большинства инструментов
        if (currentTool != ToolType.MOVE && currentTool != ToolType.EYEDROPPER) {
            PropertySlider(
                label = EditorStrings.observeString("brush.size"),
                value = brushSize,
                range = 1f..100f,
                onValueChange = { engine.setBrushSize(it) }
            )

            Spacer(Modifier.height(UiDimensions.PaddingMedium.scaled()))

            PropertySlider(
                label = EditorStrings.observeString("brush.opacity"),
                value = opacity,
                range = 0f..1f,
                onValueChange = { engine.setOpacity(it) }
            )

            Spacer(Modifier.height(UiDimensions.PaddingMedium.scaled()))

            // Уровень сглаживания
            Column(modifier = Modifier.padding(vertical = 4.dp.scaled())) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(EditorStrings.observeString("brush.smoothing"), style = EditorTypography.caption())
                    Text(
                        EditorStrings.observeString("brush.smoothing.$smoothingLevel"),
                        color = EditorColors.accent,
                        style = EditorTypography.mono().copy(fontWeight = FontWeight.Bold)
                    )
                }
                Slider(
                    value = smoothingLevel.toFloat(),
                    onValueChange = { engine.setSmoothingLevel(it.roundToInt()) },
                    valueRange = 0f..3f,
                    steps = 2,
                    modifier = Modifier.height(32.dp.scaled()),
                    colors = SliderDefaults.colors(
                        thumbColor = EditorColors.accent,
                        activeTrackColor = EditorColors.accent,
                        inactiveTrackColor = EditorColors.surfaceVariant
                    )
                )
            }
            
            Spacer(Modifier.height(UiDimensions.PaddingLarge.scaled()))

            // Библиотека кистей
            BrushesSection(engine, brushes, currentBrushIndex)
        }

        Spacer(Modifier.height(UiDimensions.PaddingLarge.scaled()))
        
        // Дополнительные параметры
        Text(
            text = EditorStrings.observeString("settings.performance"), 
            style = EditorTypography.caption(),
            modifier = Modifier.padding(bottom = 8.dp.scaled())
        )
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable { engine.setAntiAliasingEnabled(!antiAliasing) }
        ) {
            Checkbox(
                checked = antiAliasing, 
                onCheckedChange = { engine.setAntiAliasingEnabled(it) }, 
                colors = CheckboxDefaults.colors(checkedColor = EditorColors.accent),
                modifier = Modifier.size(24.dp.scaled())
            )
            Spacer(Modifier.width(8.dp.scaled()))
            Text(EditorStrings.observeString("brush.antiAliasing"), style = EditorTypography.body())
        }
    }
}

@Composable
private fun BrushesSection(
    engine: AnimationEngine,
    brushes: List<BrushPreset>,
    currentIndex: Int,
    onImportBrush: (() -> Unit)? = null,
    onExportBrush: (() -> Unit)? = null
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = EditorStrings.observeString("brush.library"),
                style = EditorTypography.caption().copy(fontWeight = FontWeight.Bold)
            )
            Row {
                IconButton(onClick = { onImportBrush?.invoke() ?: engine.importBrushesFromFile() }, modifier = Modifier.size(24.dp.scaled())) {
                    Icon(Icons.Default.FileUpload, EditorStrings.observeString("brush.import"), tint = EditorColors.accent, modifier = Modifier.size(16.dp.scaled()))
                }
                IconButton(onClick = { onExportBrush?.invoke() ?: engine.exportCurrentBrushToFile() }, modifier = Modifier.size(24.dp.scaled())) {
                    Icon(Icons.Default.FileDownload, EditorStrings.observeString("brush.export"), tint = EditorColors.accent, modifier = Modifier.size(16.dp.scaled()))
                }
                if (onImportBrush == null) {
                    IconButton(onClick = { engine.openBrushStore() }, modifier = Modifier.size(24.dp.scaled())) {
                        Icon(Icons.Default.ShoppingCart, EditorStrings.observeString("store.title"), tint = EditorColors.accent, modifier = Modifier.size(16.dp.scaled()))
                    }
                }
            }
        }
        
        Spacer(Modifier.height(8.dp.scaled()))
        
        // Сетка кистей
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 300.dp.scaled())
                .clip(RoundedCornerShape(8.dp.scaled()))
                .background(EditorColors.surfaceVariant)
                .padding(4.dp.scaled())
        ) {
            // Используем Column + Row вместо LazyVerticalGrid внутри Scrollable Column
            val columns = 4
            val rows = (brushes.size + columns - 1) / columns
            
            for (row in 0 until rows) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    for (col in 0 until columns) {
                        val index = row * columns + col
                        if (index < brushes.size) {
                            val brush = brushes[index]
                            BrushItem(
                                brush = brush,
                                isSelected = index == currentIndex,
                                onClick = { engine.selectBrush(index) },
                                onDelete = if (brushes.size > 1) { { engine.removeBrush(index) } } else null,
                                modifier = Modifier.weight(1f).padding(2.dp.scaled())
                            )
                        } else {
                            Spacer(Modifier.weight(1f).padding(2.dp.scaled()))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BrushItem(
    brush: BrushPreset,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDelete: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isSelected) EditorColors.accent else Color.Transparent
    
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(4.dp.scaled()))
            .background(if (isSelected) EditorColors.accent.copy(alpha = 0.1f) else EditorColors.surface)
            .border(1.dp.scaled(), borderColor, RoundedCornerShape(4.dp.scaled()))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (brush.shape == BrushShape.TEXTURE && brush.stampPng != null) {
            val bitmap = remember(brush.stampPng) { decodeImage(brush.stampPng) }
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = brush.name,
                    modifier = Modifier.fillMaxSize().padding(4.dp.scaled()),
                    contentScale = ContentScale.Fit
                )
            }
        } else {
            // Заглушка для стандартных кистей
            Box(
                modifier = Modifier
                    .size(24.dp.scaled())
                    .clip(if (brush.shape == BrushShape.SQUARE) RoundedCornerShape(2.dp.scaled()) else RoundedCornerShape(12.dp.scaled()))
                    .background(EditorColors.textPrimary.copy(alpha = 0.6f))
            )
        }
        
        if (isSelected && onDelete != null) {
            IconButton(
                onClick = onDelete,
                modifier = Modifier.align(Alignment.TopEnd).size(16.dp.scaled()).background(Color.Red.copy(alpha = 0.7f), RoundedCornerShape(bottomStart = 4.dp.scaled()))
            ) {
                Icon(Icons.Default.Delete, null, tint = Color.White, modifier = Modifier.size(10.dp.scaled()))
            }
        }
    }
}

@Composable
private fun PropertySlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 4.dp.scaled())) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = EditorTypography.caption())
            Text(
                if (range.endInclusive == 1f) "${(value * 100).toInt()}%" else value.toInt().toString(),
                color = EditorColors.accent,
                style = EditorTypography.mono().copy(fontWeight = FontWeight.Bold)
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            modifier = Modifier.height(32.dp.scaled()),
            colors = SliderDefaults.colors(
                thumbColor = EditorColors.accent,
                activeTrackColor = EditorColors.accent,
                inactiveTrackColor = EditorColors.surfaceVariant
            )
        )
    }
}

private fun getToolIcon(tool: ToolType): androidx.compose.ui.graphics.vector.ImageVector {
    return when (tool) {
        ToolType.BRUSH -> EditorIcons.iconBrush
        ToolType.PENCIL -> EditorIcons.iconPencil
        ToolType.HARD_ERASER, ToolType.SOFT_ERASER, ToolType.PARTIAL_ERASER, ToolType.OBJECT_ERASER -> EditorIcons.iconEraser
        ToolType.LINE -> EditorIcons.iconLine
        ToolType.RECTANGLE -> EditorIcons.iconRectangle
        ToolType.ELLIPSE -> EditorIcons.iconEllipse
        ToolType.BUCKET_FILL, ToolType.GRADIENT_FILL, ToolType.PATTERN_FILL -> EditorIcons.iconFill
        ToolType.EYEDROPPER -> EditorIcons.iconEyedropper
        ToolType.RECT_SELECTION, ToolType.LASSO, ToolType.MAGIC_WAND -> EditorIcons.iconSelect
        ToolType.MOVE, ToolType.SCALE, ToolType.ROTATE, ToolType.SKEW, ToolType.MIRROR, ToolType.FLIP -> EditorIcons.iconMove
        else -> EditorIcons.iconBrush
    }
}
