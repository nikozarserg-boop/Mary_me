package org.example.animation.io

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.example.animation.localization.EditorStrings
import kotlinx.datetime.Clock

@Serializable
data class RecentProject(
    val path: String,
    val name: String,
    val lastAccessed: Long
)

@Serializable
data class AppSettings(
    var uiScale: Float = 0.0f, // 0.0 означает Авто
    var language: String = "ru",
    var theme: String = "DARK", // Текущая тема (DARK, LIGHT, GREY, GLASS)
    var recentProjects: MutableList<RecentProject> = mutableListOf(),
    var autoSaveEnabled: Boolean = true,
    var autoSaveIntervalMin: Int = 2,
    var ghostFramesColor: ULong = 0xFF0099FFuL,
    var ghostFramesEnabled: Boolean = true,
    var ghostFramesBefore: Int = 2,
    var ghostFramesAfter: Int = 1,
    var brushSize: Float = 4f,
    var smoothingLevel: Int = 1,
    var antiAliasingEnabled: Boolean = true,
    var lastActiveTool: String = "BRUSH",
    var currentColor: ULong = 0xFF000000uL,
    var currentOpacity: Float = 1.0f,
    var currentBrushIndex: Int = 0
)

object AppSettingsManager {
    private var settings = AppSettings()
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; prettyPrint = true }
    private val handler = createPlatformFileHandler()
    private const val SETTINGS_FILE_NAME = "settings.json"

    private fun getSettingsFilePath(): String {
        return handler.getCacheDirectory() + "/../" + SETTINGS_FILE_NAME
    }

    fun load() {
        try {
            val path = getSettingsFilePath()
            if (handler.fileExists(path)) {
                val bytes = handler.readFromPath(path)
                if (bytes != null) {
                    settings = json.decodeFromString(AppSettings.serializer(), bytes.decodeToString())
                    EditorStrings.setLanguage(settings.language)
                }
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun save() {
        try {
            val path = getSettingsFilePath()
            val content = json.encodeToString(AppSettings.serializer(), settings)
            handler.saveToPath(path, content.encodeToByteArray())
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun exportConfig(targetPath: String): Boolean {
        val content = json.encodeToString(AppSettings.serializer(), settings)
        return handler.saveToPath(targetPath, content.encodeToByteArray())
    }

    fun importConfig(sourcePath: String): Boolean {
        return try {
            val bytes = handler.readFromPath(sourcePath)
            if (bytes != null) {
                settings = json.decodeFromString(AppSettings.serializer(), bytes.decodeToString())
                save()
                true
            } else false
        } catch (e: Exception) { false }
    }

    fun getUiScale() = settings.uiScale
    fun setUiScale(scale: Float) { settings.uiScale = scale; save() }
    fun getLanguage() = settings.language
    fun setLanguage(code: String) { settings.language = code; save() }

    fun getTheme(): String = settings.theme
    fun setTheme(theme: String) { settings.theme = theme; save() }
    fun isAutoSaveEnabled() = settings.autoSaveEnabled
    fun setAutoSaveEnabled(enabled: Boolean) { settings.autoSaveEnabled = enabled; save() }
    fun getAutoSaveInterval() = settings.autoSaveIntervalMin
    fun setAutoSaveInterval(min: Int) { settings.autoSaveIntervalMin = min; save() }

    fun getGhostFramesColor() = settings.ghostFramesColor
    fun setGhostFramesColor(color: ULong) { settings.ghostFramesColor = color; save() }
    
    fun getGhostFramesEnabled() = settings.ghostFramesEnabled
    fun setGhostFramesEnabled(enabled: Boolean) { settings.ghostFramesEnabled = enabled; save() }
    
    fun getGhostFramesBefore() = settings.ghostFramesBefore
    fun setGhostFramesBefore(count: Int) { settings.ghostFramesBefore = count; save() }
    
    fun getGhostFramesAfter() = settings.ghostFramesAfter
    fun setGhostFramesAfter(count: Int) { settings.ghostFramesAfter = count; save() }

    fun getBrushSize() = settings.brushSize
    fun setBrushSize(size: Float) { settings.brushSize = size; save() }

    fun getSmoothingLevel() = settings.smoothingLevel
    fun setSmoothingLevel(level: Int) { settings.smoothingLevel = level; save() }

    fun isAntiAliasingEnabled() = settings.antiAliasingEnabled
    fun setAntiAliasingEnabled(enabled: Boolean) { settings.antiAliasingEnabled = enabled; save() }

    fun getLastActiveTool() = settings.lastActiveTool
    fun setLastActiveTool(tool: String) { settings.lastActiveTool = tool; save() }

    fun getCurrentColor() = settings.currentColor
    fun setCurrentColor(color: ULong) { settings.currentColor = color; save() }

    fun getCurrentOpacity() = settings.currentOpacity
    fun setCurrentOpacity(opacity: Float) { settings.currentOpacity = opacity; save() }

    fun getCurrentBrushIndex() = settings.currentBrushIndex
    fun setCurrentBrushIndex(index: Int) { settings.currentBrushIndex = index; save() }

    fun addRecentProject(path: String, name: String) {
        settings.recentProjects.removeAll { it.path == path }
        settings.recentProjects.add(0, RecentProject(path, name, Clock.System.now().toEpochMilliseconds()))
        if (settings.recentProjects.size > 20) settings.recentProjects.removeLast()
        save()
    }

    fun getRecentProjects(): List<RecentProject> {
        val existing = settings.recentProjects.filter { handler.fileExists(it.path) }
        if (existing.size != settings.recentProjects.size) {
            settings.recentProjects.clear()
            settings.recentProjects.addAll(existing)
            save()
        }
        return settings.recentProjects
    }

    fun openSettingsFolder() {
        handler.openInExplorer(handler.getCacheDirectory() + "/../")
    }
}
