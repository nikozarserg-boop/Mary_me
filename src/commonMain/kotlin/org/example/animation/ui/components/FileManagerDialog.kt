package org.example.animation.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt
import org.example.animation.io.FileEntry
import org.example.animation.io.PlatformFileHandler
import org.example.animation.io.createPlatformFileHandler
import org.example.animation.localization.EditorStrings
import org.example.animation.ui.theme.*

/**
 * Кроссплатформенный файловый менеджер
 */
@Composable
fun FileManagerDialog(
    mode: FileDialogMode,
    defaultName: String = "",
    extension: String = "maryme",
    onResult: (String?) -> Unit
) {
    val fileHandler = remember { createPlatformFileHandler() }
    var currentPath by remember { mutableStateOf(fileHandler.getHomeDirectory()) }
    var fileName by remember { mutableStateOf(defaultName) }
    var selectedEntry by remember { mutableStateOf<FileEntry?>(null) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var offset by remember { mutableStateOf(IntOffset(0, 0)) }

    val entries = remember(currentPath) {
        try {
            fileHandler.listFiles(currentPath)
                .sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
        } catch (e: Exception) {
            emptyList()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .offset { offset }
                .width(680.dp.scaled())
                .height(500.dp.scaled())
                .clickable(enabled = false) {},
            // Проводник получает цвета темы напрямую из EditorColors (как и другие панели),
            // поэтому в стеклянной теме он автоматически становится голубым и полупрозрачным
            color = EditorColors.surface,
            shape = RoundedCornerShape(12.dp.scaled()),
            elevation = 16.dp.scaled(),
            border = BorderStroke(1.dp.scaled(), EditorColors.divider)
        ) {
            // Контент и блик лежат в одном Box: блик — наложение, не влияет на раскладку
            Box(modifier = Modifier.fillMaxSize()) {
                // В стеклянной теме поверх проводника рисуем линзовый блик «рыбьего глаза»
                if (LocalThemeType.current == ThemeType.GLASS) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(EditorColors.glassSheen, Color.Transparent),
                                    center = Offset(0.2f, 0.08f),
                                    radius = 1.2f
                                )
                            )
                    )
                }
                Column(modifier = Modifier.fillMaxSize()) {
                    // Заголовок (перетаскиваемый)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(EditorColors.panelHeader.copy(alpha = 0.5f))
                            .pointerHoverIcon(PointerIcon.Hand)
                            .pointerInput(Unit) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    offset = IntOffset(
                                        offset.x + dragAmount.x.roundToInt(),
                                        offset.y + dragAmount.y.roundToInt()
                                    )
                                }
                            }
                            .padding(16.dp.scaled()),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (mode == FileDialogMode.SAVE) EditorIcons.iconSave else EditorIcons.iconFolderOpen,
                            null,
                            tint = EditorColors.accent,
                            modifier = Modifier.size(20.dp.scaled())
                        )
                        Spacer(Modifier.width(12.dp.scaled()))
                        Text(
                            text = if (mode == FileDialogMode.SAVE) EditorStrings.observeString("file.saveTitle") else EditorStrings.observeString("file.openTitle"),
                            color = EditorColors.textPrimary,
                            fontSize = 16.sp.scaled(),
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = { onResult(null) }, modifier = Modifier.size(24.dp.scaled())) {
                            Icon(EditorIcons.iconClose, null, tint = EditorColors.textSecondary, modifier = Modifier.size(16.dp.scaled()))
                        }
                    }

                    Divider(color = EditorColors.divider)

                    // Навигация
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(EditorColors.panelBackground.copy(alpha = 0.3f))
                            .padding(12.dp.scaled()),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        NavButton(EditorIcons.iconArrowBack) {
                            fileHandler.getParentPath(currentPath)?.let { currentPath = it }
                        }
                        Spacer(Modifier.width(8.dp.scaled()))
                        NavButton(EditorIcons.iconHome) {
                            currentPath = fileHandler.getHomeDirectory()
                        }
                        Spacer(Modifier.width(12.dp.scaled()))

                        Surface(
                            modifier = Modifier.weight(1f),
                            color = EditorColors.background.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(6.dp.scaled())
                        ) {
                            Text(
                                currentPath,
                                color = EditorColors.textPrimary,
                                fontSize = 12.sp.scaled(),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 12.dp.scaled(), vertical = 8.dp.scaled())
                            )
                        }
                    }

                    // Список файлов
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        val scroll = rememberScrollState()
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(scroll)
                                .padding(horizontal = 12.dp.scaled())
                        ) {
                            entries.forEach { entry ->
                                FileItem(
                                    entry = entry,
                                    isSelected = selectedEntry?.path == entry.path,
                                    filterExt = extension,
                                    onClick = {
                                        if (entry.isDirectory) {
                                            currentPath = entry.path
                                            selectedEntry = null
                                        } else {
                                            selectedEntry = entry
                                            fileName = entry.name.substringBeforeLast(".")
                                        }
                                    }
                                )
                            }
                        }
                    }

                    Divider(color = EditorColors.divider)

                    // Нижняя панель
                    Column(modifier = Modifier.padding(16.dp.scaled())) {
                        if (errorMsg != null) {
                            Text(errorMsg!!, color = EditorColors.accentRed, fontSize = 12.sp.scaled(), modifier = Modifier.padding(bottom = 8.dp.scaled()))
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (mode == FileDialogMode.SAVE) EditorStrings.observeString("file.name") else EditorStrings.observeString("file.file"),
                                color = EditorColors.textPrimary,
                                fontSize = 13.sp.scaled(),
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.width(60.dp.scaled())
                            )
                            OutlinedTextField(
                                value = fileName,
                                onValueChange = { fileName = it },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                textStyle = LocalTextStyle.current.copy(fontSize = 14.sp.scaled(), color = EditorColors.textPrimary),
                                colors = TextFieldDefaults.outlinedTextFieldColors(
                                    textColor = EditorColors.textPrimary,
                                    focusedBorderColor = EditorColors.accent,
                                    unfocusedBorderColor = EditorColors.divider,
                                    backgroundColor = EditorColors.background.copy(alpha = 0.8f),
                                    cursorColor = EditorColors.accent
                                )
                            )
                            if (mode == FileDialogMode.SAVE) {
                                Text(".$extension", color = EditorColors.textMuted, modifier = Modifier.padding(start = 8.dp.scaled()))
                            }
                        }

                        Spacer(Modifier.height(16.dp.scaled()))

                        DialogButtonRow(
                            cancelText = EditorStrings.observeString("cancel"),
                            confirmText = if (mode == FileDialogMode.SAVE) EditorStrings.observeString("file.saveBtn") else EditorStrings.observeString("file.openBtn"),
                            onCancel = { onResult(null) },
                            onConfirm = {
                                if (mode == FileDialogMode.SAVE) {
                                    if (fileName.isEmpty()) {
                                        errorMsg = EditorStrings["file.selectError"]
                                        return@DialogButtonRow
                                    }
                                    val fullFileName = if (fileName.contains(".")) fileName else "$fileName.$extension"
                                    val separator = if (currentPath.endsWith("/") || currentPath.endsWith("\\")) "" else "/"
                                    val fullPath = "$currentPath$separator$fullFileName"

                                    if (fileHandler.fileExists(fullPath)) {
                                        errorMsg = EditorStrings["file.exists"]
                                    } else {
                                        onResult(fullPath)
                                    }
                                } else {
                                    selectedEntry?.let { onResult(it.path) } ?: run { errorMsg = EditorStrings["file.selectError"] }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NavButton(icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.size(32.dp.scaled()).clickable { onClick() },
        color = EditorColors.divider.copy(alpha = 0.4f),
        shape = RoundedCornerShape(6.dp.scaled())
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = EditorColors.textPrimary, modifier = Modifier.size(16.dp.scaled()))
        }
    }
}

@Composable
private fun FileItem(entry: FileEntry, isSelected: Boolean, filterExt: String, onClick: () -> Unit) {
    val isMatch = !entry.isDirectory && entry.extension.lowercase() == filterExt.lowercase()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(38.dp.scaled())
            .clip(RoundedCornerShape(6.dp.scaled()))
            .background(if (isSelected) EditorColors.selection.copy(alpha = 0.7f) else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 12.dp.scaled()),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            if (entry.isDirectory) EditorIcons.iconFolderOpen else EditorIcons.iconFileDownload,
            null,
            tint = if (entry.isDirectory) EditorColors.accent else EditorColors.textSecondary,
            modifier = Modifier.size(18.dp.scaled())
        )
        Spacer(Modifier.width(12.dp.scaled()))
        Text(
            entry.name,
            color = if (isSelected) Color.White else if (isMatch || entry.isDirectory) EditorColors.textPrimary else EditorColors.textMuted,
            fontSize = 13.sp.scaled(),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

enum class FileDialogMode { OPEN, SAVE }