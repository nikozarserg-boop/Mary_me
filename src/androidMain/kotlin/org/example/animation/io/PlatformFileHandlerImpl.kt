package org.example.animation.io

import java.io.File

/**
 * Android реализация платформенно-зависимого файлового ввода/вывода
 */
actual fun createPlatformFileHandler(): PlatformFileHandler = AndroidPlatformFileHandler()

class AndroidPlatformFileHandler : PlatformFileHandler {
    override fun saveFile(defaultName: String, extension: String, data: ByteArray): Boolean {
        // На Android используем встроенный FileManagerDialog в Compose, 
        // поэтому этот метод может не использоваться напрямую.
        return false
    }

    override fun openFile(extension: String): ByteArray? {
        // На Android используем встроенный FileManagerDialog в Compose
        return null
    }

    override fun saveToPath(path: String, data: ByteArray): Boolean {
        return try {
            File(path).writeBytes(data)
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
}
