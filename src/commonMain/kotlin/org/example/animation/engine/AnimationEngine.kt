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

class AnimationEngine {
    private val _project = MutableStateFlow(AnimationProject())
    val project: StateFlow<AnimationProject> = _project.asStateFlow()

    private val _hasUnsavedChanges = MutableStateFlow(false)
    val hasUnsavedChanges: StateFlow<Boolean> = _hasUnsavedChanges.asStateFlow()

    private val _lastAutosaveTime = MutableStateFlow<Long>(0)
    val lastAutosaveTime: StateFlow<Long> = _lastAutosaveTime.asStateFlow()

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
    
    // Камера
    private val _zoom = MutableStateFlow(1f)
    val zoom: StateFlow<Float> = _zoom.asStateFlow()
    private val _panOffset = MutableStateFlow(Offset.Zero)
    val panOffset: StateFlow<Offset> = _panOffset.asStateFlow()

    // Onion Skin
    private val _onionSkinEnabled = MutableStateFlow(true)
    val onionSkinEnabled: StateFlow<Boolean> = _onionSkinEnabled.asStateFlow()
    private val _onionSkinFramesBefore = MutableStateFlow(2)
    val onionSkinFramesBefore: StateFlow<Int> = _onionSkinFramesBefore.asStateFlow()
    private val _onionSkinFramesAfter = MutableStateFlow(1)
    val onionSkinFramesAfter: StateFlow<Int> = _onionSkinFramesAfter.asStateFlow()

    // История
    private val undoStack = mutableListOf<AnimationProject>()
    private val redoStack = mutableListOf<AnimationProject>()
    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()
    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

    private var currentStroke: Stroke? = null
    private var playbackJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val fileHandler = createPlatformFileHandler()

    // Таймеры
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
            val path = fileHandler.getCacheDirectory() + "/autosave_backup.maryme"
            if (fileHandler.saveToPath(path, data)) {
                _lastAutosaveTime.value = Clock.System.now().toEpochMilliseconds()
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun markAsSaved() { _hasUnsavedChanges.value = false }
    
    fun setProject(newProject: AnimationProject) {
        saveUndoState()
        _project.value = newProject
        _currentFrameIndex.value = 0
        _currentLayerIndex.value = 0
        _hasUnsavedChanges.value = false
        lastSessionTimestamp = Clock.System.now().toEpochMilliseconds()
    }

    // --- Инструменты и Свойства ---
    fun setCurrentFrame(index: Int) { if (index in 0 until _project.value.maxFrames) _currentFrameIndex.value = index }
    fun setCurrentLayer(index: Int) { if (index in _project.value.layers.indices) _currentLayerIndex.value = index }
    fun setTool(tool: ToolType) { _currentTool.value = tool }
    fun setCurrentColor(color: ULong) { _currentColor.value = color }
    fun setBrushSize(size: Float) { _brushSize.value = size.coerceIn(1f, 100f) }
    fun setOpacity(opacity: Float) { _opacity.value = opacity.coerceIn(0f, 1f) }
    fun setZoom(zoom: Float) { _zoom.value = zoom.coerceIn(0.1f, 10f) }
    fun setPanOffset(offset: Offset) { _panOffset.value = offset }
    fun setOnionSkinFramesBefore(count: Int) { _onionSkinFramesBefore.value = count }
    fun setOnionSkinFramesAfter(count: Int) { _onionSkinFramesAfter.value = count }

    // --- Рисование ---
    fun startStroke(point: Offset) {
        val layer = _project.value.layers.getOrNull(_currentLayerIndex.value) ?: return
        if (layer.isLocked || !layer.isVisible) return
        _project.value.ensureFrameCount(_currentFrameIndex.value + 1)
        
        val stroke = Stroke(
            points = mutableListOf(point),
            color = _currentColor.value,
            strokeWidth = _brushSize.value,
            isEraser = _currentTool.value == ToolType.ERASER,
            toolType = _currentTool.value,
            opacity = _opacity.value
        )
        currentStroke = stroke
        saveUndoState()
        _project.value.layers[_currentLayerIndex.value].frames[_currentFrameIndex.value].strokes.add(stroke)
        _project.value.lastModifiedTimestamp = Clock.System.now().toEpochMilliseconds()
        _project.value = _project.value.copy()
        _hasUnsavedChanges.value = true
    }

    fun continueStroke(point: Offset) {
        currentStroke?.points?.add(point)
        _project.value = _project.value.copy()
        _hasUnsavedChanges.value = true
    }

    fun endStroke() { currentStroke = null }

    // --- Управление Кадрами и Слоями ---
    fun addLayer() { 
        saveUndoState(); _project.value.addLayer(); _project.value = _project.value.copy()
        _currentLayerIndex.value = _project.value.layers.size - 1; _hasUnsavedChanges.value = true 
    }
    fun removeLayer(index: Int) {
        if (index in _project.value.layers.indices) {
            saveUndoState(); _project.value.removeLayer(index); _project.value = _project.value.copy()
            _currentLayerIndex.value = (_project.value.layers.size - 1).coerceAtLeast(0)
            _hasUnsavedChanges.value = true
        }
    }
    fun moveLayerUp(index: Int) { if (index < _project.value.layers.size - 1) { saveUndoState(); _project.value.moveLayer(index, index + 1); _project.value = _project.value.copy(); _currentLayerIndex.value = index + 1; _hasUnsavedChanges.value = true } }
    fun moveLayerDown(index: Int) { if (index > 0) { saveUndoState(); _project.value.moveLayer(index, index - 1); _project.value = _project.value.copy(); _currentLayerIndex.value = index - 1; _hasUnsavedChanges.value = true } }
    fun setLayerVisible(index: Int, visible: Boolean) { if (index in _project.value.layers.indices) { _project.value.layers[index].isVisible = visible; _project.value = _project.value.copy(); _hasUnsavedChanges.value = true } }
    fun setLayerLocked(index: Int, locked: Boolean) { if (index in _project.value.layers.indices) { _project.value.layers[index].isLocked = locked; _project.value = _project.value.copy(); _hasUnsavedChanges.value = true } }

    fun addFrame() { saveUndoState(); _project.value.addFrame(_currentLayerIndex.value); _project.value = _project.value.copy(); _currentFrameIndex.value = _project.value.maxFrames - 1; _hasUnsavedChanges.value = true }
    fun duplicateFrame() { saveUndoState(); val next = _project.value.duplicateFrame(_currentLayerIndex.value, _currentFrameIndex.value); _project.value = _project.value.copy(); _currentFrameIndex.value = next; _hasUnsavedChanges.value = true }
    fun removeFrame() { saveUndoState(); _project.value.removeFrame(_currentLayerIndex.value, _currentFrameIndex.value); _project.value = _project.value.copy(); _currentFrameIndex.value = (_project.value.maxFrames - 1).coerceAtLeast(0); _hasUnsavedChanges.value = true }
    fun clearFrame() { saveUndoState(); _project.value.layers.getOrNull(_currentLayerIndex.value)?.frames?.getOrNull(_currentFrameIndex.value)?.clear(); _project.value = _project.value.copy(); _hasUnsavedChanges.value = true }

    // --- Плеер ---
    fun togglePlayback() { if (_isPlaying.value) stopPlayback() else startPlayback() }
    private fun startPlayback() {
        _isPlaying.value = true
        playbackJob = scope.launch {
            while (isActive && _isPlaying.value) {
                delay((1000 / _project.value.fps).toLong())
                var next = _currentFrameIndex.value + 1
                if (next >= _project.value.maxFrames) next = 0
                _currentFrameIndex.value = next
            }
        }
    }
    private fun stopPlayback() { _isPlaying.value = false; playbackJob?.cancel() }
    fun goToFirstFrame() { _currentFrameIndex.value = 0 }
    fun goToLastFrame() { _currentFrameIndex.value = _project.value.maxFrames - 1 }
    fun goToPreviousFrame() { if (_currentFrameIndex.value > 0) _currentFrameIndex.value-- }
    fun goToNextFrame() { if (_currentFrameIndex.value < _project.value.maxFrames - 1) _currentFrameIndex.value++ }

    // --- История ---
    private fun saveUndoState() {
        undoStack.add(_project.value.copy())
        if (undoStack.size > 50) undoStack.removeFirst()
        redoStack.clear()
        _canUndo.value = true
        _canRedo.value = false
    }
    fun undo() { if (undoStack.isNotEmpty()) { redoStack.add(_project.value.copy()); _project.value = undoStack.removeLast(); _canUndo.value = undoStack.isNotEmpty(); _canRedo.value = true; _hasUnsavedChanges.value = true } }
    fun redo() { if (redoStack.isNotEmpty()) { undoStack.add(_project.value.copy()); _project.value = redoStack.removeLast(); _canUndo.value = true; _canRedo.value = redoStack.isNotEmpty(); _hasUnsavedChanges.value = true } }

    // --- Рендеринг ---
    data class OnionSkinFrame(val strokes: List<Stroke>, val opacity: Float, val isCurrent: Boolean)
    fun getFramesForRendering(): List<OnionSkinFrame> {
        val current = _currentFrameIndex.value
        val result = mutableListOf<OnionSkinFrame>()
        if (!_onionSkinEnabled.value) {
            val strokes = _project.value.layers.filter { it.isVisible }.flatMap { it.frames.getOrNull(current)?.strokes ?: emptyList() }
            return listOf(OnionSkinFrame(strokes, 1f, true))
        }
        // Onion Skin Before
        for (i in _onionSkinFramesBefore.value downTo 1) {
            val idx = current - i
            if (idx >= 0) result.add(OnionSkinFrame(_project.value.layers.filter { it.isVisible }.flatMap { it.frames.getOrNull(idx)?.strokes ?: emptyList() }, 0.2f / i, false))
        }
        // Current
        result.add(OnionSkinFrame(_project.value.layers.filter { it.isVisible }.flatMap { it.frames.getOrNull(current)?.strokes ?: emptyList() }, 1f, true))
        // Onion Skin After
        for (i in 1.._onionSkinFramesAfter.value) {
            val idx = current + i
            if (idx < _project.value.maxFrames) result.add(OnionSkinFrame(_project.value.layers.filter { it.isVisible }.flatMap { it.frames.getOrNull(idx)?.strokes ?: emptyList() }, 0.2f / i, false))
        }
        return result
    }

    fun cleanup() {
        workTimeJob?.cancel(); playbackJob?.cancel(); autoSaveJob?.cancel(); scope.cancel()
    }
}
