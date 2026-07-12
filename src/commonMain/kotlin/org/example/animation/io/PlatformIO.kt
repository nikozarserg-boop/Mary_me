package org.example.animation.io

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
