package org.example.animation.brush.importers

import org.example.animation.model.BrushPreset

class GihBrushImporter(private val gbrImporter: GbrBrushImporter) : BrushImporter {
    override val extensions: List<String> = listOf("gih")

    override fun parse(bytes: ByteArray, fileName: String): List<BrushPreset> {
        val results = mutableListOf<BrushPreset>()
        try {
            // GIH is often a text header followed by GBR blocks, 
            // but the spec is complex. Simplified version: find GIMP signatures or just try to parse segments.
            // A more robust way is to skip the first lines (name and params) and then find GBR headers.
            
            var offset = 0
            // Find first GBR block. GBR often starts with headerSize (usually 20-30 bytes in big-endian)
            // But GIH header is usually: Name\nNumber of brushes...
            // We can look for the "GIMP" magic or just try to find where GBR headers might be.
            
            // For simplicity, let's look for common GBR header sizes (20, 24, 28) in big-endian
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
                        // This is naive as we don't know the exact size of the GBR block without parsing it fully
                        // But GbrBrushImporter doesn't return the consumed length.
                        // Let's assume one brush for now or improve GbrBrushImporter to return length.
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
