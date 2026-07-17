package org.example.animation.brush.importers

import org.example.animation.model.BrushPreset
import org.example.animation.model.BrushShape
import org.example.animation.io.unzip

class ProcreateBrushImporter : BrushImporter {
    override val extensions: List<String> = listOf("brush", "brushset")

    override fun parse(bytes: ByteArray, fileName: String): List<BrushPreset> {
        val results = mutableListOf<BrushPreset>()
        try {
            val files = unzip(bytes)
            
            // Для .brush это одна кисть. Для .brushset это папка файлов .brush
            if (fileName.endsWith(".brushset")) {
                // Упрощённый способ: ищем Shape.png и Grain.png во вложенных папках
                // Это сложновато, так как нужно группировать их по подпапкам
                val brushFolders = files.keys.filter { it.contains("/") }.map { it.substringBeforeLast("/") }.distinct()
                for (folder in brushFolders) {
                    val shape = files["$folder/Shape.png"] ?: files["$folder/shape.png"]
                    val grain = files["$folder/Grain.png"] ?: files["$folder/grain.png"]
                    if (shape != null) {
                        results.add(BrushPreset(
                            name = folder.substringAfterLast("/"),
                            shape = BrushShape.TEXTURE,
                            stampPng = shape,
                            grainPng = grain
                        ))
                    }
                }
            } else {
                val shape = files["Shape.png"] ?: files["shape.png"]
                val grain = files["Grain.png"] ?: files["grain.png"]
                if (shape != null) {
                    results.add(BrushPreset(
                        name = fileName.substringBeforeLast("."),
                        shape = BrushShape.TEXTURE,
                        stampPng = shape,
                        grainPng = grain
                    ))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return results
    }
}
