package org.example.animation.model

import androidx.compose.ui.geometry.Offset
import kotlinx.datetime.Clock

/**
 * Представляет изображение, загруженное на холст
 */
data class ImageElement(
    val data: ByteArray,
    var x: Float = 0f,
    var y: Float = 0f,
    var scale: Float = 1f,
    var rotation: Float = 0f,
    val id: Long = Clock.System.now().toEpochMilliseconds()
) {
    fun copy(): ImageElement = ImageElement(
        data = data.copyOf(),
        x = x,
        y = y,
        scale = scale,
        rotation = rotation,
        id = id
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ImageElement) return false
        if (!data.contentEquals(other.data)) return false
        if (x != other.x) return false
        if (y != other.y) return false
        if (scale != other.scale) return false
        if (rotation != other.rotation) return false
        return id == other.id
    }

    override fun hashCode(): Int {
        var result = data.contentHashCode()
        result = 31 * result + x.hashCode()
        result = 31 * result + y.hashCode()
        result = 31 * result + scale.hashCode()
        result = 31 * result + rotation.hashCode()
        result = 31 * result + id.hashCode()
        return result
    }
}

/**
 * Представляет один штрих/рисунок на холсте
 */
data class Stroke(
    val points: MutableList<Offset> = mutableListOf(),
    val color: ULong = 0xFF000000uL, // ARGB
    val strokeWidth: Float = 4f,
    val isEraser: Boolean = false,
    val toolType: ToolType = ToolType.PEN,
    val opacity: Float = 1f
) {
    fun copy(): Stroke = Stroke(
        points = points.toMutableList(),
        color = color,
        strokeWidth = strokeWidth,
        isEraser = isEraser,
        toolType = toolType,
        opacity = opacity
    )
}

/**
 * Данные одного кадра анимации
 */
class FrameData(
    val strokes: MutableList<Stroke> = mutableListOf(),
    val images: MutableList<ImageElement> = mutableListOf(),
    var durationMs: Int = 83, // ~12 FPS
    val name: String = ""
) {
    fun copy(): FrameData {
        val newFrame = FrameData(
            strokes = strokes.map { it.copy() }.toMutableList(),
            images = images.map { it.copy() }.toMutableList(),
            durationMs = durationMs,
            name = name
        )
        return newFrame
    }

    fun clear() {
        strokes.clear()
        images.clear()
    }

    fun isEmpty(): Boolean = strokes.isEmpty() && images.isEmpty()
}

/**
 * Уровень (слой) анимации
 */
class LayerData(
    var name: String = "Слой",
    val frames: MutableList<FrameData> = mutableListOf(FrameData()),
    var isVisible: Boolean = true,
    var opacity: Float = 1f,
    var isLocked: Boolean = false
) {
    fun copy(): LayerData {
        val newLayer = LayerData(
            name = name,
            frames = frames.map { it.copy() }.toMutableList(),
            isVisible = isVisible,
            opacity = opacity,
            isLocked = isLocked
        )
        return newLayer
    }

    val frameCount: Int get() = frames.size

    fun getFrame(index: Int): FrameData =
        frames.getOrElse(index) { FrameData() }
}

/**
 * Проект анимации целиком
 */
class AnimationProject(
    var name: String = "Новый проект",
    val layers: MutableList<LayerData> = mutableListOf(LayerData("Слой 1")),
    var canvasWidth: Int = 800,
    var canvasHeight: Int = 600,
    var fps: Int = 24,
    var backgroundColor: ULong = 0xFFFFFFFFuL, // белый
    var workingTimeMs: Long = 0,
    var dpi: Int = 72,
    var createdTimestamp: Long = Clock.System.now().toEpochMilliseconds(),
    var lastModifiedTimestamp: Long = Clock.System.now().toEpochMilliseconds()
) {
    fun copy(): AnimationProject {
        val newProject = AnimationProject(
            name = name,
            layers = layers.map { it.copy() }.toMutableList(),
            canvasWidth = canvasWidth,
            canvasHeight = canvasHeight,
            fps = fps,
            backgroundColor = backgroundColor,
            workingTimeMs = workingTimeMs,
            dpi = dpi,
            createdTimestamp = createdTimestamp,
            lastModifiedTimestamp = lastModifiedTimestamp
        )
        return newProject
    }

    val layerCount: Int get() = layers.size

    val totalFrames: Int
        get() = layers.maxOfOrNull { it.frameCount } ?: 1

    fun ensureFrameCount(targetCount: Int) {
        for (layer in layers) {
            while (layer.frames.size < targetCount) {
                layer.frames.add(FrameData())
            }
        }
    }

    fun addLayer(name: String = "Слой ${layers.size + 1}"): LayerData {
        val layer = LayerData(name = name)
        for (i in 0 until totalFrames) {
            layer.frames.add(FrameData())
        }
        layers.add(layer)
        return layer
    }

    fun removeLayer(index: Int) {
        if (layers.size > 1 && index in layers.indices) {
            layers.removeAt(index)
        }
    }

    fun moveLayer(from: Int, to: Int) {
        if (from in layers.indices && to in layers.indices) {
            val item = layers.removeAt(from)
            layers.add(to, item)
        }
    }

    fun addFrame(layerIndex: Int) {
        if (layerIndex in layers.indices) {
            layers[layerIndex].frames.add(FrameData())
        }
    }

    fun removeFrame(layerIndex: Int, frameIndex: Int) {
        if (layerIndex in layers.indices) {
            val layer = layers[layerIndex]
            if (layer.frames.size > 1 && frameIndex in layer.frames.indices) {
                layer.frames.removeAt(frameIndex)
            }
        }
    }

    fun duplicateFrame(layerIndex: Int, frameIndex: Int): Int {
        if (layerIndex in layers.indices) {
            val layer = layers[layerIndex]
            if (frameIndex in layer.frames.indices) {
                val newFrame = layer.frames[frameIndex].copy()
                layer.frames.add(frameIndex + 1, newFrame)
                return frameIndex + 1
            }
        }
        return frameIndex
    }

    val maxFrames: Int
        get() = layers.maxOfOrNull { it.frameCount } ?: 1
}
