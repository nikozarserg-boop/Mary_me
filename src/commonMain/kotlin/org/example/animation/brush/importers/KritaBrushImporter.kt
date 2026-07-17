package org.example.animation.brush.importers

import org.example.animation.model.BrushPreset
import org.example.animation.model.BrushShape

class KritaBrushImporter : BrushImporter {
    override val extensions: List<String> = listOf("kpp")

    override fun parse(bytes: ByteArray, fileName: String): List<BrushPreset> {
        // .kpp — это в основном PNG файл с некоторыми метаданными.
        // Мы можем использовать весь PNG как штамп.
        return listOf(
            BrushPreset(
                name = fileName.substringBeforeLast("."),
                shape = BrushShape.TEXTURE,
                stampPng = bytes, // Файл сам является PNG превью/штампом
                size = 50f,
                spacing = 0.1f
            )
        )
    }
}
