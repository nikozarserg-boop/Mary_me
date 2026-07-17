package org.example.animation.brush.importers

import org.example.animation.model.BrushPreset
import org.example.animation.model.BrushShape

class SutBrushImporter : BrushImporter {
    override val extensions: List<String> = listOf("sut")

    override fun parse(bytes: ByteArray, fileName: String): List<BrushPreset> {
        val results = mutableListOf<BrushPreset>()
        try {
        // .sut — это SQLite база данных.
        // Полное парсинг в commonMain без библиотеки SQLite сложно.
        // В качестве best-effort ищем подписи PNG в необработанных байтах.
        
        var i = 0
        val pngSignature = byteArrayOf(0x89.toByte(), 'P'.code.toByte(), 'N'.code.toByte(), 'G'.code.toByte())
            
            while (i < bytes.size - 8) {
                if (bytes[i] == pngSignature[0] && 
                    bytes[i+1] == pngSignature[1] && 
                    bytes[i+2] == pngSignature[2] && 
                    bytes[i+3] == pngSignature[3]) {
                    
                    // Найден PNG! Пытаемся найти его конец.
                    // PNG заканчивается чанком IEND (4 байта имени + 4 байта CRC)
                    val iendSignature = byteArrayOf('I'.code.toByte(), 'E'.code.toByte(), 'N'.code.toByte(), 'D'.code.toByte())
                    var j = i + 4
                    var foundEnd = false
                    while (j < bytes.size - 4) {
                        if (bytes[j] == iendSignature[0] && 
                            bytes[j+1] == iendSignature[1] && 
                            bytes[j+2] == iendSignature[2] && 
                            bytes[j+3] == iendSignature[3]) {
                            
                            val pngEnd = j + 8 // IEND + CRC
                            if (pngEnd <= bytes.size) {
                                val pngData = bytes.sliceArray(i until pngEnd)
                                results.add(BrushPreset(
                                    name = "CSP Stamp ${results.size + 1}",
                                    shape = BrushShape.TEXTURE,
                                    stampPng = pngData
                                ))
                                i = pngEnd - 1
                                foundEnd = true
                            }
                            break
                        }
                        j++
                    }
                    if (!foundEnd) i++
                } else {
                    i++
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return results
    }
}
