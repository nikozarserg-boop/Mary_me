package org.example.animation.brush.importers

import org.example.animation.model.BrushPreset
import org.example.animation.model.BrushShape
import org.example.animation.io.encodeRawToPng

class GbrBrushImporter : BrushImporter {
    override val extensions: List<String> = listOf("gbr")

    override fun parse(bytes: ByteArray, fileName: String): List<BrushPreset> {
        if (bytes.size < 28) return emptyList()

        var offset = 0
        fun readInt(): Int {
            val res = ((bytes[offset].toInt() and 0xFF) shl 24) or
                      ((bytes[offset + 1].toInt() and 0xFF) shl 16) or
                      ((bytes[offset + 2].toInt() and 0xFF) shl 8) or
                      (bytes[offset + 3].toInt() and 0xFF)
            offset += 4
            return res
        }

        val headerSize = readInt()
        val version = readInt()
        val width = readInt()
        val height = readInt()
        val bpp = readInt()
        
        // Skip magic "GIMP" if version >= 2
        if (version >= 2) offset += 4
        
        val nameSize = headerSize - offset
        val name = if (nameSize > 0) {
            val nameBytes = bytes.sliceArray(offset until (offset + nameSize))
            offset += nameSize
            nameBytes.decodeToString().trimEnd('\u0000')
        } else fileName

        val pixelDataSize = width * height * bpp
        if (offset + pixelDataSize > bytes.size) return emptyList()

        val pixels = bytes.sliceArray(offset until (offset + pixelDataSize))
        
        val pngBytes = try {
            encodeRawToPng(pixels, width, height, bpp)
        } catch (e: Exception) {
            e.printStackTrace()
            return emptyList()
        }

        return listOf(
            BrushPreset(
                name = name,
                shape = BrushShape.TEXTURE,
                stampPng = pngBytes,
                size = maxOf(width, height).toFloat(),
                spacing = 0.1f
            )
        )
    }
}
