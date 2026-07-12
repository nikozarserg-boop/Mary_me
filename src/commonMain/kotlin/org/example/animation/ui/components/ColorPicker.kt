package org.example.animation.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
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
            .padding(8.dp)
    ) {
        Text(
            EditorStrings.observeString("panel.color").uppercase(),
            style = EditorTypography.panelTitle(),
            fontSize = 9.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .padding(bottom = 8.dp),
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
                hue = hsv[0], saturation = hsv[1], value = hsv[2],
                onValueChange = { s, v -> 
                    hsv = floatArrayOf(hsv[0], s, v)
                    engine.setCurrentColor(hsvToColor(hsv).toULong())
                },
                modifier = Modifier.fillMaxSize(0.5f)
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
            Box(modifier = Modifier.size(24.dp).clip(RoundedCornerShape(4.dp)).background(ulongToColor(currentColorULong).copy(alpha = opacity)).border(1.dp, EditorColors.dividerColor, RoundedCornerShape(4.dp)))
            Spacer(Modifier.width(8.dp))
            Text(
                "#${currentColorULong.toString(16).padStart(8, '0').uppercase().takeLast(6)}",
                color = EditorColors.textPrimary,
                fontSize = 11.sp,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            )
        }

        Divider(color = EditorColors.dividerColor.copy(alpha = 0.3f), modifier = Modifier.padding(bottom = 8.dp))

        // Sliders
        CompactSlider(EditorStrings.observeString("brush.size"), brushSize, 1f..100f) { engine.setBrushSize(it) }
        CompactSlider(EditorStrings.observeString("brush.opacity"), opacity, 0f..1f) { engine.setOpacity(it) }
    }
}

@Composable
private fun CompactSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onValueChange: (Float) -> Unit) {
    Column(modifier = Modifier.padding(vertical = 1.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = EditorColors.textSecondary, fontSize = 9.sp)
            Text(if (range.endInclusive == 1f) "${(value * 100).toInt()}%" else value.toInt().toString(), color = EditorColors.accentBlue, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
        }
        Slider(
            value = value, onValueChange = onValueChange, valueRange = range, 
            modifier = Modifier.height(16.dp),
            colors = SliderDefaults.colors(thumbColor = EditorColors.accentBlue, activeTrackColor = EditorColors.accentBlue, inactiveTrackColor = EditorColors.dividerColor)
        )
    }
}

@Composable
fun HueCircle(hue: Float, onHueChange: (Float) -> Unit) {
    Canvas(modifier = Modifier.fillMaxSize().pointerHoverIcon(PointerIcon.Hand).pointerInput(Unit) {
        detectDragGestures { change, _ ->
            val center = Offset(size.width / 2f, size.height / 2f)
            val angle = atan2(change.position.y - center.y, change.position.x - center.x) * 180 / PI
            var newHue = angle.toFloat(); if (newHue < 0) newHue += 360f; onHueChange(newHue)
        }
    }) {
        val radius = size.minDimension / 2f; val thickness = 10.dp.toPx()
        val sweep = Brush.sweepGradient(listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red), center = center)
        drawCircle(brush = sweep, radius = radius - thickness / 2, style = Stroke(width = thickness))
        val angleRad = hue * PI / 180f
        val pos = Offset(center.x + (radius - thickness / 2) * cos(angleRad).toFloat(), center.y + (radius - thickness / 2) * sin(angleRad).toFloat())
        drawCircle(Color.White, radius = 4.dp.toPx(), center = pos)
        drawCircle(Color.Black, radius = 3.dp.toPx(), center = pos, style = Stroke(1.dp.toPx()))
    }
}

@Composable
fun SaturationValueBox(hue: Float, saturation: Float, value: Float, onValueChange: (Float, Float) -> Unit, modifier: Modifier) {
    Canvas(modifier = modifier.pointerHoverIcon(PointerIcon.Hand).pointerInput(hue) {
        detectDragGestures { change, _ ->
            onValueChange((change.position.x / size.width).coerceIn(0f, 1f), 1f - (change.position.y / size.height).coerceIn(0f, 1f))
        }
    }) {
        drawRect(brush = Brush.horizontalGradient(listOf(Color.White, Color.hsv(hue, 1f, 1f))))
        drawRect(brush = Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
        val pos = Offset(saturation * size.width, (1f - value) * size.height)
        drawCircle(if (value > 0.5f) Color.Black else Color.White, radius = 3.dp.toPx(), center = pos, style = Stroke(1.dp.toPx()))
    }
}

private fun colorToHSV(color: Color): FloatArray {
    val r = color.red; val g = color.green; val b = color.blue
    val max = maxOf(r, maxOf(g, b)); val min = minOf(r, minOf(g, b)); val delta = max - min
    var h = 0f; if (delta > 0) { h = when (max) { r -> (g - b) / delta % 6; g -> (b - r) / delta + 2; else -> (r - g) / delta + 4 } * 60 }; if (h < 0) h += 360f
    return floatArrayOf(h, if (max == 0f) 0f else delta / max, max)
}
private fun hsvToColor(hsv: FloatArray): Color = Color.hsv(hsv[0], hsv[1], hsv[2])
private fun ulongToColor(c: ULong): Color = Color((c shr 16 and 0xFFuL).toInt(), (c shr 8 and 0xFFuL).toInt(), (c and 0xFFuL).toInt(), (c shr 24 and 0xFFuL).toInt())
private fun Color.toULong(): ULong = ((alpha * 255).toULong() shl 24) or ((red * 255).toULong() shl 16) or ((green * 255).toULong() shl 8) or (blue * 255).toULong()
