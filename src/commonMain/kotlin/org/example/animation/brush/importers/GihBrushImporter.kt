package org.example.animation.brush.importers

import org.example.animation.model.BrushPreset

class GihBrushImporter(private val gbrImporter: GbrBrushImporter) : BrushImporter {
    override val extensions: List<String> = listOf("gih")

    override fun parse(bytes: ByteArray, fileName: String): List<BrushPreset> {
        val results = mutableListOf<BrushPreset>()
        try {
            // GIH часто содержит текстовый заголовок, за которым следуют блоки GBR,
            // но спецификация сложна. Упрощённая версия: находим подписи GIMP или пытаемся разобрать сегменты.
            // Более надёжный способ — пропустить первые строки (имя и параметры), затем найти заголовки GBR.
            
            var offset = 0
            // Находим первый блок GBR. GBR обычно начинается с headerSize (обычно 20-30 байт в big-endian)
            // Но заголовок GIH обычно: Name\nNumber of brushes...
            // Можем искать магию "GIMP" или пытаться найти, где могут быть заголовки GBR.
            
            // Для простоты ищем общие размеры заголовка GBR (20, 24, 28) в big-endian
            while (offset < bytes.size - 28) {
                val headerSize = ((bytes[offset].toInt() and 0xFF) shl 24) or
                                 ((bytes[offset + 1].toInt() and 0xFF) shl 16) or
                                 ((bytes[offset + 2].toInt() and 0xFF) shl 8) or
                                 (bytes[offset + 3].toInt() and 0xFF)
                
                if (headerSize in 20..1024) {
                    val subBytes = bytes.sliceArray(offset until bytes.size)
                    val brushes = gbrImporter.parse(subBytes, fileName)
                    if (brushes.isNotEmpty()) {
                        results.addAll(brushes)
                        // Наивная реализация: мы не знаем точный размер блока GBR без полного парсинга
                        // Но GbrBrushImporter не возвращает длину прочитанных данных.
                        // Пока предполагаем одну кисть или улучшаем GbrBrushImporter для возврата длины.
                        break
                    }
                }
                offset++
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return results
    }
}
