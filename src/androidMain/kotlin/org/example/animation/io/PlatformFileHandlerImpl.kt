package org.example.animation.io

import java.io.File
import android.os.Environment

/**
 * Android реализация платформенно-зависимого файлового ввода/вывода
 */
actual fun createPlatformFileHandler(): PlatformFileHandler = AndroidPlatformFileHandler()

class AndroidPlatformFileHandler : PlatformFileHandler {
    override fun saveFile(defaultName: String, extension: String, data: ByteArray): Boolean {
        return false
    }

    override fun openFile(extension: String): ByteArray? {
        return null
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
            File(path).readBytes()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override fun getDocumentsDirectory(): String {
        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).absolutePath
    }

    override fun getCacheDirectory(): String {
        // На Android используем внутренний кэш приложения
        return "/data/user/0/org.example.mary_me/cache" 
    }

    override fun openInExplorer(path: String) {
        // На Android это сложнее через Intent, оставим заглушку или базовый лог
    }

    override fun fileExists(path: String): Boolean = File(path).exists()

    override fun deleteFile(path: String): Boolean = File(path).delete()
}
