package org.example.animation.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.animation.engine.AnimationEngine
import org.example.animation.model.ToolType
import org.example.animation.localization.EditorStrings
import org.example.animation.ui.theme.*
import kotlin.math.roundToInt

@Composable
fun ToolPropertiesPanel(engine: AnimationEngine) {
    val currentTool by engine.currentTool.collectAsState()
    val brushSize by engine.brushSize.collectAsState()
    val opacity by engine.opacity.collectAsState()
    val smoothingLevel by engine.smoothingLevel.collectAsState()
    val antiAliasing by engine.antiAliasingEnabled.collectAsState()

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
