package org.example

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import org.example.animation.engine.AnimationEngine
import org.example.animation.io.ProjectSerializer
import org.example.animation.io.createPlatformFileHandler
import org.example.animation.model.AnimationProject
import org.example.animation.ui.components.EditorScreen
import org.example.animation.ui.components.FileDialogMode
import org.example.animation.ui.components.FileManagerDialog
import org.example.animation.ui.components.NewProjectDialog
import org.example.animation.ui.components.SettingsDialog
import org.example.animation.ui.theme.EditorTheme

@Composable
fun App() {
    val engine = remember { AnimationEngine() }
    val fileHandler = remember { createPlatformFileHandler() }
    var showSettings by remember { mutableStateOf(false) }
    var showNewProject by remember { mutableStateOf(false) }
    var pendingExport by remember { mutableStateOf<String?>(null) }

    var uiScale by remember { mutableStateOf(1f) }

    DisposableEffect(Unit) {
        onDispose { engine.cleanup() }
    }

    EditorTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            EditorScreen(
                engine = engine,
                uiScale = uiScale,
                onSave = { pendingExport = "maryme" },
                onLoad = { pendingExport = "open" },
                onExportGif = { pendingExport = "gif" },
                onExportPng = { pendingExport = "png" },
                onExportAvi = { pendingExport = "avi" },
                onNewProject = { showNewProject = true },
                onSettings = { showSettings = true }
            )

            if (showSettings) {
                SettingsDialog(
                    engine = engine,
                    uiScale = uiScale,
                    onUiScaleChange = { uiScale = it },
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

            when (pendingExport) {
                "maryme" -> FileManagerDialog(FileDialogMode.SAVE, engine.project.value.name, "maryme") { path ->
                    if (path != null) {
                        val data = ProjectSerializer.serializeToBytes(engine.project.value)
                        fileHandler.saveToPath(path, data)
                    }
                    pendingExport = null
                }
                "open" -> FileManagerDialog(FileDialogMode.OPEN, "", "maryme") { path ->
                    if (path != null) {
                        val data = fileHandler.readFromPath(path)
                        if (data != null) {
                            try { engine.setProject(ProjectSerializer.deserializeFromBytes(data)) } catch (e: Exception) { e.printStackTrace() }
                        }
                    }
                    pendingExport = null
                }
                "gif", "png", "avi" -> FileManagerDialog(FileDialogMode.SAVE, engine.project.value.name, pendingExport!!) { path ->
                    if (path != null) {
                        val data = ProjectSerializer.serializeToBytes(engine.project.value)
                        fileHandler.saveToPath(path, data)
                    }
                    pendingExport = null
                }
            }
        }
    }
}