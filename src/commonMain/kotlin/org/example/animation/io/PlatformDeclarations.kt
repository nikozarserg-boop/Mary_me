package org.example.animation.io

import androidx.compose.ui.graphics.ImageBitmap

/**
 * Платформенно-нейтральный интерфейс файлового хендлера.
 * Реализация выбирается через createPlatformFileHandler() (expect/actual).
 */
// Файловый хендлер
interface PlatformFileHandler {
    fun saveFile(defaultName: String, extension: String, data: ByteArray): Boolean
    fun openFile(extension: String): ByteArray?
    fun saveToPath(path: String, data: ByteArray): Boolean
    fun readFromPath(path: String): ByteArray?
    fun getDocumentsDirectory(): String
    fun getCacheDirectory(): String
    fun listFiles(path: String): List<FileEntry>
    fun getParentPath(path: String): String?
    fun getHomeDirectory(): String
    fun openInExplorer(path: String)
    fun fileExists(path: String): Boolean
    fun isDirectory(path: String): Boolean
    fun deleteFile(path: String): Boolean
    suspend fun exportAnimation(
        project: org.example.animation.model.AnimationProject,
        outputPath: String,
        format: String,
        width: Int,
        height: Int,
        fps: Int,
        density: androidx.compose.ui.unit.Density,
        onProgress: (Float) -> Unit
    ): String?
    suspend fun extractVideoFrames(path: String, fps: Int, maxFrames: Int): List<ByteArray>
    fun shareFile(path: String)
    fun copyFileToClipboard(path: String)
}

data class FileEntry(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val extension: String
)

// Утилиты для работы с изображениями
expect fun decodeImage(data: ByteArray): ImageBitmap?


expect fun encodeImage(bitmap: ImageBitmap, format: String): ByteArray
expect fun encodeRawToPng(pixels: ByteArray, width: Int, height: Int, bytesPerPixel: Int): ByteArray
expect fun unzip(bytes: ByteArray): Map<String, ByteArray>

// Графические утилиты
expect fun floodFillOnBitmap(
    project: org.example.animation.model.AnimationProject,
    layerIndex: Int,
    frameIndex: Int,
    point: androidx.compose.ui.geometry.Offset,
    fillColor: ULong
)

expect fun pickColorFromBitmap(
    project: org.example.animation.model.AnimationProject,
    point: androidx.compose.ui.geometry.Offset,
    layerIndex: Int,
    frameIndex: Int
): ULong?