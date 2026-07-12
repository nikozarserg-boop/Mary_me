package org.example.animation.io

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
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
 * Кодирование ImageBitmap в байты (PNG)
 */
expect fun encodeImage(bitmap: ImageBitmap): ByteArray

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
    fun deleteFile(path: String): Boolean
}