package org.example.animation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
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
import org.example.animation.localization.EditorStrings
import org.example.animation.model.AnimationProject
import org.example.animation.ui.theme.*

@Composable
fun NewProjectDialog(
    onCancel: () -> Unit,
    onCreate: (AnimationProject) -> Unit
) {
    var name by remember { mutableStateOf("Untitled") }
    var width by remember { mutableStateOf("1280") }
    var height by remember { mutableStateOf("720") }
    var fps by remember { mutableStateOf("24") }

    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)).clickable { onCancel() },
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.width(360.dp.scaled()).clickable(enabled = false) {},
            color = EditorColors.surface,
            shape = EditorShapes.large,
            elevation = 16.dp.scaled(),
            border = BorderStroke(1.dp.scaled(), EditorColors.divider)
        ) {
            Column(modifier = Modifier.padding(20.dp.scaled())) {
                Text(
                    EditorStrings.observeString("dialog.newProject"), 
                    style = EditorTypography.body().copy(fontSize = 18.sp.scaled(), fontWeight = FontWeight.Bold)
                )
                
                Spacer(Modifier.height(20.dp.scaled()))

                Text(EditorStrings.observeString("project.name"), style = EditorTypography.caption())
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth().height(36.dp.scaled()),
                    textStyle = EditorTypography.body(),
                    singleLine = true,
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = EditorColors.accent,
                        backgroundColor = EditorColors.background,
                        unfocusedBorderColor = EditorColors.divider
                    )
                )

                Spacer(Modifier.height(16.dp.scaled()))

                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(EditorStrings.observeString("project.width"), style = EditorTypography.caption())
                        OutlinedTextField(
                            value = width,
                            onValueChange = { width = it.filter { c -> c.isDigit() } },
                            modifier = Modifier.fillMaxWidth().height(36.dp.scaled()),
                            textStyle = EditorTypography.mono(),
                            singleLine = true,
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                focusedBorderColor = EditorColors.accent,
                                backgroundColor = EditorColors.background
                            )
                        )
                    }
                    Spacer(Modifier.width(12.dp.scaled()))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(EditorStrings.observeString("project.height"), style = EditorTypography.caption())
                        OutlinedTextField(
                            value = height,
                            onValueChange = { height = it.filter { c -> c.isDigit() } },
                            modifier = Modifier.fillMaxWidth().height(36.dp.scaled()),
                            textStyle = EditorTypography.mono(),
                            singleLine = true,
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                focusedBorderColor = EditorColors.accent,
                                backgroundColor = EditorColors.background
                            )
                        )
                    }
                }

                Spacer(Modifier.height(16.dp.scaled()))

                Text(EditorStrings.observeString("canvas.fps"), style = EditorTypography.caption())
                OutlinedTextField(
                    value = fps,
                    onValueChange = { fps = it.filter { c -> c.isDigit() } },
                    modifier = Modifier.width(80.dp.scaled()).height(36.dp.scaled()),
                    textStyle = EditorTypography.mono(),
                    singleLine = true,
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = EditorColors.accent,
                        backgroundColor = EditorColors.background
                    )
                )

                Spacer(Modifier.height(32.dp.scaled()))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onCancel) {
                        Text(EditorStrings.observeString("cancel"), style = EditorTypography.body(), color = EditorColors.textSecondary)
                    }
                    Spacer(Modifier.width(12.dp.scaled()))
                    Button(
                        onClick = {
                            val w = width.toIntOrNull() ?: 1280
                            val h = height.toIntOrNull() ?: 720
                            val f = fps.toIntOrNull() ?: 24
                            onCreate(AnimationProject(name = name, canvasWidth = w, canvasHeight = h, fps = f))
                        },
                        colors = ButtonDefaults.buttonColors(backgroundColor = EditorColors.accent),
                        shape = EditorShapes.medium
                    ) {
                        Text(EditorStrings.observeString("btn.create"), color = Color.White, style = EditorTypography.body().copy(fontWeight = FontWeight.Bold))
                    }
                }
            }
        }
    }
}
