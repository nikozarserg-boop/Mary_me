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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.example.mary_me.generated.resources.Res
import org.example.animation.engine.AnimationEngine
import org.example.animation.engine.ProjectManager
import org.example.animation.engine.ExportManager
import org.example.animation.io.AppSettingsManager
import org.example.animation.io.hasStoragePermissions
import org.example.animation.io.requestStoragePermissions
import org.example.animation.io.ProjectSerializer
import org.example.animation.io.createPlatformFileHandler
import org.example.animation.localization.EditorStrings
import org.example.animation.ui.components.*
import org.example.animation.ui.components.tooltip.LocalTooltipManager
import org.example.animation.ui.components.tooltip.TooltipHost
import org.example.animation.ui.components.tooltip.TooltipManager
import org.example.animation.ui.theme.*
import org.jetbrains.compose.resources.ExperimentalResourceApi

@OptIn(ExperimentalResourceApi::class)
@Composable
fun App(
    exitRequested: Boolean = false,
    onExitCancel: () -> Unit = {},
    onExitConfirm: () -> Unit = {}
) {
    val projectManager = remember { ProjectManager() }
    val engine by projectManager.activeEngine.collectAsState()
    val fileHandler = remember { createPlatformFileHandler() }
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    
    var showSettings by remember { mutableStateOf(false) }
    var showNewProject by remember { mutableStateOf(false) }
    var showExitConfirm by remember { mutableStateOf(false) }
    var hasPermissions by remember { mutableStateOf(false) }
    
    var closingTabIndex by remember { mutableStateOf<Int?>(null) }
    
    var pendingExportFormat by remember { mutableStateOf<String?>(null) }
    var pendingFileAction by remember { mutableStateOf<String?>(null) }
    var exportProgress by remember { mutableStateOf<Float?>(null) }
    var stringsLoaded by remember { mutableStateOf(false) }

    val hasUnsavedChanges by engine.hasUnsavedChanges.collectAsState()
    val lastAutosave by engine.lastAutosaveTime.collectAsState()
    var showAutosaveToast by remember { mutableStateOf(false) }

    var savedUiScale by remember { mutableStateOf(AppSettingsManager.getUiScale()) }
    var currentTheme by remember { mutableStateOf(ThemeType.valueOf(AppSettingsManager.getTheme())) }

    // Сайд-эффект для обработки действий с файлами, не требующих диалога
    LaunchedEffect(pendingFileAction) {
        val action = pendingFileAction ?: return@LaunchedEffect
        if (action == "save" || action == "save_and_close_tab") {
            val currentPath = engine.filePath.value
            if (currentPath != null) {
                val data = ProjectSerializer.serializeToBytes(engine.project.value)
                if (fileHandler.saveToPath(currentPath, data)) {
                    engine.markAsSaved(currentPath)
                    if (action == "save_and_close_tab") {
                        val idx = closingTabIndex
                        if (idx != null) {
                            projectManager.closeProject(idx)
                            closingTabIndex = null
                        }
                    }
                }
                pendingFileAction = null
            }
        }
    }

    LaunchedEffect(Unit) {
        if (!hasStoragePermissions()) {
            requestStoragePermissions { granted ->
                hasPermissions = granted
            }
        } else {
            hasPermissions = true
        }
    }

    LaunchedEffect(lastAutosave) {
        if (lastAutosave > 0) {
            showAutosaveToast = true
            delay(3000)
            showAutosaveToast = false
        }
    }

    LaunchedEffect(Unit) {
        AppSettingsManager.load()
        savedUiScale = AppSettingsManager.getUiScale()
        
        try {
            val ruBytes = try { Res.readBytes("files/locales/strings_ru.json") } catch (e: Exception) { try { Res.readBytes("locales/strings_ru.json") } catch (e2: Exception) { null } }
            val enBytes = try { Res.readBytes("files/locales/strings_en.json") } catch (e: Exception) { try { Res.readBytes("locales/strings_en.json") } catch (e2: Exception) { null } }

            ruBytes?.let { EditorStrings.loadStrings("ru", it.decodeToString()) }
            enBytes?.let { EditorStrings.loadStrings("en", it.decodeToString()) }
        } catch (e: Exception) {
            println("Localization loading error: ${e.message}")
        } finally {
            stringsLoaded = true
        }
    }

    LaunchedEffect(exitRequested) {
        if (exitRequested) {
            val anyUnsaved = projectManager.engines.value.any { it.hasUnsavedChanges.value }
            if (anyUnsaved) showExitConfirm = true else onExitConfirm()
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val screenWidth = maxWidth
        val screenHeight = maxHeight
        
        val autoScale = remember(screenWidth, screenHeight) {
            val baseWidth = 1280.dp
            val baseHeight = 720.dp
            val widthRatio = (screenWidth / baseWidth)
            val heightRatio = (screenHeight / baseHeight)
            (minOf(widthRatio, heightRatio)).coerceIn(0.75f, 1.4f)
        }

        val effectiveScale = if (savedUiScale <= 0f) autoScale else savedUiScale

        DisposableEffect(Unit) {
            onDispose { projectManager.engines.value.forEach { it.cleanup() } }
        }

        CompositionLocalProvider(LocalTooltipManager provides remember { TooltipManager() }) {
            EditorTheme(themeType = currentTheme, uiScale = effectiveScale) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Surface(modifier = Modifier.fillMaxSize(), color = EditorColors.background) {
                        if (stringsLoaded) {
                            EditorScreen(
                                engine = engine,
                                projectManager = projectManager,
                                uiScale = effectiveScale,
                                onSave = { pendingFileAction = "save" },
                                onLoad = { pendingFileAction = "open" },
                                onExportGif = { pendingExportFormat = "gif" },
                                onExportPng = { pendingExportFormat = "png" },
                                onExportAvi = { pendingExportFormat = "avi" },
                                onExportMp4 = { pendingExportFormat = "mp4" },
                                onNewProject = { showNewProject = true },
                                onSettings = { showSettings = true },
                                onImportImage = { pendingFileAction = "import_image" },
                                currentTheme = currentTheme,
                                onThemeChange = { currentTheme = it },
                                onCloseTab = { index ->
                                    val targetEngine = projectManager.engines.value[index]
                                    if (targetEngine.hasUnsavedChanges.value) {
                                        closingTabIndex = index
                                    } else {
                                        projectManager.closeProject(index)
                                    }
                                }
                            )

                            if (showSettings) {
                                SettingsDialog(
                                    engine = engine,
                                    uiScale = savedUiScale,
                                    currentTheme = currentTheme,
                                    onUiScaleChange = { 
                                        savedUiScale = it
                                        AppSettingsManager.setUiScale(it)
                                    },
                                    onThemeChange = { 
                                        currentTheme = it
                                        AppSettingsManager.setTheme(it.name)
                                    },
                                    onClose = { showSettings = false }
                                )
                            }

                            if (showNewProject) {
                                NewProjectDialog(
                                    onCancel = { showNewProject = false },
                                    onCreate = { project -> 
                                        projectManager.addProject(project)
                                        showNewProject = false 
                                    }
                                )
                            }

                            if (showExitConfirm) {
                                ConfirmExitDialog(
                                    onSaveAndExit = { 
                                        pendingFileAction = "save_all_exit"
                                        showExitConfirm = false 
                                    },
                                    onExitWithoutSaving = { showExitConfirm = false; onExitConfirm() },
                                    onDismiss = { showExitConfirm = false; onExitCancel() }
                                )
                            }

                            if (closingTabIndex != null) {
                                val idx = closingTabIndex!!
                                ConfirmExitDialog(
                                    onSaveAndExit = { 
                                        projectManager.setActiveProject(idx)
                                        pendingFileAction = "save_and_close_tab"
                                    },
                                    onExitWithoutSaving = { 
                                        projectManager.closeProject(idx)
                                        closingTabIndex = null 
                                    },
                                    onDismiss = { closingTabIndex = null }
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

                            // Диалоги выбора файлов (только те, что показывают UI)
                            when {
                                (pendingFileAction == "save" || pendingFileAction == "save_and_close_tab") && engine.filePath.value == null -> {
                                    FileManagerDialog(FileDialogMode.SAVE, engine.project.value.name, "maryme") { path ->
                                        if (path != null) {
                                            val data = ProjectSerializer.serializeToBytes(engine.project.value)
                                            if (fileHandler.saveToPath(path, data)) {
                                                engine.markAsSaved(path)
                                                AppSettingsManager.addRecentProject(path, engine.project.value.name)
                                                if (pendingFileAction == "save_and_close_tab") {
                                                    closingTabIndex?.let { projectManager.closeProject(it) }
                                                    closingTabIndex = null
                                                }
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
                                                    projectManager.addProject(proj, path)
                                                    AppSettingsManager.addRecentProject(path, proj.name)
                                                } catch (e: Exception) { e.printStackTrace() }
                                            }
                                        }
                                        pendingFileAction = null
                                    }
                                }
                                pendingFileAction == "import_image" -> {
                                    FileManagerDialog(FileDialogMode.OPEN, "", "png") { path ->
                                        if (path != null) {
                                            engine.importImageFromPath(path)
                                        }
                                        pendingFileAction = null
                                    }
                                }
                                pendingFileAction?.startsWith("export") == true -> {
                                    val parts = pendingFileAction!!.split("|")
                                    val name = parts[1]
                                    val w = parts[2].toInt()
                                    val h = parts[3].toInt()
                                    val fmt = parts[4]
                                    
                                    FileManagerDialog(FileDialogMode.SAVE, name, fmt) { path ->
                                        if (path != null) {
                                            if (fmt == "png") {
                                                val data = ExportManager.exportFrameToPng(
                                                    project = engine.project.value,
                                                    frameIndex = engine.currentFrameIndex.value,
                                                    width = w,
                                                    height = h,
                                                    density = density
                                                )
                                                fileHandler.saveToPath(path, data)
                                                pendingFileAction = null
                                            } else {
                                                scope.launch {
                                                    exportProgress = 0f
                                                    fileHandler.exportAnimation(
                                                        project = engine.project.value,
                                                        outputPath = path,
                                                        format = fmt,
                                                        width = w,
                                                        height = h,
                                                        fps = engine.project.value.fps,
                                                        density = density,
                                                        onProgress = { exportProgress = it }
                                                    )
                                                    exportProgress = null
                                                    pendingFileAction = null
                                                }
                                            }
                                        } else {
                                            pendingFileAction = null
                                        }
                                    }
                                }
                            }
                        } else {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = EditorColors.accent)
                            }
                        }
                    }

                    if (exportProgress != null) {
                        Box(
                            modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp.scaled()),
                                color = EditorColors.surface,
                                elevation = 8.dp.scaled(),
                                modifier = Modifier.width(280.dp.scaled()).padding(24.dp.scaled())
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(24.dp.scaled())
                                ) {
                                    Text(
                                        EditorStrings.observeString("export.title") + "...", 
                                        color = EditorColors.textPrimary, 
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(Modifier.height(16.dp.scaled()))
                                    LinearProgressIndicator(
                                        progress = exportProgress!!,
                                        modifier = Modifier.fillMaxWidth().height(8.dp.scaled()),
                                        color = EditorColors.accent,
                                        backgroundColor = EditorColors.divider
                                    )
                                    Spacer(Modifier.height(8.dp.scaled()))
                                    Text(
                                        "${(exportProgress!! * 100).toInt()}%", 
                                        color = EditorColors.textSecondary,
                                        fontSize = 12.sp.scaled()
                                    )
                                }
                            }
                        }
                    }

                    AnimatedVisibility(
                        visible = showAutosaveToast,
                        enter = fadeIn() + slideInVertically { it },
                        exit = fadeOut() + slideOutVertically { it },
                        modifier = Modifier.align(Alignment.BottomEnd).padding(32.dp.scaled())
                    ) {
                        Surface(
                            color = EditorColors.accentGreen.copy(alpha = 0.9f),
                            shape = RoundedCornerShape(8.dp.scaled()),
                            elevation = 8.dp.scaled()
                        ) {
                            Row(modifier = Modifier.padding(horizontal = 16.dp.scaled(), vertical = 10.dp.scaled()), verticalAlignment = Alignment.CenterVertically) {
                                Icon(EditorIcons.iconCheck, null, tint = Color.White, modifier = Modifier.size(16.dp.scaled()))
                                Spacer(Modifier.width(10.dp.scaled()))
                                Text(EditorStrings.observeString("autosave.done"), color = Color.White, fontSize = 13.sp.scaled())
                            }
                        }
                    }
                }
                TooltipHost()
            }
        }
    }
}
