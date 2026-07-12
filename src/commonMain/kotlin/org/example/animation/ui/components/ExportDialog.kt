package org.example.animation.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.animation.engine.AnimationEngine
import org.example.animation.localization.EditorStrings
import org.example.animation.ui.theme.EditorColors

@Composable
fun ExportDialog(
    engine: AnimationEngine,
    format: String,
    onCancel: () -> Unit,
    onExport: (name: String, width: Int, height: Int, format: String) -> Unit
) {
    val project by engine.project.collectAsState()
    var fileName by remember { mutableStateOf(project.name) }
    var exportWidth by remember { mutableStateOf(project.canvasWidth.toString()) }
    var exportHeight by remember { mutableStateOf(project.canvasHeight.toString()) }
    var selectedFormat by remember { mutableStateOf(format) }

    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.width(450.dp),
            color = EditorColors.darkSurface,
            shape = RoundedCornerShape(12.dp),
            elevation = 16.dp
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = EditorStrings.observeString("export.title"),
                    color = EditorColors.accentBlue,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(Modifier.height(24.dp))

                Text(EditorStrings.observeString("project.name"), color = EditorColors.textSecondary, fontSize = 12.sp)
                OutlinedTextField(
                    value = fileName,
                    onValueChange = { fileName = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = EditorColors.accentBlue,
                        textColor = EditorColors.textPrimary,
                        backgroundColor = EditorColors.darkSurfaceVariant
                    )
                )

                Spacer(Modifier.height(16.dp))

                Row(Modifier.fillMaxWidth()) {
                    Column(Modifier.weight(1f)) {
                        Text(EditorStrings.observeString("project.width"), color = EditorColors.textSecondary, fontSize = 12.sp)
                        OutlinedTextField(
                            value = exportWidth,
                            onValueChange = { exportWidth = it.filter { c -> c.isDigit() } },
                            singleLine = true,
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                focusedBorderColor = EditorColors.accentBlue,
                                textColor = EditorColors.textPrimary,
                                backgroundColor = EditorColors.darkSurfaceVariant
                            )
                        )
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(EditorStrings.observeString("project.height"), color = EditorColors.textSecondary, fontSize = 12.sp)
                        OutlinedTextField(
                            value = exportHeight,
                            onValueChange = { exportHeight = it.filter { c -> c.isDigit() } },
                            singleLine = true,
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                focusedBorderColor = EditorColors.accentBlue,
                                textColor = EditorColors.textPrimary,
                                backgroundColor = EditorColors.darkSurfaceVariant
                            )
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
                
                Text("Формат", color = EditorColors.textSecondary, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FormatChip("PNG", selectedFormat == "png") { selectedFormat = "png" }
                    FormatChip("GIF", selectedFormat == "gif") { selectedFormat = "gif" }
                    FormatChip("AVI", selectedFormat == "avi") { selectedFormat = "avi" }
                }

                Spacer(Modifier.height(32.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onCancel) {
                        Text(EditorStrings.observeString("cancel"), color = EditorColors.textSecondary)
                    }
                    Spacer(Modifier.width(16.dp))
                    Button(
                        onClick = {
                            onExport(
                                fileName,
                                exportWidth.toIntOrNull() ?: project.canvasWidth,
                                exportHeight.toIntOrNull() ?: project.canvasHeight,
                                selectedFormat
                            )
                        },
                        colors = ButtonDefaults.buttonColors(backgroundColor = EditorColors.accentBlue)
                    ) {
                        Text(EditorStrings.observeString("export.perform"), color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun FormatChip(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable { onClick() },
        color = if (isSelected) EditorColors.accentBlue.copy(alpha = 0.2f) else EditorColors.buttonColor,
        border = BorderStroke(1.dp, if (isSelected) EditorColors.accentBlue else EditorColors.dividerColor),
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(text, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), color = if (isSelected) EditorColors.accentBlue else EditorColors.textSecondary, fontSize = 12.sp)
    }
}
