package org.example.animation.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import org.example.animation.engine.AnimationEngine
import org.example.animation.localization.EditorStrings
import org.example.animation.ui.theme.EditorColors
import org.example.animation.ui.theme.scaled

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
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)).clickable { onCancel() },
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = 480.dp.scaled()) // Увеличено с 420 до 480 для лучшей вместимости
                .fillMaxWidth(0.9f)
                .wrapContentHeight() // Позволяем окну адаптироваться под контент
                .clickable(enabled = false) {},
            color = EditorColors.surface,
            shape = RoundedCornerShape(12.dp.scaled()),
            elevation = 16.dp.scaled(),
            border = BorderStroke(1.dp.scaled(), EditorColors.divider)
        ) {
            Column(modifier = Modifier.padding(bottom = 8.dp.scaled())) {
                // Заголовок
                Text(
                    text = EditorStrings.observeString("export.title"),
                    color = EditorColors.accent,
                    fontSize = 18.sp.scaled(),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(20.dp.scaled())
                )
                
                // Содержимое
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp.scaled())
                ) {
                    Text(EditorStrings.observeString("project.name"), color = EditorColors.textSecondary, fontSize = 11.sp.scaled())
                    OutlinedTextField(
                        value = fileName,
                        onValueChange = { fileName = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().height(48.dp.scaled()),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp.scaled(), color = EditorColors.textPrimary),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = EditorColors.accent,
                            backgroundColor = EditorColors.background,
                            unfocusedBorderColor = EditorColors.divider,
                            textColor = EditorColors.textPrimary,
                            cursorColor = EditorColors.accent
                        )
                    )

                    Spacer(Modifier.height(16.dp.scaled()))

                    Row(Modifier.fillMaxWidth()) {
                        Column(Modifier.weight(1f)) {
                            Text(EditorStrings.observeString("project.width"), color = EditorColors.textSecondary, fontSize = 11.sp.scaled())
                            OutlinedTextField(
                                value = exportWidth,
                                onValueChange = { exportWidth = it.filter { c -> c.isDigit() } },
                                singleLine = true,
                                modifier = Modifier.height(48.dp.scaled()),
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp.scaled(), color = EditorColors.textPrimary, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                                colors = TextFieldDefaults.outlinedTextFieldColors(
                                    focusedBorderColor = EditorColors.accent,
                                    backgroundColor = EditorColors.background,
                                    unfocusedBorderColor = EditorColors.divider,
                                    textColor = EditorColors.textPrimary,
                                    cursorColor = EditorColors.accent
                                )
                            )
                        }
                        Spacer(Modifier.width(12.dp.scaled()))
                        Column(Modifier.weight(1f)) {
                            Text(EditorStrings.observeString("project.height"), color = EditorColors.textSecondary, fontSize = 11.sp.scaled())
                            OutlinedTextField(
                                value = exportHeight,
                                onValueChange = { exportHeight = it.filter { c -> c.isDigit() } },
                                singleLine = true,
                                modifier = Modifier.height(48.dp.scaled()),
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp.scaled(), color = EditorColors.textPrimary, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                                colors = TextFieldDefaults.outlinedTextFieldColors(
                                    focusedBorderColor = EditorColors.accent,
                                    backgroundColor = EditorColors.background,
                                    unfocusedBorderColor = EditorColors.divider,
                                    textColor = EditorColors.textPrimary,
                                    cursorColor = EditorColors.accent
                                )
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp.scaled()))
                    
                    Text(EditorStrings.observeString("export.format") ?: "Format", color = EditorColors.textSecondary, fontSize = 11.sp.scaled())
                    
                    // Используем FlowRow для автоматического переноса кнопок форматов
                    FlowRow(
                        modifier = Modifier.padding(top = 8.dp.scaled()).fillMaxWidth(),
                        mainAxisSpacing = 8.dp.scaled(),
                        crossAxisSpacing = 8.dp.scaled()
                    ) {
                        val formats = listOf("png", "jpg", "webp", "gif", "apng", "mp4", "webm", "mov", "mkv", "avi")
                        formats.forEach { fmt ->
                            androidx.compose.material.Button(
                                onClick = { selectedFormat = fmt },
                                colors = androidx.compose.material.ButtonDefaults.buttonColors(
                                    backgroundColor = if (selectedFormat == fmt) EditorColors.accent else EditorColors.surfaceVariant,
                                    contentColor = if (selectedFormat == fmt) Color.White else EditorColors.textPrimary
                                ),
                                modifier = Modifier.height(32.dp.scaled()),
                                shape = RoundedCornerShape(8.dp.scaled()),
                                elevation = ButtonDefaults.elevation(0.dp, 0.dp)
                            ) {
                                Text(fmt.uppercase(), fontSize = 10.sp.scaled(), fontWeight = FontWeight.Medium)
                            }
                        }
                    }

                    Spacer(Modifier.height(24.dp.scaled()))
                }

                Divider(color = EditorColors.divider)

                // Нижняя панель
                DialogButtonRow(
                    cancelText = EditorStrings.observeString("cancel"),
                    confirmText = EditorStrings.observeString("export.perform"),
                    onCancel = onCancel,
                    onConfirm = {
                        onExport(
                            fileName,
                            exportWidth.toIntOrNull() ?: project.canvasWidth,
                            exportHeight.toIntOrNull() ?: project.canvasHeight,
                            selectedFormat
                        )
                    }
                )
            }
        }
    }
}


@Composable
private fun FlowRow(
    modifier: Modifier = Modifier,
    mainAxisSpacing: androidx.compose.ui.unit.Dp = 0.dp,
    crossAxisSpacing: androidx.compose.ui.unit.Dp = 0.dp,
    content: @Composable () -> Unit
) {
    androidx.compose.ui.layout.Layout(content = content, modifier = modifier) { measurables, constraints ->
        val placeables = measurables.map { it.measure(constraints.copy(minWidth = 0, minHeight = 0)) }
        val rows = mutableListOf<List<androidx.compose.ui.layout.Placeable>>()
        var currentRow = mutableListOf<androidx.compose.ui.layout.Placeable>()
        var currentRowWidth = 0
        
        placeables.forEach { placeable ->
            val placeableWidth = placeable.width
            if (currentRowWidth + placeableWidth > constraints.maxWidth && currentRow.isNotEmpty()) {
                rows.add(currentRow)
                currentRow = mutableListOf()
                currentRowWidth = 0
            }
            currentRow.add(placeable)
            currentRowWidth += placeableWidth + mainAxisSpacing.roundToPx()
        }
        if (currentRow.isNotEmpty()) {
            rows.add(currentRow)
        }
        
        val height = rows.sumOf { row -> row.maxOf { it.height } } + (rows.size - 1) * crossAxisSpacing.roundToPx()
        val width = if (constraints.hasBoundedWidth) constraints.maxWidth else rows.maxOfOrNull { row -> row.sumOf { it.width + mainAxisSpacing.roundToPx() } - mainAxisSpacing.roundToPx() } ?: 0

        layout(width, height) {
            var y = 0
            rows.forEach { row ->
                var x = 0
                val rowHeight = row.maxOf { it.height }
                row.forEach { placeable ->
                    placeable.placeRelative(x, y)
                    x += placeable.width + mainAxisSpacing.roundToPx()
                }
                y += rowHeight + crossAxisSpacing.roundToPx()
            }
        }
    }
}
