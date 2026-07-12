package org.example.animation.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import org.example.animation.model.AnimationProject
import org.example.animation.ui.theme.EditorColors

@Composable
fun NewProjectDialog(onCancel: () -> Unit, onCreate: (AnimationProject) -> Unit) {
    var name by remember { mutableStateOf("Новый проект") }
    var width by remember { mutableStateOf("800") }
    var height by remember { mutableStateOf("600") }
    var fps by remember { mutableStateOf("24") }
    var layerCount by remember { mutableStateOf("1") }
    var preset by remember { mutableStateOf(4) }

    val presets = listOf(
        "HD 1280×720" to Pair(1280, 720),
        "Full HD 1920×1080" to Pair(1920, 1080),
        "Квадрат 1080×1080" to Pair(1080, 1080),
        "Мобильный 1080×1920" to Pair(1080, 1920),
        "Стандарт 800×600" to Pair(800, 600),
        "A4 2480×3508" to Pair(2480, 3508)
    )

    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)).clickable { onCancel() }, contentAlignment = Alignment.Center) {
        Surface(modifier = Modifier.width(480.dp).clip(RoundedCornerShape(8.dp)), color = EditorColors.darkSurface, elevation = 8.dp) {
            Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                Text("Новый проект", color = EditorColors.accentBlue, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))

                Text("Название проекта", color = EditorColors.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                OutlinedTextField(value = name, onValueChange = { name = it }, singleLine = true, modifier = Modifier.fillMaxWidth().height(38.dp), textStyle = LocalTextStyle.current.copy(fontSize = 13.sp, color = EditorColors.textPrimary), colors = TextFieldDefaults.outlinedTextFieldColors(focusedBorderColor = EditorColors.accentBlue, unfocusedBorderColor = EditorColors.dividerColor, cursorColor = EditorColors.accentBlue, backgroundColor = EditorColors.darkSurfaceVariant))
                Spacer(Modifier.height(12.dp))

                Text("Пресеты размера", color = EditorColors.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(presets.size) { i ->
                        val p = presets[i]
                        Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(if (preset == i) EditorColors.accentBlue.copy(alpha = 0.3f) else EditorColors.buttonColor).border(0.5.dp, if (preset == i) EditorColors.accentBlue else EditorColors.dividerColor, RoundedCornerShape(4.dp)).clickable {
                            preset = i
                            width = p.second.first.toString()
                            height = p.second.second.toString()
                        }.padding(horizontal = 10.dp, vertical = 6.dp)) {
                            Text(p.first, color = if (preset == i) EditorColors.accentBlue else EditorColors.textSecondary, fontSize = 11.sp)
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))

                Row(Modifier.fillMaxWidth()) {
                    Column(Modifier.weight(1f)) {
                        Text("Ширина (px)", color = EditorColors.textSecondary, fontSize = 11.sp)
                        Spacer(Modifier.height(4.dp))
                        OutlinedTextField(value = width, onValueChange = { width = it.filter { c -> c.isDigit() }.take(5) }, singleLine = true, modifier = Modifier.fillMaxWidth().height(38.dp), textStyle = LocalTextStyle.current.copy(fontSize = 13.sp, color = EditorColors.textPrimary), colors = TextFieldDefaults.outlinedTextFieldColors(focusedBorderColor = EditorColors.accentBlue, unfocusedBorderColor = EditorColors.dividerColor, cursorColor = EditorColors.accentBlue, backgroundColor = EditorColors.darkSurfaceVariant))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Высота (px)", color = EditorColors.textSecondary, fontSize = 11.sp)
                        Spacer(Modifier.height(4.dp))
                        OutlinedTextField(value = height, onValueChange = { height = it.filter { c -> c.isDigit() }.take(5) }, singleLine = true, modifier = Modifier.fillMaxWidth().height(38.dp), textStyle = LocalTextStyle.current.copy(fontSize = 13.sp, color = EditorColors.textPrimary), colors = TextFieldDefaults.outlinedTextFieldColors(focusedBorderColor = EditorColors.accentBlue, unfocusedBorderColor = EditorColors.dividerColor, cursorColor = EditorColors.accentBlue, backgroundColor = EditorColors.darkSurfaceVariant))
                    }
                }
                Spacer(Modifier.height(12.dp))

                Row(Modifier.fillMaxWidth()) {
                    Column(Modifier.weight(1f)) {
                        Text("FPS (1-60)", color = EditorColors.textSecondary, fontSize = 11.sp)
                        Spacer(Modifier.height(4.dp))
                        OutlinedTextField(value = fps, onValueChange = { fps = it.filter { c -> c.isDigit() }.take(2) }, singleLine = true, modifier = Modifier.fillMaxWidth().height(38.dp), textStyle = LocalTextStyle.current.copy(fontSize = 13.sp, color = EditorColors.textPrimary), colors = TextFieldDefaults.outlinedTextFieldColors(focusedBorderColor = EditorColors.accentBlue, unfocusedBorderColor = EditorColors.dividerColor, cursorColor = EditorColors.accentBlue, backgroundColor = EditorColors.darkSurfaceVariant))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Слоёв (1-10)", color = EditorColors.textSecondary, fontSize = 11.sp)
                        Spacer(Modifier.height(4.dp))
                        OutlinedTextField(value = layerCount, onValueChange = { layerCount = it.filter { c -> c.isDigit() }.take(2) }, singleLine = true, modifier = Modifier.fillMaxWidth().height(38.dp), textStyle = LocalTextStyle.current.copy(fontSize = 13.sp, color = EditorColors.textPrimary), colors = TextFieldDefaults.outlinedTextFieldColors(focusedBorderColor = EditorColors.accentBlue, unfocusedBorderColor = EditorColors.dividerColor, cursorColor = EditorColors.accentBlue, backgroundColor = EditorColors.darkSurfaceVariant))
                    }
                }
                Spacer(Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Button(onClick = onCancel, colors = ButtonDefaults.buttonColors(backgroundColor = EditorColors.buttonColor), modifier = Modifier.height(36.dp)) {
                        Text("Отмена", color = EditorColors.textPrimary, fontSize = 13.sp)
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = {
                        val w = width.toIntOrNull()?.coerceIn(1, 8192) ?: 800
                        val h = height.toIntOrNull()?.coerceIn(1, 8192) ?: 600
                        val f = fps.toIntOrNull()?.coerceIn(1, 60) ?: 24
                        val lc = layerCount.toIntOrNull()?.coerceIn(1, 10) ?: 1
                        val project = AnimationProject(name = name.ifBlank { "Новый проект" }, canvasWidth = w, canvasHeight = h, fps = f)
                        repeat(lc - 1) { project.addLayer("Слой ${it + 2}") }
                        onCreate(project)
                    }, colors = ButtonDefaults.buttonColors(backgroundColor = EditorColors.accentBlue), modifier = Modifier.height(36.dp)) {
                        Text("Создать", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}