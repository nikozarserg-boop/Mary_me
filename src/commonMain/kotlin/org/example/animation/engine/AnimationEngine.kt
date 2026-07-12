package org.example.animation.engine

import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.Clock
import org.example.animation.io.AppSettingsManager
import org.example.animation.io.ProjectSerializer
import org.example.animation.io.createPlatformFileHandler
import org.example.animation.model.*
import kotlin.math.*
import kotlin.random.Random

class AnimationEngine(initialProject: AnimationProject = AnimationProject()) {
    val id = Clock.System.now().toEpochMilliseconds().toString() + "_" + Random.nextInt(1000, 9999)
    
    private val _project = MutableStateFlow(initialProject)
    val project: StateFlow<AnimationProject> = _project.asStateFlow()

    private val _hasUnsavedChanges = MutableStateFlow(false)
    val hasUnsavedChanges: StateFlow<Boolean> = _hasUnsavedChanges.asStateFlow()

    private val _isPinned = MutableStateFlow(false)
    val isPinned: StateFlow<Boolean> = _isPinned.asStateFlow()

    private val _filePath = MutableStateFlow<String?>(null)
    val filePath: StateFlow<String?> = _filePath.asStateFlow()

    private val _lastAutosaveTime = MutableStateFlow<Long>(0)
    val lastAutosaveTime: StateFlow<Long> = _lastAutosaveTime.asStateFlow()

    // Активный штрих, который рисуется в данный момент
    private val _activeStroke = MutableStateFlow<Stroke?>(null)
    val activeStroke: StateFlow<Stroke?> = _activeStroke.asStateFlow()

    // UI Visibility States (можно сделать глобальными, но для гибкости оставим тут)
    private val _isToolsVisible = MutableStateFlow(true)
    val isToolsVisible = _isToolsVisible.asStateFlow()
    private val _isLayersVisible = MutableStateFlow(true)
    val isLayersVisible = _isLayersVisible.asStateFlow()
    private val _isColorPickerVisible = MutableStateFlow(true)
    val isColorPickerVisible = _isColorPickerVisible.asStateFlow()
    private val _isTimelineVisible = MutableStateFlow(true)
    val isTimelineVisible = _isTimelineVisible.asStateFlow()
    private val _isPropertiesVisible = MutableStateFlow(true)
    val isPropertiesVisible = _isPropertiesVisible.asStateFlow()

    // UI Collapse States
    private val _isToolsCollapsed = MutableStateFlow(false)
    val isToolsCollapsed = _isToolsCollapsed.asStateFlow()
    private val _isLayersCollapsed = MutableStateFlow(false)
    val isLayersCollapsed = _isLayersCollapsed.asStateFlow()
    private val _isColorPickerCollapsed = MutableStateFlow(false)
    val isColorPickerCollapsed = _isColorPickerCollapsed.asStateFlow()
    private val _isTimelineCollapsed = MutableStateFlow(false)
    val isTimelineCollapsed = _isTimelineCollapsed.asStateFlow()
    private val _isPropertiesCollapsed = MutableStateFlow(false)
    val isPropertiesCollapsed = _isPropertiesCollapsed.asStateFlow()

    // Состояния интерфейса
    private val _currentFrameIndex = MutableStateFlow(0)
    val currentFrameIndex: StateFlow<Int> = _currentFrameIndex.asStateFlow()
    private val _currentLayerIndex = MutableStateFlow(0)
    val currentLayerIndex: StateFlow<Int> = _currentLayerIndex.asStateFlow()
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()
    private val _currentTool = MutableStateFlow(ToolType.BRUSH)
    val currentTool: StateFlow<ToolType> = _currentTool.asStateFlow()
    private val _currentColor = MutableStateFlow(0xFF000000uL)
    val currentColor: StateFlow<ULong> = _currentColor.asStateFlow()
    private val _brushSize = MutableStateFlow(4f)
    val brushSize: StateFlow<Float> = _brushSize.asStateFlow()
    private val _opacity = MutableStateFlow(1f)
    val opacity: StateFlow<Float> = _opacity.asStateFlow()
    
    // Сглаживание (Smoothing)
    private val _smoothingLevel = MutableStateFlow(1) 
    val smoothingLevel: StateFlow<Int> = _smoothingLevel.asStateFlow()
    private val _antiAliasingEnabled = MutableStateFlow(true)
    val antiAliasingEnabled: StateFlow<Boolean> = _antiAliasingEnabled.asStateFlow()

    // Кисти (Brushes)
    private val _brushes = MutableStateFlow(BrushManager.getPresets())
    val brushes: StateFlow<List<BrushPreset>> = _brushes.asStateFlow()
    private val _currentBrushIndex = MutableStateFlow(BrushManager.getCurrentIndex())
    val currentBrushIndex: StateFlow<Int> = _currentBrushIndex.asStateFlow()

    // Камера
    private val _zoom = MutableStateFlow(1f)
    val zoom: StateFlow<Float> = _zoom.asStateFlow()
    private val _panOffset = MutableStateFlow(Offset.Zero)
    val panOffset: StateFlow<Offset> = _panOffset.asStateFlow()
    private val _rotation = MutableStateFlow(0f) // В градусах
    val rotation: StateFlow<Float> = _rotation.asStateFlow()

    // Ghost Frames
    private val _ghostFramesEnabled = MutableStateFlow(true)
    val ghostFramesEnabled: StateFlow<Boolean> = _ghostFramesEnabled.asStateFlow()
    private val _ghostFramesBefore = MutableStateFlow(2)
    val ghostFramesBefore: StateFlow<Int> = _ghostFramesBefore.asStateFlow()
    private val _ghostFramesAfter = MutableStateFlow(1)
    val ghostFramesAfter: StateFlow<Int> = _ghostFramesAfter.asStateFlow()

    // История
    private val undoStack = mutableListOf<AnimationProject>()
    private val redoStack = mutableListOf<AnimationProject>()
    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()
    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

    private var playbackJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val fileHandler = createPlatformFileHandler()

    private var lastSessionTimestamp: Long = Clock.System.now().toEpochMilliseconds()
    private var workTimeJob: Job? = null
    private var autoSaveJob: Job? = null

    init {
        startWorkTimer()
        startAutoSaveTimer()
    }

    private fun startWorkTimer() {
        workTimeJob = scope.launch {
            while (isActive) {
                delay(10000)
                val now = Clock.System.now().toEpochMilliseconds()
                _project.value.workingTimeMs += (now - lastSessionTimestamp)
                lastSessionTimestamp = now
            }
        }
    }

    private fun startAutoSaveTimer() {
        autoSaveJob = scope.launch {
            while (isActive) {
                val intervalMs = AppSettingsManager.getAutoSaveInterval() * 60 * 1000L
                delay(intervalMs)
                if (AppSettingsManager.isAutoSaveEnabled() && _hasUnsavedChanges.value) {
                    performAutosave()
                }
            }
        }
    }

    private fun performAutosave() {
        try {
            val data = ProjectSerializer.serializeToBytes(_project.value)
            val path = fileHandler.getCacheDirectory() + "/autosave_${id}.maryme"
            if (fileHandler.saveToPath(path, data)) {
                _lastAutosaveTime.value = Clock.System.now().toEpochMilliseconds()
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun markAsSaved(path: String? = null) { 
        _hasUnsavedChanges.value = false 
        if (path != null) _filePath.value = path
    }
    
    fun setFilePath(path: String?) { _filePath.value = path }
    fun togglePinned() { _isPinned.value = !_isPinned.value }

    fun setProject(newProject: AnimationProject) {
        saveUndoState()
        _project.value = newProject
        _currentFrameIndex.value = 0
        _currentLayerIndex.value = 0
        _hasUnsavedChanges.value = false
        lastSessionTimestamp = Clock.System.now().toEpochMilliseconds()
    }

    fun setFps(fps: Int) {
        if (fps == _project.value.fps) return
        val newProject = _project.value.copy()
        newProject.fps = fps.coerceIn(1, 120)
        _project.value = newProject
        _hasUnsavedChanges.value = true
        if (_isPlaying.value) { pause(); play() }
    }

    fun toggleTools() { _isToolsVisible.value = !_isToolsVisible.value }
    fun setToolsVisible(visible: Boolean) { _isToolsVisible.value = visible }
    fun toggleLayers() { _isLayersVisible.value = !_isLayersVisible.value }
    fun setLayersVisible(visible: Boolean) { _isLayersVisible.value = visible }
    fun toggleColorPicker() { _isColorPickerVisible.value = !_isColorPickerVisible.value }
    fun setColorPickerVisible(visible: Boolean) { _isColorPickerVisible.value = visible }
    fun toggleTimeline() { _isTimelineVisible.value = !_isTimelineVisible.value }
    fun setTimelineVisible(visible: Boolean) { _isTimelineVisible.value = visible }
    fun toggleProperties() { _isPropertiesVisible.value = !_isPropertiesVisible.value }
    fun setPropertiesVisible(visible: Boolean) { _isPropertiesVisible.value = visible }

    fun setToolsCollapsed(collapsed: Boolean) { _isToolsCollapsed.value = collapsed }
    fun setLayersCollapsed(collapsed: Boolean) { _isLayersCollapsed.value = collapsed }
    fun setColorPickerCollapsed(collapsed: Boolean) { _isColorPickerCollapsed.value = collapsed }
    fun setTimelineCollapsed(collapsed: Boolean) { _isTimelineCollapsed.value = collapsed }
    fun setPropertiesCollapsed(collapsed: Boolean) { _isPropertiesCollapsed.value = collapsed }

    fun setCurrentFrame(index: Int) { if (index in 0 until _project.value.maxFrames) _currentFrameIndex.value = index }
    fun setCurrentLayer(index: Int) { if (index in _project.value.layers.indices) _currentLayerIndex.value = index }
    fun setTool(tool: ToolType) { _currentTool.value = tool }
    fun setCurrentColor(color: ULong) { _currentColor.value = color }
    fun setBrushSize(size: Float) { _brushSize.value = size.coerceIn(1f, 100f) }
    fun setOpacity(opacity: Float) { _opacity.value = opacity.coerceIn(0f, 1f) }
    fun setSmoothingLevel(level: Int) { _smoothingLevel.value = level.coerceIn(0, 3) }
    fun setAntiAliasingEnabled(enabled: Boolean) { _antiAliasingEnabled.value = enabled }
    
    fun setZoom(zoom: Float) { _zoom.value = zoom.coerceIn(0.001f, 1000f) }
    
    fun setPanOffset(offset: Offset) { _panOffset.value = offset }
    fun setRotation(deg: Float) {
        // Нормализуем в диапазон (-180°, 180°], чтобы при полном обороте
        // значение сбрасывалось к 0 вместо накопления 360/720/...
        var normalized = deg % 360f
        if (normalized > 180f) normalized -= 360f
        if (normalized <= -180f) normalized += 360f
        _rotation.value = normalized
    }

    fun setGhostFramesEnabled(enabled: Boolean) { _ghostFramesEnabled.value = enabled }
    fun setGhostFramesFramesBefore(count: Int) { _ghostFramesBefore.value = count }
    fun setGhostFramesFramesAfter(count: Int) { _ghostFramesAfter.value = count }

    private fun isWithinCanvas(point: Offset): Boolean {
        return point.x >= 0 && point.x <= _project.value.canvasWidth &&
               point.y >= 0 && point.y <= _project.value.canvasHeight
    }

    fun startStroke(point: Offset) {
        if (!isWithinCanvas(point)) return
        
        val layer = _project.value.layers.getOrNull(_currentLayerIndex.value) ?: return
        if (layer.isLocked || !layer.isVisible) return
        
        _activeStroke.value = Stroke(
            points = mutableListOf(point),
            color = _currentColor.value,
            strokeWidth = _brushSize.value,
            isEraser = _currentTool.value == ToolType.ERASER,
            toolType = _currentTool.value,
            opacity = _opacity.value
        )
    }

    fun continueStroke(point: Offset) {
        val stroke = _activeStroke.value ?: return
        
        val clampedPoint = Offset(
            point.x.coerceIn(0f, _project.value.canvasWidth.toFloat()),
            point.y.coerceIn(0f, _project.value.canvasHeight.toFloat())
        )
        
        val lastPoint = stroke.points.lastOrNull()
        if (lastPoint != null) {
            val distSq = (clampedPoint.x - lastPoint.x) * (clampedPoint.x - lastPoint.x) + 
                         (clampedPoint.y - lastPoint.y) * (clampedPoint.y - lastPoint.y)
            val threshold = when(_smoothingLevel.value) {
                0 -> 0.1f
                1 -> 2.0f
                2 -> 6.0f
                3 -> 15.0f
                else -> 1f
            }
            if (distSq < threshold) return
        }
        
        val newPoints = stroke.points.toMutableList()
        newPoints.add(clampedPoint)
        _activeStroke.value = stroke.copy(points = newPoints)
    }

    fun endStroke() {
        val stroke = _activeStroke.value ?: return
        _activeStroke.value = null
        
        saveUndoState()
        val currentProject = _project.value
        currentProject.ensureFrameCount(_currentFrameIndex.value + 1)
        currentProject.layers[_currentLayerIndex.value].frames[_currentFrameIndex.value].strokes.add(stroke)
        
        currentProject.lastModifiedTimestamp = Clock.System.now().toEpochMilliseconds()
        _project.value = currentProject.copy()
        _hasUnsavedChanges.value = true
    }

    fun importImage() {
        val data = fileHandler.openFile("png") ?: fileHandler.openFile("jpg") ?: return
        importImageFromData(data)
    }

    fun importImageFromPath(path: String) {
        val data = fileHandler.readFromPath(path) ?: return
        importImageFromData(data)
    }

    private fun importImageFromData(data: ByteArray) {
        saveUndoState()
        val currentProject = _project.value
        currentProject.ensureFrameCount(_currentFrameIndex.value + 1)
        val frame = currentProject.layers[_currentLayerIndex.value].frames[_currentFrameIndex.value]
        frame.images.add(ImageElement(data = data))

        currentProject.lastModifiedTimestamp = Clock.System.now().toEpochMilliseconds()
        _project.value = currentProject.copy()
        _hasUnsavedChanges.value = true
    }

    fun addLayer() { 
        saveUndoState(); _project.value.addLayer(); _project.value = _project.value.copy()
        _currentLayerIndex.value = _project.value.layers.size - 1; _hasUnsavedChanges.value = true 
    }
    fun removeLayer(index: Int) {
        if (index in _project.value.layers.indices && _project.value.layers.size > 1) {
            saveUndoState(); _project.value.removeLayer(index); _project.value = _project.value.copy()
            _currentLayerIndex.value = (_project.value.layers.size - 1).coerceAtLeast(0)
            _hasUnsavedChanges.value = true
        }
    }
    fun moveLayer(from: Int, to: Int) {
        if (from == to || from !in _project.value.layers.indices || to !in _project.value.layers.indices) return
        saveUndoState()
        _project.value.moveLayer(from, to)
        _project.value = _project.value.copy()
        _currentLayerIndex.value = to
        _hasUnsavedChanges.value = true
    }
    fun moveLayerUp(index: Int) { if (index < _project.value.layers.size - 1) moveLayer(index, index + 1) }
    fun moveLayerDown(index: Int) { if (index > 0) moveLayer(index, index - 1) }

    fun setLayerVisible(index: Int, visible: Boolean) { if (index in _project.value.layers.indices) { _project.value.layers[index].isVisible = visible; _project.value = _project.value.copy(); _hasUnsavedChanges.value = true } }
    fun setLayerLocked(index: Int, locked: Boolean) { if (index in _project.value.layers.indices) { _project.value.layers[index].isLocked = locked; _project.value = _project.value.copy(); _hasUnsavedChanges.value = true } }
    fun renameLayer(index: Int, newName: String) {
        if (index in _project.value.layers.indices) {
            val trimmed = newName.trim()
            if (trimmed.isEmpty()) return
            saveUndoState()
            _project.value.layers[index].name = trimmed
            _project.value = _project.value.copy()
            _hasUnsavedChanges.value = true
        }
    }

    fun addFrame() { saveUndoState(); _project.value.addFrame(_currentLayerIndex.value); _project.value = _project.value.copy(); _currentFrameIndex.value = _project.value.maxFrames - 1; _hasUnsavedChanges.value = true }
    fun duplicateFrame() { saveUndoState(); val next = _project.value.duplicateFrame(_currentLayerIndex.value, _currentFrameIndex.value); _project.value = _project.value.copy(); _currentFrameIndex.value = next; _hasUnsavedChanges.value = true }
    fun removeFrame() { saveUndoState(); _project.value.removeFrame(_currentLayerIndex.value, _currentFrameIndex.value); _project.value = _project.value.copy(); _currentFrameIndex.value = (_project.value.maxFrames - 1).coerceAtLeast(0); _hasUnsavedChanges.value = true }
    fun clearFrame() { saveUndoState(); _project.value.layers.getOrNull(_currentLayerIndex.value)?.frames?.getOrNull(_currentFrameIndex.value)?.clear(); _project.value = _project.value.copy(); _hasUnsavedChanges.value = true }

    fun togglePlayback() { if (_isPlaying.value) pause() else play() }
    fun play() {
        if (_isPlaying.value) return
        _isPlaying.value = true
        playbackJob = scope.launch {
            while (isActive && _isPlaying.value) {
                val delayMs = (1000 / _project.value.fps.coerceAtLeast(1)).toLong()
                delay(delayMs)
                var next = _currentFrameIndex.value + 1
                if (next >= _project.value.maxFrames) next = 0
                _currentFrameIndex.value = next
            }
        }
    }
    fun pause() { _isPlaying.value = false; playbackJob?.cancel() }
    
    fun goToFirstFrame() { _currentFrameIndex.value = 0 }
    fun goToLastFrame() { _currentFrameIndex.value = _project.value.maxFrames - 1 }
    fun goToPreviousFrame() { if (_currentFrameIndex.value > 0) _currentFrameIndex.value-- }
    fun goToNextFrame() { if (_currentFrameIndex.value < _project.value.maxFrames - 1) _currentFrameIndex.value++ }

    private fun saveUndoState() {
        undoStack.add(_project.value.copy())
        if (undoStack.size > 50) undoStack.removeFirst()
        redoStack.clear()
        _canUndo.value = true
        _canRedo.value = false
    }
    fun undo() { if (undoStack.isNotEmpty()) { redoStack.add(_project.value.copy()); _project.value = undoStack.removeLast(); _canUndo.value = undoStack.isNotEmpty(); _canRedo.value = true; _hasUnsavedChanges.value = true } }
    fun redo() { if (redoStack.isNotEmpty()) { undoStack.add(_project.value.copy()); _project.value = redoStack.removeLast(); _canUndo.value = true; _canRedo.value = redoStack.isNotEmpty(); _hasUnsavedChanges.value = true } }

    data class GhostFrame(val strokes: List<Stroke>, val images: List<ImageElement>, val opacity: Float, val isCurrent: Boolean)
    fun getFramesForRendering(): List<GhostFrame> {
        val current = _currentFrameIndex.value
        val result = mutableListOf<GhostFrame>()
        if (!_ghostFramesEnabled.value) {
            val strokes = _project.value.layers.filter { it.isVisible }.flatMap { it.frames.getOrNull(current)?.strokes ?: emptyList() }
            val images = _project.value.layers.filter { it.isVisible }.flatMap { it.frames.getOrNull(current)?.images ?: emptyList() }
            return listOf(GhostFrame(strokes, images, 1f, true))
        }
        for (i in _ghostFramesBefore.value downTo 1) {
            val idx = current - i
            if (idx >= 0) {
                val strokes = _project.value.layers.filter { it.isVisible }.flatMap { it.frames.getOrNull(idx)?.strokes ?: emptyList() }
                val images = _project.value.layers.filter { it.isVisible }.flatMap { it.frames.getOrNull(idx)?.images ?: emptyList() }
                result.add(GhostFrame(strokes, images, 0.2f / i, false))
            }
        }
        val currentStrokes = _project.value.layers.filter { it.isVisible }.flatMap { it.frames.getOrNull(current)?.strokes ?: emptyList() }
        val currentImages = _project.value.layers.filter { it.isVisible }.flatMap { it.frames.getOrNull(current)?.images ?: emptyList() }
        result.add(GhostFrame(currentStrokes, currentImages, 1f, true))
        for (i in 1.._ghostFramesAfter.value) {
            val idx = current + i
            if (idx < _project.value.maxFrames) {
                val strokes = _project.value.layers.filter { it.isVisible }.flatMap { it.frames.getOrNull(idx)?.strokes ?: emptyList() }
                val images = _project.value.layers.filter { it.isVisible }.flatMap { it.frames.getOrNull(idx)?.images ?: emptyList() }
                result.add(GhostFrame(strokes, images, 0.2f / i, false))
            }
        }
        return result
    }

    // Кисти (Brushes)
    fun selectBrush(index: Int) {
        if (index in _brushes.value.indices) {
            BrushManager.setCurrent(index)
            _currentBrushIndex.value = index
        }
    }

    fun removeBrush(index: Int) {
        if (index in _brushes.value.indices && _brushes.value.size > 1) {
            BrushManager.removePreset(index)
            _brushes.value = BrushManager.getPresets()
            _currentBrushIndex.value = BrushManager.getCurrentIndex()
        }
    }

    fun importBrushesFromFile() {
        val data = fileHandler.openFile("json") ?: return
        val text = data.decodeToString()
        val imported = BrushManager.loadFromJson(text)
        if (imported.isNotEmpty()) {
            imported.forEach { BrushManager.addPreset(it) }
            _brushes.value = BrushManager.getPresets()
            _currentBrushIndex.value = BrushManager.getCurrentIndex()
        }
    }

    fun exportCurrentBrushToFile() {
        val json = BrushManager.exportCurrentToJson()
        fileHandler.saveFile("brush_preset", "json", json.encodeToByteArray())
    }

    fun cleanup() {
        workTimeJob?.cancel(); playbackJob?.cancel(); autoSaveJob?.cancel(); scope.cancel()
    }
}
