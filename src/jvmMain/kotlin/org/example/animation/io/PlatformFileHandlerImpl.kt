package org.example.animation.io

import java.io.File
import java.awt.Desktop
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

/**
 * JVM реализация платформенно-зависимого файлового ввода/вывода
 */
actual fun createPlatformFileHandler(): PlatformFileHandler = JvmPlatformFileHandler()

class JvmPlatformFileHandler : PlatformFileHandler {
    private var lastDirectory: String? = null

    override fun saveFile(defaultName: String, extension: String, data: ByteArray): Boolean {
        return try {
            val chooser = JFileChooser(lastDirectory ?: getDocumentsDirectory())
            chooser.selectedFile = File(defaultName)
            chooser.dialogTitle = "Сохранить файл"
            
            val filter = when (extension.lowercase()) {
                "maryme" -> FileNameExtensionFilter("MaryMe Project (*.maryme)", "maryme")
                "gif" -> FileNameExtensionFilter("GIF Animation (*.gif)", "gif")
                "png" -> FileNameExtensionFilter("PNG Image (*.png)", "png")
                "avi" -> FileNameExtensionFilter("AVI Video (*.avi)", "avi")
                else -> FileNameExtensionFilter("$extension files (*.$extension)", extension)
            }
            chooser.fileFilter = filter

            val result = chooser.showSaveDialog(null)
            if (result == JFileChooser.APPROVE_OPTION) {
                var file = chooser.selectedFile
                if (!file.name.contains(".")) {
                    file = File(file.absolutePath + ".$extension")
                }
                lastDirectory = file.parent
                file.writeBytes(data)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    override fun openFile(extension: String): ByteArray? {
        return try {
            val chooser = JFileChooser(lastDirectory ?: getDocumentsDirectory())
            chooser.dialogTitle = "Открыть файл"
            
            val filter = when (extension.lowercase()) {
                "maryme" -> FileNameExtensionFilter("MaryMe Project (*.maryme)", "maryme")
                "gif" -> FileNameExtensionFilter("GIF Animation (*.gif)", "gif")
                "png" -> FileNameExtensionFilter("PNG Image (*.png)", "png")
                "avi" -> FileNameExtensionFilter("AVI Video (*.avi)", "avi")
                else -> FileNameExtensionFilter("All files", "*.*")
            }
            chooser.fileFilter = filter

            val result = chooser.showOpenDialog(null)
            if (result == JFileChooser.APPROVE_OPTION) {
                val file = chooser.selectedFile
                lastDirectory = file.parent
                file.readBytes()
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
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
        val userHome = System.getProperty("user.home")
        val docs = File(userHome, "Documents")
        return if (docs.exists()) docs.absolutePath else userHome
    }

    override fun getCacheDirectory(): String {
        val dir = File(System.getProperty("user.home"), ".maryme/cache")
        if (!dir.exists()) dir.mkdirs()
        return dir.absolutePath
    }

    override fun openInExplorer(path: String) {
        try {
            val file = File(path)
            if (Desktop.isDesktopSupported()) {
                val desktop = Desktop.getDesktop()
                if (file.isDirectory) desktop.open(file)
                else desktop.open(file.parentFile ?: File(System.getProperty("user.home")))
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    override fun fileExists(path: String): Boolean = File(path).exists()

    override fun deleteFile(path: String): Boolean = File(path).delete()
}
