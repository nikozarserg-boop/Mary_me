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
import org.example.animation.ui.theme.EditorColors
import org.example.animation.ui.theme.EditorIcons
import org.example.animation.ui.theme.EditorTypography

@Composable
fun ToolPropertiesPanel(engine: AnimationEngine) {
    val currentTool by engine.currentTool.collectAsState()
    val brushSize by engine.brushSize.collectAsState()
    val opacity by engine.opacity.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(EditorColors.panelBackground)
            .padding(12.dp)
    ) {
        Text(
            text = EditorStrings.observeString("panel.tools").uppercase(),
            style = EditorTypography.panelTitle(),
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Tool Icon and Name
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(EditorColors.darkSurfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    getToolIcon(currentTool),
                    null,
                    tint = EditorColors.accentBlue,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = getToolName(currentTool),
                color = EditorColors.textPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(Modifier.height(24.dp))

        // Properties common for most tools
        if (currentTool != ToolType.MOVE && currentTool != ToolType.EYEDROPPER) {
            PropertySlider(
                label = EditorStrings.observeString("brush.size"),
                value = brushSize,
                range = 1f..100f,
                onValueChange = { engine.setBrushSize(it) }
            )

            Spacer(Modifier.height(16.dp))

            PropertySlider(
                label = EditorStrings.observeString("brush.opacity"),
                value = opacity,
                range = 0f..1f,
                onValueChange = { engine.setOpacity(it) }
            )
        }

        Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun PropertySlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, color = EditorColors.textSecondary, fontSize = 11.sp)
            Text(
                if (range.endInclusive == 1f) "${(value * 100).toInt()}%" else value.toInt().toString(),
                color = EditorColors.accentBlue,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = EditorColors.accentBlue,
                activeTrackColor = EditorColors.accentBlue,
                inactiveTrackColor = EditorColors.darkSurfaceVariant
            )
        )
    }
}

private fun getToolIcon(tool: ToolType): androidx.compose.ui.graphics.vector.ImageVector {
    return when (tool) {
        ToolType.BRUSH -> EditorIcons.iconBrush
        ToolType.PENCIL -> EditorIcons.iconPencil
        ToolType.ERASER -> EditorIcons.iconEraser
        ToolType.LINE -> EditorIcons.iconLine
        ToolType.RECTANGLE -> EditorIcons.iconRectangle
        ToolType.ELLIPSE -> EditorIcons.iconEllipse
        ToolType.FILL -> EditorIcons.iconFill
        ToolType.EYEDROPPER -> EditorIcons.iconEyedropper
        ToolType.SELECT -> EditorIcons.iconSelect
        ToolType.MOVE -> EditorIcons.iconMove
        else -> EditorIcons.iconBrush
    }
}

@Composable
private fun getToolName(tool: ToolType): String {
    return EditorStrings.observeString("tool.${tool.name.lowercase()}")
}
