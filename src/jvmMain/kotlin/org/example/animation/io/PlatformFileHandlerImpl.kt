package org.example.animation.io

import java.io.File
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
            val chooser = JFileChooser(lastDirectory ?: System.getProperty("user.home"))
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
            val chooser = JFileChooser(lastDirectory ?: System.getProperty("user.home"))
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