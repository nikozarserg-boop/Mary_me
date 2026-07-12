package org.example.animation.model

/**
 * Пресет кисти с настройками
 */
data class BrushPreset(
    val name: String = "Стандартная",
    val size: Float = 4f,
    val opacity: Float = 1f,
    val flow: Float = 1f,
    val spacing: Float = 0.1f,
    val hardness: Float = 1f,
    val shape: BrushShape = BrushShape.CIRCLE,
    val usePressure: Boolean = true,
    val usePressureSize: Boolean = true,
    val usePressureOpacity: Boolean = false,
    val textureEnabled: Boolean = false,
    val textureName: String = "",
    val scatter: Float = 0f,
    val angle: Float = 0f,
    val roundness: Float = 1f,
    val color: ULong = 0xFF000000uL
) {
    fun copyWithSize(newSize: Float): BrushPreset = copy(size = newSize)
    fun copyWithOpacity(newOpacity: Float): BrushPreset = copy(opacity = newOpacity)
}

enum class BrushShape(val displayName: String) {
    CIRCLE("Круг"),
    SQUARE("Квадрат"),
    LINE("Линия"),
    TEXTURE("Текстура")
}

/**
 * Менеджер кистей
 */
object BrushManager {
    private val presets = mutableListOf<BrushPreset>()
    private var currentIndex = 0

    init {
        // Стандартные кисти
        presets.addAll(listOf(
            BrushPreset("Стандартная", 4f, 1f, 1f, 0.1f, 1f, BrushShape.CIRCLE),
            BrushPreset("Тонкая", 1f, 1f, 1f, 0.05f, 1f, BrushShape.CIRCLE),
            BrushPreset("Толстая", 12f, 1f, 1f, 0.15f, 0.8f, BrushShape.CIRCLE),
            BrushPreset("Маркер", 8f, 0.8f, 1f, 0.1f, 0.5f, BrushShape.SQUARE),
            BrushPreset("Аэрограф", 20f, 0.3f, 0.5f, 0.2f, 0.3f, BrushShape.CIRCLE),
            BrushPreset("Карандаш", 2f, 1f, 1f, 0.05f, 1f, BrushShape.CIRCLE),
            BrushPreset("Акварель", 15f, 0.5f, 0.7f, 0.15f, 0.4f, BrushShape.CIRCLE),
            BrushPreset("Ластик", 10f, 1f, 1f, 0.1f, 1f, BrushShape.CIRCLE),
            BrushPreset("Заливка", 50f, 1f, 1f, 0.3f, 0.2f, BrushShape.CIRCLE)
        ))
    }

    fun getPresets(): List<BrushPreset> = presets.toList()
    fun getCurrent(): BrushPreset = presets.getOrElse(currentIndex) { presets.first() }
    fun getCurrentIndex(): Int = currentIndex
    fun setCurrent(index: Int) { if (index in presets.indices) currentIndex = index }
    fun addPreset(preset: BrushPreset) { presets.add(preset); currentIndex = presets.size - 1 }
    fun removePreset(index: Int) { if (presets.size > 1 && index in presets.indices) { presets.removeAt(index); if (currentIndex >= presets.size) currentIndex = presets.size - 1 } }
    fun updatePreset(index: Int, preset: BrushPreset) { if (index in presets.indices) presets[index] = preset }
}