package org.example.animation.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

/**
 * Пресет кисти с настройками
 */
@Serializable
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
    val color: ULong = 0xFF000000uL,
    val stampPng: ByteArray? = null,
    val grainPng: ByteArray? = null,
    val id: Long = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
) {
    fun copyWithSize(newSize: Float): BrushPreset = copy(size = newSize)
    fun copyWithOpacity(newOpacity: Float): BrushPreset = copy(opacity = newOpacity)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BrushPreset) return false

        if (name != other.name) return false
        if (size != other.size) return false
        if (opacity != other.opacity) return false
        if (flow != other.flow) return false
        if (spacing != other.spacing) return false
        if (hardness != other.hardness) return false
        if (shape != other.shape) return false
        if (usePressure != other.usePressure) return false
        if (usePressureSize != other.usePressureSize) return false
        if (usePressureOpacity != other.usePressureOpacity) return false
        if (textureEnabled != other.textureEnabled) return false
        if (textureName != other.textureName) return false
        if (scatter != other.scatter) return false
        if (angle != other.angle) return false
        if (roundness != other.roundness) return false
        if (color != other.color) return false
        if (id != other.id) return false
        if (stampPng != null) {
            if (other.stampPng == null) return false
            if (!stampPng.contentEquals(other.stampPng)) return false
        } else if (other.stampPng != null) return false
        if (grainPng != null) {
            if (other.grainPng == null) return false
            if (!grainPng.contentEquals(other.grainPng)) return false
        } else if (other.grainPng != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + size.hashCode()
        result = 31 * result + opacity.hashCode()
        result = 31 * result + flow.hashCode()
        result = 31 * result + spacing.hashCode()
        result = 31 * result + hardness.hashCode()
        result = 31 * result + shape.hashCode()
        result = 31 * result + usePressure.hashCode()
        result = 31 * result + usePressureSize.hashCode()
        result = 31 * result + usePressureOpacity.hashCode()
        result = 31 * result + textureEnabled.hashCode()
        result = 31 * result + textureName.hashCode()
        result = 31 * result + scatter.hashCode()
        result = 31 * result + angle.hashCode()
        result = 31 * result + roundness.hashCode()
        result = 31 * result + color.hashCode()
        result = 31 * result + (stampPng?.contentHashCode() ?: 0)
        result = 31 * result + (grainPng?.contentHashCode() ?: 0)
        result = 31 * result + id.hashCode()
        return result
    }

    /** Сериализация в JSON (новый формат) */
    fun toJson(): String = Json.encodeToString(this)

    companion object {
        fun fromJson(json: String): BrushPreset? {
            return try {
                Json.decodeFromString<BrushPreset>(json)
            } catch (e: Exception) {
                // Пытаемся распарсить старый формат если новый не прошел
                parseOldJson(json)
            }
        }

        private fun parseOldJson(json: String): BrushPreset? {
            val get = { key: String ->
                val regex = """"$key"\s*:\s*("?)([^",}\]]*)\1""".toRegex()
                regex.find(json)?.groupValues?.get(2)
            }
            val name = get("name") ?: return null
            val colorStr = get("color")
            val color = colorStr?.toULongOrNull() ?: 0xFF000000uL
            return BrushPreset(
                name = name,
                size = get("size")?.replace(",", ".")?.toFloatOrNull() ?: 4f,
                opacity = get("opacity")?.replace(",", ".")?.toFloatOrNull() ?: 1f,
                flow = get("flow")?.replace(",", ".")?.toFloatOrNull() ?: 1f,
                spacing = get("spacing")?.replace(",", ".")?.toFloatOrNull() ?: 0.1f,
                hardness = get("hardness")?.replace(",", ".")?.toFloatOrNull() ?: 1f,
                shape = BrushShape.entries.firstOrNull { it.name == get("shape") } ?: BrushShape.CIRCLE,
                textureEnabled = get("textureEnabled")?.toBooleanStrictOrNull() ?: false,
                textureName = get("textureName") ?: "",
                scatter = get("scatter")?.replace(",", ".")?.toFloatOrNull() ?: 0f,
                angle = get("angle")?.replace(",", ".")?.toFloatOrNull() ?: 0f,
                roundness = get("roundness")?.replace(",", ".")?.toFloatOrNull() ?: 1f,
                color = color
            )
        }
    }
}

@Serializable
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

    /** Загрузка кистей из файла */
    fun loadFromJson(json: String): List<BrushPreset> {
        return try {
            Json.decodeFromString<List<BrushPreset>>(json)
        } catch (e: Exception) {
            // Старая логика парсинга массива
            val result = mutableListOf<BrushPreset>()
            val arrayContent = json.trim().removeSurrounding("[", "]")
            val objects = mutableListOf<String>()
            var depth = 0
            var current = StringBuilder()
            var inString = false
            for (c in arrayContent) {
                if (c == '"') inString = !inString
                if (!inString) {
                    when (c) {
                        '{' -> { depth++; current.append(c) }
                        '}' -> { depth--; current.append(c); if (depth == 0) { objects.add(current.toString()); current = StringBuilder() } }
                        else -> if (depth > 0) current.append(c)
                    }
                } else {
                    current.append(c)
                }
            }
            for (obj in objects) {
                BrushPreset.fromJson(obj)?.let { result.add(it) }
            }
            result
        }
    }

    fun exportCurrentToJson(): String = getCurrent().toJson()
}
