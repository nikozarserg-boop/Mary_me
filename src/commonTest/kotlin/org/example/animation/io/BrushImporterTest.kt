package org.example.animation.io

import org.example.animation.brush.importers.GbrBrushImporter
import org.example.animation.brush.importers.AbrBrushImporter
import org.example.animation.brush.importers.KritaBrushImporter
import org.example.animation.brush.importers.SutBrushImporter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class BrushImporterTest {

    @Test
    fun `GbrImporter парсит минимальный валидный GBR файл`() {
        // Создаём минимальный GBR заголовок для version 1
        // headerSize = 28, version = 1 (нет "GIMP" сигнатуры), width = 1, height = 1, bpp = 4
        val header = byteArrayOf(
            0x00, 0x00, 0x00, 0x1C, // headerSize = 28
            0x00, 0x00, 0x00, 0x01, // version = 1
            0x00, 0x00, 0x00, 0x01, // width = 1
            0x00, 0x00, 0x00, 0x01, // height = 1
            0x00, 0x00, 0x00, 0x04  // bpp = 4
        )
        // 1x1x4 = 4 байта RGBA данные
        val pixels = ByteArray(4) { 0xFF.toByte() }
        
        // Имя кисти (headerSize - offset = 28 - 20 = 8 байт) - заполняем нулями
        val nameBytes = byteArrayOf(0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00)
        
        val gbrData = header + nameBytes + pixels
        
        val importer = GbrBrushImporter()
        val presets = importer.parse(gbrData, "test.gbr")

        assertEquals(1, presets.size)
        assertNotNull(presets[0].stampPng)
        assertTrue(presets[0].stampPng!!.isNotEmpty())
        assertEquals(1f, presets[0].size)
    }

    @Test
    fun `AbrImporter decodePackBits правильная распаковка`() {
        val importer = AbrBrushImporter()
        
        // PackBits тест: простая последовательность
        // 0x05 + 0xFF + 0xFF + 0xFF = 5 байт
        val input = byteArrayOf(0x05.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte())
        
        val output = importer.decodePackBits(input, 5)
        
        assertEquals(5, output.size)
        assertEquals(0xFF.toByte(), output[0])
    }

    @Test
    fun `KritaImporter парсит KPP как PNG`() {
        // KPP - это в основном PNG с метаданными
        // Создаём минимальный PNG (не валидный, но для теста)
        val pngHeader = byteArrayOf(
            0x89.toByte(), 0x50.toByte(), 0x4E.toByte(), 0x47.toByte(), 0x0D.toByte(), 0x0A.toByte(), 0x1A.toByte(), 0x0A.toByte(), // PNG signature
            0x00, 0x00, 0x00, 0x0D, // IHDR length
            0x49, 0x48, 0x45, 0x52, // IHDR
            0x00, 0x00, 0x00, 0x01, // width = 1
            0x00, 0x00, 0x00, 0x01, // height = 1
            0x08, 0x02, 0x00, 0x00, 0x00, // bit depth, color type, etc
            0x90.toByte(), 0x77.toByte(), 0x53.toByte(), 0xDE.toByte() // CRC
        )
        
        val importer = KritaBrushImporter()
        val presets = importer.parse(pngHeader, "test.kpp")

        assertEquals(1, presets.size)
        assertNotNull(presets[0].stampPng)
    }

    @Test
    fun `SutImporter не бросает на некорректных данных`() {
        val importer = SutBrushImporter()
        
        // Случайные данные без PNG сигнатуры
        val invalidData = byteArrayOf(0x00, 0x01, 0x02, 0x03, 0x04, 0x05)
        
        val presets = importer.parse(invalidData, "test.sut")
        
        // Не должно бросать исключение, может вернуть пустой список
        assertTrue(presets.isEmpty())
    }

    @Test
    fun `импортёры выбираются по расширению`() {
        val gbr = GbrBrushImporter()
        val abr = AbrBrushImporter()
        val krita = KritaBrushImporter()
        val sut = SutBrushImporter()

        // Проверяем что расширения объявлены
        assertEquals(listOf("gbr"), gbr.extensions)
        assertEquals(listOf("abr"), abr.extensions)
        assertEquals(listOf("kpp"), krita.extensions)
        assertEquals(listOf("sut"), sut.extensions)
    }
}