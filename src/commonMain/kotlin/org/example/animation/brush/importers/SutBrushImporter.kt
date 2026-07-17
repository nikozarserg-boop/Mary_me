package org.example.animation.brush.importers

import org.example.animation.model.BrushPreset
import org.example.animation.model.BrushShape

class SutBrushImporter : BrushImporter {
    override val extensions: List<String> = listOf("sut")

    override fun parse(bytes: ByteArray, fileName: String): List<BrushPreset> {
        val results = mutableListOf<BrushPreset>()
        try {
            // .sut is an SQLite database. 
            // Parsing it fully in commonMain without an SQLite library is hard.
            // As a best-effort, we search for PNG signatures in the raw bytes.
            
            var i = 0
            val pngSignature = byteArrayOf(0x89.toByte(), 'P'.toByte(), 'N'.toByte(), 'G'.toByte())
            
            while (i < bytes.size - 8) {
                if (bytes[i] == pngSignature[0] && 
                    bytes[i+1] == pngSignature[1] && 
                    bytes[i+2] == pngSignature[2] && 
                    bytes[i+3] == pngSignature[3]) {
                    
                    // Found a PNG! Try to find its end. 
                    // PNG ends with IEND chunk (4 bytes name + 4 bytes CRC)
                    val iendSignature = byteArrayOf('I'.toByte(), 'E'.toByte(), 'N'.toByte(), 'D'.toByte())
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
