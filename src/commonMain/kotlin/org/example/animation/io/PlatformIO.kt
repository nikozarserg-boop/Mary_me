package org.example.animation.io

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.Density
import org.example.animation.model.AnimationProject

/**
 * Описание записи файла или папки для кроссплатформенного менеджера
 */
data class FileEntry(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val extension: String = ""
)

/**
 * Платформенно-зависимые операции ввода/вывода
 */
expect fun createPlatformFileHandler(): PlatformFileHandler

/**
 * Декодирование изображения из байтов в ImageBitmap
 */
expect fun decodeImage(data: ByteArray): ImageBitmap?

/**
 * Кодирование ImageBitmap в байты (PNG, JPG, WEBP)
 */
expect fun encodeImage(bitmap: ImageBitmap, format: String = "png"): ByteArray

/**
 * Кодирование сырых пикселей в PNG
 */
expect fun encodeRawToPng(pixels: ByteArray, width: Int, height: Int, bytesPerPixel: Int): ByteArray

/**
 * Распаковка ZIP архива
 */
expect fun unzip(bytes: ByteArray): Map<String, ByteArray>

/**
 * Заливка на bitmap (рендеринг текущего кадра + flood fill)
 */
expect fun floodFillOnBitmap(
    project: AnimationProject,
    layerIndex: Int,
    frameIndex: Int,
    point: Offset,
    fillColor: ULong
)

/**
 * Пипетка - получение цвета из bitmap
 */
expect fun pickColorFromBitmap(
    project: AnimationProject,
    point: Offset,
    layerIndex: Int,
    frameIndex: Int
): ULong?

interface PlatformFileHandler {
    fun saveFile(defaultName: String, extension: String, data: ByteArray): Boolean
    fun openFile(extension: String): ByteArray?
    fun saveToPath(path: String, data: ByteArray): Boolean
    fun readFromPath(path: String): ByteArray?
    
    // Работа с директориями
    fun getDocumentsDirectory(): String
    fun getCacheDirectory(): String
    fun listFiles(path: String): List<FileEntry>
    fun getParentPath(path: String): String?
    fun getHomeDirectory(): String
    
    fun openInExplorer(path: String)
    fun fileExists(path: String): Boolean
    fun isDirectory(path: String): Boolean
    fun deleteFile(path: String): Boolean

    /**
     * Экспорт анимации в видео или GIF.
     * Возвращает null при успехе, иначе строку с текстом ошибки.
     */
    suspend fun exportAnimation(
        project: AnimationProject,
        outputPath: String,
        format: String,
        width: Int,
        height: Int,
        fps: Int,
        density: Density,
        onProgress: (Float) -> Unit
    ): String?

    /**
     * Извлечение кадров из видеофайла.
     */
    suspend fun extractVideoFrames(path: String, fps: Int, maxFrames: Int): List<ByteArray>

    /**
     * Поделиться файлом (Android Intent / Desktop - открытие)
     */
    fun shareFile(path: String)

    /**
     * Копировать файл в буфер обмена
     */
    fun copyFileToClipboard(path: String)
}
