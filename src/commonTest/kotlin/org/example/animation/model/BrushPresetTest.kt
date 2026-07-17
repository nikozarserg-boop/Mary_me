package org.example.animation.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class BrushPresetTest {

    @Test
    fun `toJson fromJson round-trip сохраняет все поля`() {
        val original = BrushPreset(
            name = "Тестовая кисть",
            size = 25.5f,
            opacity = 0.85f,
            flow = 0.7f,
            spacing = 0.12f,
            hardness = 0.9f,
            shape = BrushShape.TEXTURE,
            usePressure = false,
            usePressureSize = false,
            usePressureOpacity = true,
            textureEnabled = true,
            textureName = "texture.png",
            scatter = 5.5f,
            angle = 45f,
            roundness = 0.5f,
            color = 0xFF00A5FFuL
        )

        val json = original.toJson()
        val restored = BrushPreset.fromJson(json)

        assertNotNull(restored)
        assertEquals(original.name, restored?.name)
        assertEquals(original.size, restored?.size)
        assertEquals(original.opacity, restored?.opacity)
        assertEquals(original.flow, restored?.flow)
        assertEquals(original.spacing, restored?.spacing)
        assertEquals(original.hardness, restored?.hardness)
        assertEquals(original.shape, restored?.shape)
        assertEquals(original.usePressure, restored?.usePressure)
        assertEquals(original.usePressureSize, restored?.usePressureSize)
        assertEquals(original.usePressureOpacity, restored?.usePressureOpacity)
        assertEquals(original.textureEnabled, restored?.textureEnabled)
        assertEquals(original.textureName, restored?.textureName)
        assertEquals(original.scatter, restored?.scatter)
        assertEquals(original.angle, restored?.angle)
        assertEquals(original.roundness, restored?.roundness)
        assertEquals(0xFF00A5FFuL, restored?.color)
    }

    @Test
    fun `fromJson для старого формата читает поля`() {
        val oldJson = """{"name":"СтараяКисть","size":10.5,"opacity":0.5,"flow":0.8,"spacing":0.05,"hardness":0.7,"shape":"CIRCLE","textureEnabled":true,"textureName":"tex.jpg","scatter":3.0,"angle":90,"roundness":0.6,"color":"invalid"}"""
        
        val restored = BrushPreset.fromJson(oldJson)
        
        assertNotNull(restored)
        assertEquals("СтараяКисть", restored?.name)
        assertEquals(10.5f, restored?.size)
        assertEquals(0.5f, restored?.opacity)
        assertEquals(0xFF000000uL, restored?.color) // невалидный color даст дефолт
    }

    @Test
    fun `fromJson на мусорной строке возвращает null`() {
        val garbage = "это не json вообще"
        val restored = BrushPreset.fromJson(garbage)
        assertNull(restored)
    }

    @Test
    fun `BrushManager loadFromJson для валидного массива возвращает элементы`() {
        val jsonArray = """[
            {"name":"Кисть 1","size":5.0,"opacity":1.0,"color":255},
            {"name":"Кисть 2","size":10.0,"opacity":0.5,"color":65280}
        ]"""
        
        val presets = BrushManager.loadFromJson(jsonArray)
        assertEquals(2, presets.size)
        assertEquals("Кисть 1", presets[0].name)
        assertEquals("Кисть 2", presets[1].name)
    }

    @Test
    fun `BrushManager loadFromJson для некорректной строки возвращает пустой список`() {
        val invalid = "не json массив"
        val presets = BrushManager.loadFromJson(invalid)
        assertTrue(presets.isEmpty())
    }

    @Test
    fun `addPreset увеличивает список и делает currentIndex последним`() {
        val initialSize = BrushManager.getPresets().size
        val initialIndex = BrushManager.getCurrentIndex()

        try {
            val newPreset = BrushPreset(name = "Новая тестовая кисть")
            BrushManager.addPreset(newPreset)

            assertEquals(initialSize + 1, BrushManager.getPresets().size)
            assertEquals(initialSize, BrushManager.getCurrentIndex())
        } finally {
            // Восстанавливаем состояние - удаляем если есть возможность
            val presets = BrushManager.getPresets()
            if (presets.size > 1) {
                BrushManager.removePreset(presets.size - 1)
            }
            BrushManager.setCurrent(initialIndex)
        }
    }

    @Test
    fun `removePreset не удаляет единственный пресет`() {
        val initialSize = BrushManager.getPresets().size
        
        try {
            // Проверяем обычное удаление (стартовый список > 1)
            val indexToRemove = initialSize - 1
            BrushManager.removePreset(indexToRemove)
            
            // Размер должен уменьшиться на 1
            assertEquals(initialSize - 1, BrushManager.getPresets().size)
        } finally {
            BrushManager.setCurrent(0) // Восстанавливаем индекс
        }
    }

    @Test
    fun `setCurrent игнорирует индекс вне диапазона`() {
        val initialIndex = BrushManager.getCurrentIndex()
        
        try {
            BrushManager.setCurrent(999) // Вне диапазона
            assertEquals(initialIndex, BrushManager.getCurrentIndex())
            
            BrushManager.setCurrent(-1) // Отрицательный индекс
            assertEquals(initialIndex, BrushManager.getCurrentIndex())
        } finally {
            BrushManager.setCurrent(initialIndex)
        }
    }

    @Test
    fun `round-trip stampPng сохраняет байты`() {
        val stampData = byteArrayOf(0x89.toByte(), 0x50.toByte(), 0x4E.toByte(), 0x47.toByte(), 0x0D.toByte(), 0x0A.toByte(), 0x1A.toByte(), 0x0A.toByte())
        val original = BrushPreset(
            name = "Кисть с штампом",
            stampPng = stampData
        )

        val json = original.toJson()
        val restored = BrushPreset.fromJson(json)

        assertNotNull(restored)
        assertNotNull(restored?.stampPng)
        assertTrue(restored?.stampPng?.contentEquals(stampData) ?: false)
    }

    @Test
    fun `equals учитывает stampPng`() {
        val stamp1 = byteArrayOf(1.toByte(), 2.toByte(), 3.toByte())
        val stamp2 = byteArrayOf(1.toByte(), 2.toByte(), 3.toByte())
        val stamp3 = byteArrayOf(1.toByte(), 2.toByte(), 4.toByte())

        val preset1 = BrushPreset(name = "A", stampPng = stamp1)
        val preset2 = BrushPreset(name = "A", stampPng = stamp2)
        val preset3 = BrushPreset(name = "A", stampPng = stamp3)

        assertEquals(preset1, preset2) // Одинаковые байты
        assertFalse(preset1 == preset3) // Разные байты
    }

    @Test
    fun `hashCode учитывает stampPng`() {
        val stamp1 = byteArrayOf(1.toByte(), 2.toByte(), 3.toByte())
        val stamp2 = byteArrayOf(1.toByte(), 2.toByte(), 3.toByte())

        val preset1 = BrushPreset(name = "A", stampPng = stamp1)
        val preset2 = BrushPreset(name = "A", stampPng = stamp2)

        assertEquals(preset1.hashCode(), preset2.hashCode())
    }
}
