package org.example.animation.engine

import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.example.animation.model.*

/**
 * Движок анимации - управляет состоянием проекта, воспроизведением, отменой/повтором
 */
class AnimationEngine {
    // Состояние проекта
    private val _project = MutableStateFlow(AnimationProject())
    val project: StateFlow<AnimationProject> = _project.asStateFlow()

    // Текущий кадр
    private val _currentFrameIndex = MutableStateFlow(0)
    val currentFrameIndex: StateFlow<Int> = _currentFrameIndex.asStateFlow()

    // Текущий слой
    private val _currentLayerIndex = MutableStateFlow(0)
    val currentLayerIndex: StateFlow<Int> = _currentLayerIndex.asStateFlow()

    // Воспроизведение
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    // Текущий инструмент
    private val _currentTool = MutableStateFlow(ToolType.PEN)
    val currentTool: StateFlow<ToolType> = _currentTool.asStateFlow()

    // Цвет
    private val _currentColor = MutableStateFlow(0xFF000000uL)
    val currentColor: StateFlow<ULong> = _currentColor.asStateFlow()

    // Размер кисти
    private val _brushSize = MutableStateFlow(4f)
    val brushSize: StateFlow<Float> = _brushSize.asStateFlow()

    // Прозрачность
    private val _opacity = MutableStateFlow(1f)
    val opacity: StateFlow<Float> = _opacity.asStateFlow()

    // Onion skinning (просмотр предыдущих/следующих кадров)
    private val _onionSkinEnabled = MutableStateFlow(true)
    val onionSkinEnabled: StateFlow<Boolean> = _onionSkinEnabled.asStateFlow()

    private val _onionSkinFramesBefore = MutableStateFlow(2)
    val onionSkinFramesBefore: StateFlow<Int> = _onionSkinFramesBefore.asStateFlow()

    private val _onionSkinFramesAfter = MutableStateFlow(1)
    val onionSkinFramesAfter: StateFlow<Int> = _onionSkinFramesAfter.asStateFlow()

    // Зум и панорамирование
    private val _zoom = MutableStateFlow(1f)
    val zoom: StateFlow<Float> = _zoom.asStateFlow()

    private val _panOffset = MutableStateFlow(Offset.Zero)
    val panOffset: StateFlow<Offset> = _panOffset.asStateFlow()

    // История отмены/повтора
    private val undoStack = mutableListOf<AnimationProject>()
    private val redoStack = mutableListOf<AnimationProject>()
    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()
    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

    // Текущий штрих (для рисования)
    private var currentStroke: Stroke? = null

    // Job для воспроизведения
    private var playbackJob: Job? = null

    // Скоуп для корутин
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    fun setProject(newProject: AnimationProject) {
        saveUndoState()
        _project.value = newProject
        _currentFrameIndex.value = 0
        _currentLayerIndex.value = 0
    }

    fun setCurrentFrame(index: Int) {
        val maxFrames = _project.value.maxFrames
        if (index in 0 until maxFrames) {
            _currentFrameIndex.value = index
        }
    }

    fun setCurrentLayer(index: Int) {
        if (index in _project.value.layers.indices) {
            _currentLayerIndex.value = index
        }
    }

    fun setCurrentTool(tool: ToolType) {
        _currentTool.value = tool
    }

    fun setCurrentColor(color: ULong) {
        _currentColor.value = color
    }

    fun setBrushSize(size: Float) {
        _brushSize.value = size.coerceIn(1f, 100f)
    }

    fun setOpacity(opacity: Float) {
        _opacity.value = opacity.coerceIn(0f, 1f)
    }

    fun setOnionSkinEnabled(enabled: Boolean) {
        _onionSkinEnabled.value = enabled
    }

    fun setOnionSkinFramesBefore(count: Int) {
        _onionSkinFramesBefore.value = count
    }

    fun setOnionSkinFramesAfter(count: Int) {
        _onionSkinFramesAfter.value = count
    }

    fun setZoom(zoom: Float) {
        _zoom.value = zoom.coerceIn(0.1f, 10f)
    }

    fun setPanOffset(offset: Offset) {
        _panOffset.value = offset
    }

    // --- Рисование ---

    fun startStroke(point: Offset) {
        val project = _project.value
        val layerIndex = _currentLayerIndex.value
        val frameIndex = _currentFrameIndex.value

        if (layerIndex !in project.layers.indices) return
        val layer = project.layers[layerIndex]
        if (layer.isLocked || !layer.isVisible) return

        project.ensureFrameCount(frameIndex + 1)

        val tool = _currentTool.value
        val stroke = Stroke(
            points = mutableListOf(point),
            color = _currentColor.value,
            strokeWidth = _brushSize.value,
            isEraser = tool == ToolType.ERASER,
            toolType = tool,
            opacity = _opacity.value
        )
        currentStroke = stroke

        saveUndoState()
        val frame = project.layers[layerIndex].frames[frameIndex]
        frame.strokes.add(stroke)
        _project.value = project.copy()
    }

    fun continueStroke(point: Offset) {
        val stroke = currentStroke ?: return
        stroke.points.add(point)
        // Обновляем проект для рекомпозиции
        _project.value = _project.value.copy()
    }

    fun endStroke() {
        currentStroke = null
    }

    // --- Слои ---

    fun addLayer() {
        saveUndoState()
        val project = _project.value
        project.addLayer()
        _project.value = project.copy()
        _currentLayerIndex.value = project.layers.size - 1
    }

    fun removeLayer(index: Int) {
        saveUndoState()
        val project = _project.value
        project.removeLayer(index)
        _project.value = project.copy()
        if (_currentLayerIndex.value >= project.layers.size) {
            _currentLayerIndex.value = project.layers.size - 1
        }
    }

    fun moveLayer(from: Int, to: Int) {
        saveUndoState()
        val project = _project.value
        project.moveLayer(from, to)
        _project.value = project.copy()
        _currentLayerIndex.value = to
    }

    fun setLayerVisibility(index: Int, visible: Boolean) {
        val project = _project.value
        if (index in project.layers.indices) {
            project.layers[index].isVisible = visible
            _project.value = project.copy()
        }
    }

    fun setLayerOpacity(index: Int, opacity: Float) {
        val project = _project.value
        if (index in project.layers.indices) {
            project.layers[index].opacity = opacity.coerceIn(0f, 1f)
            _project.value = project.copy()
        }
    }

    fun setLayerLocked(index: Int, locked: Boolean) {
        val project = _project.value
        if (index in project.layers.indices) {
            project.layers[index].isLocked = locked
            _project.value = project.copy()
        }
    }

    fun setLayerName(index: Int, name: String) {
        val project = _project.value
        if (index in project.layers.indices) {
            project.layers[index].name = name
            _project.value = project.copy()
        }
    }

    // --- Кадры ---

    fun addFrame() {
        saveUndoState()
        val project = _project.value
        project.addFrame(_currentLayerIndex.value)
        _project.value = project.copy()
        _currentFrameIndex.value = project.maxFrames - 1
    }

    fun duplicateFrame() {
        saveUndoState()
        val project = _project.value
        val newIndex = project.duplicateFrame(_currentLayerIndex.value, _currentFrameIndex.value)
        _project.value = project.copy()
        _currentFrameIndex.value = newIndex
    }

    fun removeFrame() {
        saveUndoState()
        val project = _project.value
        project.removeFrame(_currentLayerIndex.value, _currentFrameIndex.value)
        _project.value = project.copy()
        if (_currentFrameIndex.value >= project.maxFrames) {
            _currentFrameIndex.value = project.maxFrames - 1
        }
    }

    fun clearFrame() {
        saveUndoState()
        val project = _project.value
        val layerIndex = _currentLayerIndex.value
        val frameIndex = _currentFrameIndex.value
        if (layerIndex in project.layers.indices && frameIndex in project.layers[layerIndex].frames.indices) {
            project.layers[layerIndex].frames[frameIndex].clear()
            _project.value = project.copy()
        }
    }

    // --- Воспроизведение ---

    fun togglePlayback() {
        if (_isPlaying.value) {
            stopPlayback()
        } else {
            startPlayback()
        }
    }

    private fun startPlayback() {
        _isPlaying.value = true
        playbackJob = scope.launch {
            while (isActive && _isPlaying.value) {
                val project = _project.value
                val maxFrames = project.maxFrames
                if (maxFrames <= 1) {
                    _isPlaying.value = false
                    break
                }

                val frameDuration = 1000 / project.fps
                delay(frameDuration.toLong())

                var nextFrame = _currentFrameIndex.value + 1
                if (nextFrame >= maxFrames) {
                    nextFrame = 0
                }
                _currentFrameIndex.value = nextFrame
            }
        }
    }

    private fun stopPlayback() {
        _isPlaying.value = false
        playbackJob?.cancel()
        playbackJob = null
    }

    fun goToPreviousFrame() {
        val current = _currentFrameIndex.value
        if (current > 0) {
            _currentFrameIndex.value = current - 1
        }
    }

    fun goToNextFrame() {
        val current = _currentFrameIndex.value
        val maxFrames = _project.value.maxFrames
        if (current < maxFrames - 1) {
            _currentFrameIndex.value = current + 1
        }
    }

    fun goToFirstFrame() {
        _currentFrameIndex.value = 0
    }

    fun goToLastFrame() {
        _currentFrameIndex.value = _project.value.maxFrames - 1
    }

    // --- Отмена/Повтор ---

    private fun saveUndoState() {
        undoStack.add(_project.value.copy())
        if (undoStack.size > 50) {
            undoStack.removeFirst()
        }
        redoStack.clear()
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = false
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            redoStack.add(_project.value.copy())
            _project.value = undoStack.removeLast()
            _canUndo.value = undoStack.isNotEmpty()
            _canRedo.value = true
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            undoStack.add(_project.value.copy())
            _project.value = redoStack.removeLast()
            _canUndo.value = true
            _canRedo.value = redoStack.isNotEmpty()
        }
    }

    // --- Получение данных для отрисовки с onion skinning ---

    data class OnionSkinFrame(
        val strokes: List<Stroke>,
        val opacity: Float,
        val isCurrent: Boolean
    )

    fun getFramesForRendering(): List<OnionSkinFrame> {
        val project = _project.value
        val currentFrame = _currentFrameIndex.value
        val result = mutableListOf<OnionSkinFrame>()

        if (!_onionSkinEnabled.value) {
            // Только текущий кадр
            val strokes = mutableListOf<Stroke>()
            for (layer in project.layers) {
                if (layer.isVisible && !layer.isLocked) {
                    if (currentFrame < layer.frames.size) {
                        strokes.addAll(layer.frames[currentFrame].strokes)
                    }
                }
            }
            result.add(OnionSkinFrame(strokes, 1f, true))
            return result
        }

        // Кадры до текущего
        val beforeCount = _onionSkinFramesBefore.value
        for (i in 1..beforeCount) {
            val frameIdx = currentFrame - i
            if (frameIdx < 0) continue
            val strokes = mutableListOf<Stroke>()
            for (layer in project.layers) {
                if (layer.isVisible) {
                    if (frameIdx < layer.frames.size) {
                        strokes.addAll(layer.frames[frameIdx].strokes)
                    }
                }
            }
            if (strokes.isNotEmpty()) {
                val opacity = 0.15f / i
                result.add(OnionSkinFrame(strokes, opacity, false))
            }
        }

        // Текущий кадр
        val currentStrokes = mutableListOf<Stroke>()
        for (layer in project.layers) {
            if (layer.isVisible) {
                if (currentFrame < layer.frames.size) {
                    currentStrokes.addAll(layer.frames[currentFrame].strokes)
                }
            }
        }
        result.add(OnionSkinFrame(currentStrokes, 1f, true))

        // Кадры после текущего
        val afterCount = _onionSkinFramesAfter.value
        for (i in 1..afterCount) {
            val frameIdx = currentFrame + i
            if (frameIdx >= project.maxFrames) continue
            val strokes = mutableListOf<Stroke>()
            for (layer in project.layers) {
                if (layer.isVisible) {
                    if (frameIdx < layer.frames.size) {
                        strokes.addAll(layer.frames[frameIdx].strokes)
                    }
                }
            }
            if (strokes.isNotEmpty()) {
                val opacity = 0.15f / i
                result.add(OnionSkinFrame(strokes, opacity, false))
            }
        }

        return result
    }

    fun cleanup() {
        playbackJob?.cancel()
        scope.cancel()
    }
}