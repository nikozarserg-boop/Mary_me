package org.example.animation.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.animation.engine.AnimationEngine
import org.example.animation.localization.EditorStrings
import org.example.animation.model.BrushManager
import org.example.animation.model.BrushPreset
import org.example.animation.ui.theme.EditorColors
import org.example.animation.ui.theme.EditorIcons
import org.example.animation.ui.theme.EditorTypography
import kotlin.math.*

@Composable
fun ColorPicker(engine: AnimationEngine, modifier: Modifier = Modifier) {
    val currentColorULong by engine.currentColor.collectAsState()
    val brushSize by engine.brushSize.collectAsState()
    val opacity by engine.opacity.collectAsState()

    var hsv by remember { mutableStateOf(colorToHSV(ulongToColor(currentColorULong))) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(EditorColors.panelBackground)
            .padding(12.dp)
    ) {
        Text(
            EditorStrings.observeString("panel.color").uppercase(),
            style = EditorTypography.panelTitle,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // HSV Picker Area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.1f)
                .padding(bottom = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            HueCircle(
                hue = hsv[0],
                onHueChange = { 
                    hsv = floatArrayOf(it, hsv[1], hsv[2])
                    engine.setCurrentColor(hsvToColor(hsv).toULong())
                }
            )
            
            SaturationValueBox(
                hue = hsv[0],
                saturation = hsv[1],
                value = hsv[2],
                onValueChange = { s, v -> 
                    hsv = floatArrayOf(hsv[0], s, v)
                    engine.setCurrentColor(hsvToColor(hsv).toULong())
                },
                modifier = Modifier.fillMaxSize(0.55f)
            )
        }

        // Preview & Values
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
        ) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, EditorColors.dividerColor),
                elevation = 2.dp
            ) {
                Box(modifier = Modifier.fillMaxSize().background(ulongToColor(currentColorULong).copy(alpha = opacity)))
            }
            
            Spacer(Modifier.width(12.dp))
            
            Column {
                Text(
                    "#${currentColorULong.toString(16).padStart(8, '0').uppercase().takeLast(6)}",
                    color = EditorColors.textPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
                Text(
                    "A: ${(opacity * 100).toInt()}% | ${brushSize.toInt()}px",
                    color = EditorColors.textSecondary,
                    fontSize = 10.sp
                )
            }
        }

        Divider(color = EditorColors.dividerColor, modifier = Modifier.padding(bottom = 12.dp))

        // Brush Presets Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                EditorStrings.observeString("brush.presets").uppercase(),
                style = EditorTypography.panelTitle.copy(fontSize = 10.sp)
            )
            IconButton(
                onClick = {
                    BrushManager.addPreset(BrushPreset("New", brushSize, opacity, color = currentColorULong))
                },
                modifier = Modifier.size(20.dp).pointerHoverIcon(PointerIcon.Hand)
            ) {
                Icon(EditorIcons.iconAdd, null, tint = EditorColors.accentBlue, modifier = Modifier.size(16.dp))
            }
        }
        
        // Presets Grid with Fixed weight conflict
        val presets = BrushManager.getPresets()
        Box(modifier = Modifier.weight(1f)) {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                presets.chunked(3).forEach { row ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        row.forEach { preset ->
                            Box(modifier = Modifier.weight(1f)) {
                                BrushPresetItem(
                                    preset = preset,
                                    isSelected = BrushManager.getCurrent() == preset,
                                    onClick = {
                                        engine.setBrushSize(preset.size)
                                        engine.setOpacity(preset.opacity)
                                        engine.setCurrentColor(preset.color)
                                        BrushManager.setCurrent(BrushManager.getPresets().indexOf(preset))
                                    }
                                )
                            }
                        }
                        // Placeholders
                        if (row.size < 3) {
                            repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Sliders
        BrushSettingSlider(EditorStrings.observeString("brush.size"), brushSize, 1f..100f) { engine.setBrushSize(it) }
        BrushSettingSlider(EditorStrings.observeString("brush.opacity"), opacity, 0f..1f) { engine.setOpacity(it) }
    }
}

@Composable
private fun BrushPresetItem(preset: BrushPreset, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(34.dp)
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable { onClick() },
        color = if (isSelected) EditorColors.selectionColor else EditorColors.darkSurfaceVariant,
        shape = RoundedCornerShape(4.dp),
        border = BorderStroke(1.dp, if (isSelected) EditorColors.accentBlue else EditorColors.dividerColor)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(2.dp)) {
            Text(
                preset.name, 
                fontSize = 9.sp, 
                color = if (isSelected) Color.White else EditorColors.textPrimary, 
                maxLines = 1, 
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun HueCircle(hue: Float, onHueChange: (Float) -> Unit) {
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerHoverIcon(PointerIcon.Hand)
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val angle = atan2(change.position.y - center.y, change.position.x - center.x) * 180 / PI
                    var newHue = angle.toFloat()
                    if (newHue < 0) newHue += 360f
                    onHueChange(newHue)
                }
            }
    ) {
        val radius = size.minDimension / 2f
        val thickness = 14.dp.toPx()
        val sweepGradient = Brush.sweepGradient(
            colors = listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red),
            center = center
        )
        drawCircle(brush = sweepGradient, radius = radius - thickness / 2, style = Stroke(width = thickness))
        val angleRad = (hue) * PI / 180f
        val indicatorPos = Offset(center.x + (radius - thickness / 2) * cos(angleRad).toFloat(), center.y + (radius - thickness / 2) * sin(angleRad).toFloat())
        drawCircle(Color.White, radius = 5.dp.toPx(), center = indicatorPos)
        drawCircle(Color.Black, radius = 4.dp.toPx(), center = indicatorPos, style = Stroke(1.5.dp.toPx()))
    }
}

@Composable
fun SaturationValueBox(hue: Float, saturation: Float, value: Float, onValueChange: (Float, Float) -> Unit, modifier: Modifier) {
    Canvas(
        modifier = modifier
            .pointerHoverIcon(PointerIcon.Hand)
            .pointerInput(hue) {
                detectDragGestures { change, _ ->
                    val s = (change.position.x / size.width).coerceIn(0f, 1f)
                    val v = 1f - (change.position.y / size.height).coerceIn(0f, 1f)
                    onValueChange(s, v)
                }
            }
    ) {
        val baseColor = Color.hsv(hue, 1f, 1f)
        drawRect(brush = Brush.horizontalGradient(listOf(Color.White, baseColor)))
        drawRect(brush = Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
        val pos = Offset(saturation * size.width, (1f - value) * size.height)
        drawCircle(if (value > 0.5f) Color.Black else Color.White, radius = 4.dp.toPx(), center = pos, style = Stroke(1.dp.toPx()))
    }
}

@Composable
private fun BrushSettingSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onValueChange: (Float) -> Unit) {
    Column(modifier = Modifier.padding(vertical = 2.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = EditorColors.textSecondary, fontSize = 10.sp)
            Text(if (range.endInclusive == 1f) "${(value * 100).toInt()}%" else value.toInt().toString(), color = EditorColors.accentBlue, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
        Slider(value = value, onValueChange = onValueChange, valueRange = range, modifier = Modifier.height(20.dp), colors = SliderDefaults.colors(thumbColor = EditorColors.accentBlue, activeTrackColor = EditorColors.accentBlue, inactiveTrackColor = EditorColors.darkSurfaceVariant))
    }
}

private fun colorToHSV(color: Color): FloatArray {
    val r = color.red; val g = color.green; val b = color.blue
    val max = maxOf(r, maxOf(g, b)); val min = minOf(r, minOf(g, b)); val delta = max - min
    var h = 0f
    if (delta > 0) { h = when (max) { r -> (g - b) / delta % 6; g -> (b - r) / delta + 2; else -> (r - g) / delta + 4 } * 60 }
    if (h < 0) h += 360f
    val s = if (max == 0f) 0f else delta / max; val v = max
    return floatArrayOf(h, s, v)
}

private fun hsvToColor(hsv: FloatArray): Color = Color.hsv(hsv[0], hsv[1], hsv[2])

private fun ulongToColor(color: ULong): Color {
    val a = ((color shr 24) and 0xFFuL).toInt() / 255f
    val r = ((color shr 16) and 0xFFuL).toInt() / 255f
    val g = ((color shr 8) and 0xFFuL).toInt() / 255f
    val b = (color and 0xFFuL).toInt() / 255f
    return Color(r, g, b, a)
}

private fun Color.toULong(): ULong {
    val a = (alpha * 255).toInt().toULong(); val r = (red * 255).toInt().toULong()
    val g = (green * 255).toInt().toULong(); val b = (blue * 255).toInt().toULong()
    return (a shl 24) or (r shl 16) or (g shl 8) or b
}
