package org.example

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.example.mary_me.generated.resources.Res
import org.example.animation.engine.AnimationEngine
import org.example.animation.io.AppSettingsManager
import org.example.animation.io.ProjectSerializer
import org.example.animation.io.createPlatformFileHandler
import org.example.animation.localization.EditorStrings
import org.example.animation.ui.components.*
import org.example.animation.ui.theme.EditorColors
import org.example.animation.ui.theme.EditorIcons
import org.example.animation.ui.theme.EditorTheme
import org.jetbrains.compose.resources.ExperimentalResourceApi

@OptIn(ExperimentalResourceApi::class)
@Composable
fun App(
    exitRequested: Boolean = false,
    onExitCancel: () -> Unit = {},
    onExitConfirm: () -> Unit = {}
) {
    val engine = remember { AnimationEngine() }
    val fileHandler = remember { createPlatformFileHandler() }
    
    var showSettings by remember { mutableStateOf(false) }
    var showNewProject by remember { mutableStateOf(false) }
    var showExitConfirm by remember { mutableStateOf(false) }
    var pendingExportFormat by remember { mutableStateOf<String?>(null) }
    var pendingFileAction by remember { mutableStateOf<String?>(null) }
    var stringsLoaded by remember { mutableStateOf(false) }

    val hasUnsavedChanges by engine.hasUnsavedChanges.collectAsState()
    val lastAutosave by engine.lastAutosaveTime.collectAsState()
    var showAutosaveToast by remember { mutableStateOf(false) }

    // Глобальный масштаб
    var uiScale by remember { mutableStateOf(AppSettingsManager.getUiScale()) }

    // Показ уведомления об автосохранении
    LaunchedEffect(lastAutosave) {
        if (lastAutosave > 0) {
            showAutosaveToast = true
            delay(3000)
            showAutosaveToast = false
        }
    }

    // Инициализация и загрузка локализации
    LaunchedEffect(Unit) {
        AppSettingsManager.load()
        uiScale = AppSettingsManager.getUiScale()
        
        try {
            // Исправленные пути к ресурсам: файлы в папке files/ должны иметь префикс "files/"
            val ruBytes = try { 
                Res.readBytes("files/locales/strings_ru.json") 
            } catch (e: Exception) { 
                Res.readBytes("locales/strings_ru.json") // Fallback
            }
            
            val enBytes = try { 
                Res.readBytes("files/locales/strings_en.json") 
            } catch (e: Exception) { 
                Res.readBytes("locales/strings_en.json") // Fallback
            }

            EditorStrings.loadStrings("ru", ruBytes.decodeToString())
            EditorStrings.loadStrings("en", enBytes.decodeToString())
        } catch (e: Exception) {
            println("Localization loading error: ${e.message}")
            e.printStackTrace()
        } finally {
            stringsLoaded = true
        }
    }

    // Системный выход (Desktop)
    LaunchedEffect(exitRequested) {
        if (exitRequested) {
            if (hasUnsavedChanges) showExitConfirm = true else onExitConfirm()
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val width = maxWidth
        val baseScale = if (width < 600.dp) 0.85f else 1.0f
        val effectiveScale = uiScale * baseScale

        DisposableEffect(Unit) {
            onDispose { engine.cleanup() }
        }

        EditorTheme {
            Box(modifier = Modifier.fillMaxSize()) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    if (stringsLoaded) {
                        EditorScreen(
                            engine = engine,
                            uiScale = effectiveScale,
                            onSave = { pendingFileAction = "save" },
                            onLoad = { pendingFileAction = "open" },
                            onExportGif = { pendingExportFormat = "gif" },
                            onExportPng = { pendingExportFormat = "png" },
                            onExportAvi = { pendingExportFormat = "avi" },
                            onNewProject = { showNewProject = true },
                            onSettings = { showSettings = true }
                        )

                        if (showSettings) {
                            SettingsDialog(
                                engine = engine,
                                uiScale = uiScale,
                                onUiScaleChange = { 
                                    uiScale = it
                                    AppSettingsManager.setUiScale(it)
                                },
                                onClose = { showSettings = false }
                            )
                        }

                        if (showNewProject) {
                            NewProjectDialog(
                                onCancel = { showNewProject = false },
                                onCreate = { project -> 
                                    engine.setProject(project)
                                    showNewProject = false 
                                }
                            )
                        }

                        if (showExitConfirm) {
                            ConfirmExitDialog(
                                onSaveAndExit = { 
                                    pendingFileAction = "save"
                                    showExitConfirm = false
                                },
                                onExitWithoutSaving = { 
                                    showExitConfirm = false
                                    onExitConfirm()
                                },
                                onDismiss = { 
                                    showExitConfirm = false
                                    onExitCancel()
                                }
                            )
                        }

                        if (pendingExportFormat != null) {
                            ExportDialog(
                                engine = engine,
                                format = pendingExportFormat!!,
                                onCancel = { pendingExportFormat = null },
                                onExport = { name, w, h, fmt ->
                                    pendingFileAction = "export|$name|$w|$h|$fmt"
                                    pendingExportFormat = null
                                }
                            )
                        }

                        // Файловые операции
                        when {
                            pendingFileAction == "save" -> {
                                FileManagerDialog(FileDialogMode.SAVE, engine.project.value.name, "maryme") { path ->
                                    if (path != null) {
                                        val data = ProjectSerializer.serializeToBytes(engine.project.value)
                                        if (fileHandler.saveToPath(path, data)) {
                                            engine.markAsSaved()
                                            AppSettingsManager.addRecentProject(path, engine.project.value.name)
                                        }
                                    }
                                    pendingFileAction = null
                                }
                            }
                            pendingFileAction == "open" -> {
                                FileManagerDialog(FileDialogMode.OPEN, "", "maryme") { path ->
                                    if (path != null) {
                                        val data = fileHandler.readFromPath(path)
                                        if (data != null) {
                                            try { 
                                                val proj = ProjectSerializer.deserializeFromBytes(data)
                                                engine.setProject(proj)
                                                AppSettingsManager.addRecentProject(path, proj.name)
                                            } catch (e: Exception) { e.printStackTrace() }
                                        }
                                    }
                                    pendingFileAction = null
                                }
                            }
                            pendingFileAction?.startsWith("export") == true -> {
                                val parts = pendingFileAction!!.split("|")
                                val name = parts[1]; val fmt = parts[4]
                                FileManagerDialog(FileDialogMode.SAVE, name, fmt) { path ->
                                    if (path != null) {
                                        val data = ProjectSerializer.serializeToBytes(engine.project.value)
                                        fileHandler.saveToPath(path, data)
                                    }
                                    pendingFileAction = null
                                }
                            }
                        }
                    }
                }

                // Уведомление об автосохранении
                AnimatedVisibility(
                    visible = showAutosaveToast,
                    enter = fadeIn() + slideInVertically { it },
                    exit = fadeOut() + slideOutVertically { it },
                    modifier = Modifier.align(Alignment.BottomEnd).padding(32.dp)
                ) {
                    Surface(
                        color = EditorColors.accentGreen.copy(alpha = 0.9f),
                        shape = RoundedCornerShape(8.dp),
                        elevation = 8.dp
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(EditorIcons.iconCheck, null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(10.dp))
                            Text(EditorStrings.observeString("autosave.done"), color = Color.White, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}
