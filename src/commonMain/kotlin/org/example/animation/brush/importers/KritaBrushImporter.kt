package org.example.animation.brush.importers

import org.example.animation.model.BrushPreset
import org.example.animation.model.BrushShape

class KritaBrushImporter : BrushImporter {
    override val extensions: List<String> = listOf("kpp")

    override fun parse(bytes: ByteArray, fileName: String): List<BrushPreset> {
        // .kpp is basically a PNG file with some metadata.
        // We can use the whole PNG as the stamp.
        return listOf(
            BrushPreset(
                name = fileName.substringBeforeLast("."),
                shape = BrushShape.TEXTURE,
                stampPng = bytes, // The file itself is the PNG preview/stamp
                size = 50f,
                spacing = 0.1f
            )
        )
    }
}
