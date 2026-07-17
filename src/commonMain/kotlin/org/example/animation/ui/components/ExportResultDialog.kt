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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.example.animation.io.PlatformFileHandler
import org.example.animation.io.decodeImage
import org.example.animation.localization.EditorStrings
import org.example.animation.ui.theme.EditorColors
import org.example.animation.ui.theme.scaled

@Composable
fun ExportResultDialog(
    filePath: String,
    format: String,
    fileHandler: PlatformFileHandler,
    onClose: () -> Unit
) {
    var previewFrames by remember { mutableStateOf<List<ImageBitmap>>(emptyList()) }
    var currentFrameIndex by remember { mutableStateOf(0) }
    var isLoadingPreview by remember { mutableStateOf(true) }
    var fileSize by remember { mutableStateOf("") }

    val isVideo = format.lowercase() in listOf("gif", "apng", "mp4", "webm", "mov", "mkv", "avi")

    LaunchedEffect(filePath) {
        isLoadingPreview = true
        try {
            val bytes = fileHandler.readFromPath(filePath)
            if (bytes != null) {
                val sizeKb = bytes.size / 1024
                fileSize = if (sizeKb > 1024) "${"%.2f".format(sizeKb / 1024f)} MB" else "$sizeKb KB"

                if (isVideo) {
                    val frameBytesList = fileHandler.extractVideoFrames(filePath, 12, 60)
                    previewFrames = frameBytesList.mapNotNull { decodeImage(it) }
                } else {
                    val bmp = decodeImage(bytes)
                    if (bmp != null) previewFrames = listOf(bmp)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        isLoadingPreview = false
    }

    LaunchedEffect(previewFrames, isVideo) {
        if (isVideo && previewFrames.isNotEmpty()) {
            while (true) {
                delay(1000L / 12) // По умолчанию 12 FPS для превью
                currentFrameIndex = (currentFrameIndex + 1) % previewFrames.size
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)).clickable { onClose() },
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = 450.dp.scaled())
                .fillMaxWidth(0.9f)
                .wrapContentHeight()
                .clickable(enabled = false) {},
            color = EditorColors.surface,
            shape = RoundedCornerShape(12.dp.scaled()),
            elevation = 16.dp.scaled(),
            border = BorderStroke(1.dp.scaled(), EditorColors.divider)
        ) {
            Column(modifier = Modifier.padding(20.dp.scaled())) {
                Text(
                    text = EditorStrings.observeString("export.success") ?: "Экспорт завершен",
                    color = EditorColors.accentGreen,
                    fontSize = 18.sp.scaled(),
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(16.dp.scaled()))

                // Предпросмотр
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp.scaled())
                        .clip(RoundedCornerShape(8.dp.scaled()))
                        .background(EditorColors.background),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoadingPreview) {
                        CircularProgressIndicator(color = EditorColors.accent, modifier = Modifier.size(32.dp.scaled()))
                    } else if (previewFrames.isNotEmpty()) {
                        Image(
                            bitmap = previewFrames[currentFrameIndex],
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize().padding(8.dp.scaled()),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Text("Нет превью", color = EditorColors.textSecondary)
                    }
                }

                Spacer(Modifier.height(16.dp.scaled()))

                // Метаданные
                Column(modifier = Modifier.fillMaxWidth()) {
                    InfoRow("Формат:", format.uppercase())
                    InfoRow("Размер:", fileSize)
                    InfoRow("Путь:", filePath, isPath = true)
                }

                Spacer(Modifier.height(24.dp.scaled()))

                // Кнопки действий
                FlowRow(
                    mainAxisSpacing = 8.dp.scaled(),
                    crossAxisSpacing = 8.dp.scaled(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ActionButton(EditorStrings.observeString("export.open") ?: "Открыть") {
                        fileHandler.shareFile(filePath)
                    }
                    ActionButton(EditorStrings.observeString("export.copy") ?: "Копировать") {
                        fileHandler.copyFileToClipboard(filePath)
                    }
                    ActionButton(EditorStrings.observeString("export.showInFolder") ?: "В папке") {
                        fileHandler.openInExplorer(filePath)
                    }
                }

                Spacer(Modifier.height(16.dp.scaled()))

                Button(
                    onClick = onClose,
                    modifier = Modifier.align(Alignment.End).height(36.dp.scaled()),
                    colors = ButtonDefaults.buttonColors(backgroundColor = EditorColors.surfaceVariant),
                    shape = RoundedCornerShape(8.dp.scaled())
                ) {
                    Text(EditorStrings.observeString("close"), color = EditorColors.textPrimary)
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String, isPath: Boolean = false) {
    Row(modifier = Modifier.padding(vertical = 2.dp.scaled())) {
        Text(label, color = EditorColors.textSecondary, fontSize = 12.sp.scaled(), modifier = Modifier.width(60.dp.scaled()))
        Text(
            value,
            color = EditorColors.textPrimary,
            fontSize = 12.sp.scaled(),
            fontWeight = FontWeight.Medium,
            maxLines = if (isPath) 2 else 1
        )
    }
}

@Composable
private fun ActionButton(text: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.height(32.dp.scaled()),
        shape = RoundedCornerShape(8.dp.scaled()),
        border = BorderStroke(1.dp.scaled(), EditorColors.divider),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = EditorColors.textPrimary)
    ) {
        Text(text, fontSize = 11.sp.scaled())
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
        if (currentRow.isNotEmpty()) rows.add(currentRow)
        
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
