package org.example.animation.io

import java.io.File
import android.os.Environment
import android.content.Context

/**
 * Android реализация платформенно-зависимого файлового ввода/вывода
 */
actual fun createPlatformFileHandler(): PlatformFileHandler = AndroidPlatformFileHandler()

class AndroidPlatformFileHandler : PlatformFileHandler {
    override fun saveFile(defaultName: String, extension: String, data: ByteArray): Boolean {
        // На Android обычно используется системный Picker через ActivityResultContract
        // В данной реализации через File API для простоты (требует разрешений)
        return saveToPath(getDocumentsDirectory() + "/" + defaultName + "." + extension, data)
    }

    override fun openFile(extension: String): ByteArray? {
        return null // Требует системного диалога
    }

    override fun saveToPath(path: String, data: ByteArray): Boolean {
        return try {
            val file = File(path)
            file.parentFile?.mkdirs()
            file.writeBytes(data)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    override fun readFromPath(path: String): ByteArray? {
        return try {
            val file = File(path)
            if (file.exists()) file.readBytes() else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override fun getDocumentsDirectory(): String {
        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).absolutePath
    }

    override fun getCacheDirectory(): String {
        return "/data/user/0/org.example.mary_me/cache" 
    }

    override fun listFiles(path: String): List<FileEntry> {
        val dir = File(path)
        if (!dir.exists() || !dir.isDirectory) return emptyList()
        
        return dir.listFiles()?.map {
            FileEntry(
                name = it.name,
                path = it.absolutePath,
                isDirectory = it.isDirectory,
                extension = it.extension
            )
        } ?: emptyList()
    }

    override fun getParentPath(path: String): String? {
        return File(path).parent
    }

    override fun getHomeDirectory(): String {
        return Environment.getExternalStorageDirectory().absolutePath
    }

    override fun openInExplorer(path: String) {
        // Заглушка
    }

    override fun fileExists(path: String): Boolean = File(path).exists()

    override fun deleteFile(path: String): Boolean = File(path).delete()
}
