package org.example.animation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
    var nameError by remember { mutableStateOf<String?>(null) }
    var sizeError by remember { mutableStateOf<String?>(null) }

    // Нельзя выйти иначе чем по "Отмена": фон не перехватывает клики для закрытия
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = 400.dp.scaled())
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.85f)
                .clickable(enabled = false) {},
            color = EditorColors.surface,
            shape = EditorShapes.large,
            elevation = 16.dp.scaled(),
            border = BorderStroke(1.dp.scaled(), EditorColors.divider)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Заголовок (фиксированный)
                Text(
                    EditorStrings.observeString("dialog.newProject"),
                    style = EditorTypography.body().copy(fontSize = 18.sp.scaled(), fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(20.dp.scaled())
                )

                // Прокручиваемое содержимое
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp.scaled())
                ) {
                    Text(EditorStrings.observeString("project.name"), style = EditorTypography.caption())
                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            nameError = if (it.isBlank()) EditorStrings["project.nameRequired"] else null
                        },
                        modifier = Modifier.fillMaxWidth().height(40.dp.scaled()),
                        textStyle = EditorTypography.body(),
                        singleLine = true,
                        isError = nameError != null,
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = EditorColors.accent,
                            backgroundColor = EditorColors.background,
                            unfocusedBorderColor = EditorColors.divider
                        )
                    )
                    if (nameError != null) {
                        Text(nameError!!, color = EditorColors.accentRed, fontSize = 11.sp.scaled(), modifier = Modifier.padding(top = 4.dp.scaled()))
                    }

                    Spacer(Modifier.height(16.dp.scaled()))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(EditorStrings.observeString("project.width"), style = EditorTypography.caption())
                            OutlinedTextField(
                                value = width,
                                onValueChange = {
                                    width = it.filter { c -> c.isDigit() }.take(5)
                                    sizeError = null
                                },
                                modifier = Modifier.fillMaxWidth().height(40.dp.scaled()),
                                textStyle = EditorTypography.mono(),
                                singleLine = true,
                                isError = sizeError != null,
                                colors = TextFieldDefaults.outlinedTextFieldColors(
                                    focusedBorderColor = EditorColors.accent,
                                    backgroundColor = EditorColors.background,
                                    unfocusedBorderColor = EditorColors.divider
                                )
                            )
                        }
                        Spacer(Modifier.width(12.dp.scaled()))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(EditorStrings.observeString("project.height"), style = EditorTypography.caption())
                            OutlinedTextField(
                                value = height,
                                onValueChange = {
                                    height = it.filter { c -> c.isDigit() }.take(5)
                                    sizeError = null
                                },
                                modifier = Modifier.fillMaxWidth().height(40.dp.scaled()),
                                textStyle = EditorTypography.mono(),
                                singleLine = true,
                                isError = sizeError != null,
                                colors = TextFieldDefaults.outlinedTextFieldColors(
                                    focusedBorderColor = EditorColors.accent,
                                    backgroundColor = EditorColors.background,
                                    unfocusedBorderColor = EditorColors.divider
                                )
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp.scaled()))

                    Text(EditorStrings.observeString("canvas.fps"), style = EditorTypography.caption())
                    OutlinedTextField(
                        value = fps,
                        onValueChange = { fps = it.filter { c -> c.isDigit() }.take(3) },
                        modifier = Modifier.width(100.dp.scaled()).height(40.dp.scaled()),
                        textStyle = EditorTypography.mono(),
                        singleLine = true,
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = EditorColors.accent,
                            backgroundColor = EditorColors.background,
                            unfocusedBorderColor = EditorColors.divider
                        )
                    )

                    if (sizeError != null) {
                        Text(sizeError!!, color = EditorColors.accentRed, fontSize = 11.sp.scaled(), modifier = Modifier.padding(top = 4.dp.scaled()))
                    }

                    Spacer(Modifier.height(20.dp.scaled()))
                }

                Divider(color = EditorColors.divider)

                // Нижняя панель (фиксированная)
                DialogButtonRow(
                    cancelText = EditorStrings.observeString("cancel"),
                    confirmText = EditorStrings.observeString("btn.create"),
                    onCancel = onCancel,
                    onConfirm = {
                        val w = width.toIntOrNull()
                        val h = height.toIntOrNull()
                        val f = fps.toIntOrNull()
                        val nameOk = name.isNotBlank()
                        val sizeOk = (w != null && w in 1..10000) && (h != null && h in 1..10000) && (f != null && f in 1..240)
                        nameError = if (!nameOk) EditorStrings["project.nameRequired"] else null
                        sizeError = if (!sizeOk) EditorStrings["project.sizeError"] else null
                        if (nameOk && sizeOk) {
                            onCreate(AnimationProject(name = name.trim(), canvasWidth = w!!, canvasHeight = h!!, fps = f!!))
                        }
                    },
                    confirmEnabled = name.isNotBlank()
                        && (width.toIntOrNull()?.let { it in 1..10000 } ?: false)
                        && (height.toIntOrNull()?.let { it in 1..10000 } ?: false)
                        && (fps.toIntOrNull()?.let { it in 1..240 } ?: false)
                )
            }
        }
    }
}
