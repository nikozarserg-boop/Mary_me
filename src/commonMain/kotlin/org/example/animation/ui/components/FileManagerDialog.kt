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
import org.example.animation.localization.EditorStrings
import org.example.animation.ui.theme.EditorColors
import org.example.animation.ui.theme.EditorIcons
import java.io.File

/**
 * Кастомный файловый менеджер в стиле проводника
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

    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)).clickable { onResult(null) }, contentAlignment = Alignment.Center) {
        Surface(modifier = Modifier.width(640.dp).height(460.dp).clickable(enabled = false) {}, color = EditorColors.darkSurface, shape = RoundedCornerShape(8.dp), elevation = 8.dp) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Заголовок
                Row(modifier = Modifier.fillMaxWidth().background(EditorColors.panelHeader).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (mode == FileDialogMode.SAVE) EditorIcons.iconFileDownload else EditorIcons.iconFileUpload, "", tint = EditorColors.accentBlue, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (mode == FileDialogMode.SAVE) "Сохранить" else "Открыть", color = EditorColors.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.weight(1f))
                    Box(modifier = Modifier.size(24.dp).clip(RoundedCornerShape(4.dp)).clickable { onResult(null) }, contentAlignment = Alignment.Center) {
                        Text("✕", color = EditorColors.textSecondary, fontSize = 14.sp)
                    }
                }
                Divider(color = EditorColors.dividerColor, thickness = 1.dp)

                // Навигация
                Row(modifier = Modifier.fillMaxWidth().background(EditorColors.panelBackground).padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(28.dp).clip(RoundedCornerShape(4.dp)).background(EditorColors.buttonColor).clickable {
                        val parent = currentDir.parentFile
                        if (parent != null) currentDir = parent
                    }, contentAlignment = Alignment.Center) {
                        Icon(EditorIcons.iconArrowBack, "Назад", tint = EditorColors.textSecondary, modifier = Modifier.size(16.dp))
                    }
                    Spacer(Modifier.width(4.dp))
                    Box(modifier = Modifier.size(28.dp).clip(RoundedCornerShape(4.dp)).background(EditorColors.buttonColor).clickable {
                        val home = File(System.getProperty("user.home"))
                        if (home.exists()) currentDir = home
                    }, contentAlignment = Alignment.Center) {
                        Icon(EditorIcons.iconHome, "Домой", tint = EditorColors.textSecondary, modifier = Modifier.size(16.dp))
                    }
                    Spacer(Modifier.width(8.dp))
                    Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(4.dp)).background(EditorColors.darkSurfaceVariant).padding(horizontal = 8.dp, vertical = 6.dp)) {
                        Text(currentDir.absolutePath, color = EditorColors.textPrimary, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                Divider(color = EditorColors.dividerColor, thickness = 1.dp)

                // Список файлов
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    val scroll = rememberScrollState()
                    Column(modifier = Modifier.fillMaxSize().verticalScroll(scroll).padding(4.dp)) {
                        files.forEach { file ->
                            val isSel = selectedFile == file
                            Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)).background(if (isSel) EditorColors.selectionColor else Color.Transparent).clickable {
                                if (file.isDirectory) {
                                    currentDir = file
                                    selectedFile = null
                                } else {
                                    selectedFile = file
                                    fileName = file.nameWithoutExtension
                                }
                            }.padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    if (file.isDirectory) EditorIcons.iconFolderOpen else EditorIcons.iconFileDownload,
                                    "",
                                    tint = if (file.isDirectory) EditorColors.accentBlue else EditorColors.textSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(file.name, color = EditorColors.textPrimary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                if (!file.isDirectory) {
                                    val ext = file.extension.lowercase()
                                    if (ext == extension || mode == FileDialogMode.OPEN) {
                                        Box(modifier = Modifier.clip(RoundedCornerShape(3.dp)).background(EditorColors.accentGreen.copy(alpha = 0.2f)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                                            Text(ext.uppercase(), color = EditorColors.accentGreen, fontSize = 9.sp)
                                        }
                                    }
                                }
                            }
                            Spacer(Modifier.height(2.dp))
                        }
                        if (files.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Папка пуста", color = EditorColors.textMuted, fontSize = 12.sp)
                            }
                        }
                    }
                }

                Divider(color = EditorColors.dividerColor, thickness = 1.dp)

                // Поле имени файла и кнопки
                Column(modifier = Modifier.fillMaxWidth().background(EditorColors.panelBackground).padding(12.dp)) {
                    if (errorMsg != null) {
                        Text(errorMsg!!, color = EditorColors.accentRed, fontSize = 11.sp)
                        Spacer(Modifier.height(4.dp))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${if (mode == FileDialogMode.SAVE) "Имя:" else "Файл:"} ", color = EditorColors.textSecondary, fontSize = 12.sp)
                        OutlinedTextField(
                            value = fileName,
                            onValueChange = { fileName = it },
                            singleLine = true,
                            modifier = Modifier.weight(1f).height(36.dp),
                            textStyle = LocalTextStyle.current.copy(fontSize = 12.sp, color = EditorColors.textPrimary),
                            colors = TextFieldDefaults.outlinedTextFieldColors(focusedBorderColor = EditorColors.accentBlue, unfocusedBorderColor = EditorColors.dividerColor, cursorColor = EditorColors.accentBlue, backgroundColor = EditorColors.darkSurfaceVariant)
                        )
                        if (mode == FileDialogMode.SAVE) {
                            Spacer(Modifier.width(4.dp))
                            Text(".$extension", color = EditorColors.textMuted, fontSize = 12.sp)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Button(onClick = { onResult(null) }, colors = ButtonDefaults.buttonColors(backgroundColor = EditorColors.buttonColor), modifier = Modifier.height(34.dp)) {
                            Text("Отмена", color = EditorColors.textPrimary, fontSize = 12.sp)
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(onClick = {
                            val targetName = if (mode == FileDialogMode.SAVE) {
                                val n = if (fileName.contains(".")) fileName else "$fileName.$extension"
                                val f = File(currentDir, n)
                                if (f.exists()) {
                                    errorMsg = "Файл уже существует"
                                    return@Button
                                }
                                f.absolutePath
                            } else {
                                selectedFile?.absolutePath ?: run {
                                    // попробуем собрать из имени
                                    val n = if (fileName.contains(".")) fileName else "$fileName.$extension"
                                    val f = File(currentDir, n)
                                    if (f.exists()) f.absolutePath else null
                                }
                            }
                            if (targetName != null) onResult(targetName) else errorMsg = "Выберите файл"
                        }, colors = ButtonDefaults.buttonColors(backgroundColor = EditorColors.accentBlue), modifier = Modifier.height(34.dp)) {
                            Text(if (mode == FileDialogMode.SAVE) "Сохранить" else "Открыть", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

enum class FileDialogMode { OPEN, SAVE }