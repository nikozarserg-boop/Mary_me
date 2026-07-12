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
import org.example.animation.localization.EditorStrings

@Composable
fun NewProjectDialog(onCancel: () -> Unit, onCreate: (AnimationProject) -> Unit) {
    var name by remember { mutableStateOf("") }
    var width by remember { mutableStateOf("1280") }
    var height by remember { mutableStateOf("720") }
    var fps by remember { mutableStateOf("24") }
    var layerCount by remember { mutableStateOf("1") }
    var preset by remember { mutableIntStateOf(0) }

    val presets = listOf(
        "HD 1280×720" to Pair(1280, 720),
        "Full HD 1920×1080" to Pair(1920, 1080),
        "Square 1080×1080" to Pair(1080, 1080),
        "Mobile 1080×1920" to Pair(1080, 1920),
        "Standard 800×600" to Pair(800, 600),
        "Cinema 4K" to Pair(4096, 2160)
    )

    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)).clickable { onCancel() }, contentAlignment = Alignment.Center) {
        Surface(
            modifier = Modifier.width(520.dp).clickable(enabled = false) {}, 
            color = EditorColors.darkSurface, 
            shape = RoundedCornerShape(12.dp),
            elevation = 16.dp
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = EditorStrings.observeString("dialog.newProject"), 
                    color = EditorColors.accentBlue, 
                    fontSize = 20.sp, 
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(20.dp))

                // Project Name
                Text(EditorStrings.observeString("project.name"), color = EditorColors.textSecondary, fontSize = 12.sp)
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = name, 
                    onValueChange = { name = it }, 
                    singleLine = true, 
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    placeholder = { Text("Untitled Project", color = EditorColors.textMuted) },
                    textStyle = LocalTextStyle.current.copy(fontSize = 14.sp, color = EditorColors.textPrimary), 
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = EditorColors.accentBlue, 
                        unfocusedBorderColor = EditorColors.dividerColor, 
                        textColor = EditorColors.textPrimary, // Fixed text visibility
                        cursorColor = EditorColors.accentBlue, 
                        backgroundColor = EditorColors.darkSurfaceVariant
                    )
                )
                
                Spacer(Modifier.height(16.dp))

                // Presets
                Text("Размер", color = EditorColors.textSecondary, fontSize = 12.sp)
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(presets.size) { i ->
                        val p = presets[i]
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable {
                                    preset = i
                                    width = p.second.first.toString()
                                    height = p.second.second.toString()
                                },
                            color = if (preset == i) EditorColors.accentBlue.copy(alpha = 0.2f) else EditorColors.buttonColor,
                            border = BorderStroke(1.dp, if (preset == i) EditorColors.accentBlue else EditorColors.dividerColor)
                        ) {
                            Text(
                                text = p.first, 
                                color = if (preset == i) EditorColors.accentBlue else EditorColors.textSecondary, 
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
                
                Spacer(Modifier.height(20.dp))

                // Width and Height (allowing any integer)
                Row(Modifier.fillMaxWidth()) {
                    ProjectField(EditorStrings.observeString("project.width"), width, Modifier.weight(1f)) { width = it }
                    Spacer(Modifier.width(16.dp))
                    ProjectField(EditorStrings.observeString("project.height"), height, Modifier.weight(1f)) { height = it }
                }
                
                Spacer(Modifier.height(16.dp))

                // FPS and Layers
                Row(Modifier.fillMaxWidth()) {
                    ProjectField("FPS", fps, Modifier.weight(1f)) { fps = it }
                    Spacer(Modifier.width(16.dp))
                    ProjectField(EditorStrings.observeString("project.layers"), layerCount, Modifier.weight(1f)) { layerCount = it }
                }

                Spacer(Modifier.height(32.dp))

                // Buttons
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onCancel, modifier = Modifier.height(44.dp)) {
                        Text(EditorStrings.observeString("cancel"), color = EditorColors.textSecondary)
                    }
                    Spacer(Modifier.width(16.dp))
                    Button(
                        onClick = {
                            val w = width.toIntOrNull() ?: 1280
                            val h = height.toIntOrNull() ?: 720
                            val f = fps.toIntOrNull() ?: 24
                            val lc = layerCount.toIntOrNull() ?: 1
                            val project = AnimationProject(
                                name = name.ifBlank { "Untitled" }, 
                                canvasWidth = w, 
                                canvasHeight = h, 
                                fps = f
                            )
                            project.layers.clear()
                            repeat(lc) { project.addLayer("Layer ${it + 1}") }
                            onCreate(project)
                        }, 
                        colors = ButtonDefaults.buttonColors(backgroundColor = EditorColors.accentBlue), 
                        modifier = Modifier.height(44.dp).width(120.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(EditorStrings.observeString("btn.create"), color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun ProjectField(label: String, value: String, modifier: Modifier, onValueChange: (String) -> Unit) {
    Column(modifier) {
        Text(label, color = EditorColors.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(
            value = value, 
            onValueChange = { onValueChange(it.filter { c -> c.isDigit() }.take(5)) }, 
            singleLine = true, 
            modifier = Modifier.fillMaxWidth().height(56.dp),
            textStyle = LocalTextStyle.current.copy(fontSize = 14.sp, color = EditorColors.textPrimary),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = EditorColors.accentBlue, 
                unfocusedBorderColor = EditorColors.dividerColor, 
                textColor = EditorColors.textPrimary, // Fixed text visibility
                cursorColor = EditorColors.accentBlue,
                backgroundColor = EditorColors.darkSurfaceVariant
            )
        )
    }
}
