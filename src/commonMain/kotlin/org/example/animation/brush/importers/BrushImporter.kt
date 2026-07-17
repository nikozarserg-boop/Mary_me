package org.example.animation.brush.importers

import org.example.animation.model.BrushPreset

interface BrushImporter {
    val extensions: List<String>
    fun parse(bytes: ByteArray, fileName: String): List<BrushPreset>
}

object BrushImporterRegistry {
    private val importers = mutableListOf<BrushImporter>()

    fun register(importer: BrushImporter) {
        importers.add(importer)
    }

    fun getImporterForExtension(extension: String): BrushImporter? {
        val ext = extension.lowercase().removePrefix(".")
        return importers.find { it.extensions.contains(ext) }
    }

    fun import(bytes: ByteArray, fileName: String): List<BrushPreset> {
        val ext = fileName.substringAfterLast(".", "").lowercase()
        if (ext == "marybrush") {
            return org.example.animation.model.BrushManager.loadFromJson(bytes.decodeToString())
        }
        val importer = getImporterForExtension(ext)
        return try {
            importer?.parse(bytes, fileName) ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}
