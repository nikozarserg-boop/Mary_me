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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import org.example.animation.localization.EditorStrings
import org.example.animation.ui.theme.EditorColors
import org.example.animation.ui.theme.EditorIcons
import java.io.File

/**
 * Кастомный файловый менеджер с эффектом полупрозрачности
 */
@Composable
fun FileManagerDialog(
    mode: FileDialogMode,
    defaultName: String = "",
    extension: String = "maryme",
    onResult: (String?) -> Unit
) {
    var currentDir by remember { mutableStateOf(File(System.getProperty("user.home"))) }
    var fileName by remember { mutableStateOf(defaultName) }
    var selectedFile by remember { mutableStateOf<File?>(null) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val files = remember(currentDir) {
        try {
            currentDir.listFiles()?.filter { !it.isHidden }?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() })) ?: emptyList()
        } catch (e: Exception) { emptyList() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f))
            .clickable { onResult(null) }, 
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .width(680.dp)
                .height(500.dp)
                .clickable(enabled = false) {},
            color = EditorColors.darkSurface.copy(alpha = 0.92f), // Полупрозрачность
            shape = RoundedCornerShape(12.dp), 
            elevation = 16.dp,
            border = BorderStroke(1.dp, EditorColors.dividerColor)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Заголовок
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(EditorColors.panelHeader.copy(alpha = 0.5f))
                        .padding(16.dp), 
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (mode == FileDialogMode.SAVE) EditorIcons.iconFileDownload else EditorIcons.iconFileUpload, 
                        null, 
                        tint = EditorColors.accentBlue, 
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = if (mode == FileDialogMode.SAVE) EditorStrings.observeString("file.saveTitle") else EditorStrings.observeString("file.openTitle"), 
                        color = EditorColors.textPrimary, 
                        fontSize = 16.sp, 
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = { onResult(null) }, modifier = Modifier.size(24.dp)) {
                        Text("✕", color = EditorColors.textSecondary, fontSize = 16.sp)
                    }
                }
                
                Divider(color = EditorColors.dividerColor)

                // Навигация
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(EditorColors.panelBackground.copy(alpha = 0.3f))
                        .padding(12.dp), 
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NavButton(EditorIcons.iconArrowBack) {
                        val parent = currentDir.parentFile
                        if (parent != null) currentDir = parent
                    }
                    Spacer(Modifier.width(8.dp))
                    NavButton(EditorIcons.iconHome) {
                        currentDir = File(System.getProperty("user.home"))
                    }
                    Spacer(Modifier.width(12.dp))
                    
                    Surface(
                        modifier = Modifier.weight(1f),
                        color = EditorColors.darkSurfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            currentDir.absolutePath, 
                            color = EditorColors.textPrimary, 
                            fontSize = 12.sp, 
                            maxLines = 1, 
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
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
                            .padding(horizontal = 12.dp)
                    ) {
                        files.forEach { file ->
                            FileItem(
                                file = file,
                                isSelected = selectedFile == file,
                                filterExt = extension,
                                onClick = {
                                    if (file.isDirectory) {
                                        currentDir = file
                                        selectedFile = null
                                    } else {
                                        selectedFile = file
                                        fileName = file.nameWithoutExtension
                                    }
                                }
                            )
                        }
                    }
                }

                Divider(color = EditorColors.dividerColor)

                // Нижняя панель
                Column(modifier = Modifier.padding(16.dp)) {
                    if (errorMsg != null) {
                        Text(errorMsg!!, color = EditorColors.accentRed, fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (mode == FileDialogMode.SAVE) "Имя:" else "Файл:", 
                            color = EditorColors.textSecondary, 
                            fontSize = 13.sp,
                            modifier = Modifier.width(60.dp)
                        )
                        OutlinedTextField(
                            value = fileName,
                            onValueChange = { fileName = it },
                            singleLine = true,
                            modifier = Modifier.weight(1f).height(48.dp),
                            textStyle = LocalTextStyle.current.copy(fontSize = 14.sp, color = EditorColors.textPrimary),
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                focusedBorderColor = EditorColors.accentBlue, 
                                unfocusedBorderColor = EditorColors.dividerColor,
                                backgroundColor = EditorColors.darkSurfaceVariant.copy(alpha = 0.2f)
                            )
                        )
                        if (mode == FileDialogMode.SAVE) {
                            Text(".$extension", color = EditorColors.textMuted, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                    
                    Spacer(Modifier.height(16.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { onResult(null) }) {
                            Text(EditorStrings.observeString("cancel"), color = EditorColors.textSecondary)
                        }
                        Spacer(Modifier.width(12.dp))
                        Button(
                            onClick = {
                                val target = if (mode == FileDialogMode.SAVE) {
                                    val n = if (fileName.contains(".")) fileName else "$fileName.$extension"
                                    val f = File(currentDir, n)
                                    if (f.exists()) { errorMsg = "Файл уже существует"; null } else f.absolutePath
                                } else {
                                    selectedFile?.absolutePath
                                }
                                if (target != null) onResult(target) else if (mode == FileDialogMode.OPEN) errorMsg = "Выберите файл"
                            },
                            colors = ButtonDefaults.buttonColors(backgroundColor = EditorColors.accentBlue),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(40.dp).width(120.dp)
                        ) {
                            Text(
                                if (mode == FileDialogMode.SAVE) "Сохранить" else "Открыть", 
                                color = Color.White, 
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NavButton(icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.size(32.dp).clickable { onClick() },
        color = EditorColors.buttonColor.copy(alpha = 0.4f),
        shape = RoundedCornerShape(6.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = EditorColors.textPrimary, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun FileItem(file: File, isSelected: Boolean, filterExt: String, onClick: () -> Unit) {
    val isMatch = !file.isDirectory && file.extension.lowercase() == filterExt.lowercase()
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(38.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (isSelected) EditorColors.selectionColor.copy(alpha = 0.7f) else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            if (file.isDirectory) EditorIcons.iconFolderOpen else EditorIcons.iconFileDownload,
            null,
            tint = if (file.isDirectory) EditorColors.accentBlue else EditorColors.textSecondary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            file.name, 
            color = if (isSelected) Color.White else if (isMatch || file.isDirectory) EditorColors.textPrimary else EditorColors.textMuted,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

enum class FileDialogMode { OPEN, SAVE }
