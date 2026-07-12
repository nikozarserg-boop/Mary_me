package org.example.animation.io

/**
 * Платформенно-зависимые операции ввода/вывода
 */
expect fun createPlatformFileHandler(): PlatformFileHandler

interface PlatformFileHandler {
    fun saveFile(defaultName: String, extension: String, data: ByteArray): Boolean
    fun openFile(extension: String): ByteArray?
    fun saveToPath(path: String, data: ByteArray): Boolean
    fun readFromPath(path: String): ByteArray?
}