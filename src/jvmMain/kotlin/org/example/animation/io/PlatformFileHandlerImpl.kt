package org.example.animation.io

import java.io.File
import java.awt.Desktop
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.unit.Density
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.example.animation.engine.ExportManager
import org.example.animation.model.AnimationProject
import org.jetbrains.skia.Image
import org.jetbrains.skia.EncodedImageFormat
import ws.schild.jave.process.ffmpeg.DefaultFFMPEGLocator

/**
 * JVM реализация платформенно-зависимого файлового ввода/вывода
 */
actual fun createPlatformFileHandler(): PlatformFileHandler = JvmPlatformFileHandler()

actual fun decodeImage(data: ByteArray): ImageBitmap? {
    return try {
        Image.makeFromEncoded(data).toComposeImageBitmap()
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

actual fun encodeImage(bitmap: ImageBitmap, format: String): ByteArray {
    return try {
        val skiaBitmap = bitmap.asSkiaBitmap()
        val image = Image.makeFromBitmap(skiaBitmap)
        val skiaFormat = when (format.lowercase()) {
            "jpg", "jpeg" -> EncodedImageFormat.JPEG
            "webp" -> EncodedImageFormat.WEBP
            else -> EncodedImageFormat.PNG
        }
        val data = image.encodeToData(skiaFormat, 90)
        data?.bytes ?: ByteArray(0)
    } catch (e: Exception) {
        e.printStackTrace()
        ByteArray(0)
    }
}

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
                "jpg", "jpeg" -> FileNameExtensionFilter("JPEG Image (*.jpg)", "jpg")
                "webp" -> FileNameExtensionFilter("WEBP Image (*.webp)", "webp")
                "avi" -> FileNameExtensionFilter("AVI Video (*.avi)", "avi")
                "mp4" -> FileNameExtensionFilter("MP4 Video (*.mp4)", "mp4")
                "webm" -> FileNameExtensionFilter("WebM Video (*.webm)", "webm")
                "mov" -> FileNameExtensionFilter("QuickTime Video (*.mov)", "mov")
                "mkv" -> FileNameExtensionFilter("Matroska Video (*.mkv)", "mkv")
                "apng" -> FileNameExtensionFilter("Animated PNG (*.png)", "png")
                else -> FileNameExtensionFilter("$extension files (*.$extension)", extension)
            }
            chooser.fileFilter = filter

            val result = chooser.showSaveDialog(null)
            if (result == JFileChooser.APPROVE_OPTION) {
                var file = chooser.selectedFile
                if (!file.name.lowercase().endsWith(".$extension")) {
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
                "brush" -> FileNameExtensionFilter("Brush Preset (*.brush)", "brush")
                else -> FileNameExtensionFilter("All supported files", "maryme", "gif", "png", "brush")
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
        val file = File(path)
        return if (file.exists()) file.readBytes() else null
    }

    override fun getDocumentsDirectory(): String = System.getProperty("user.home") + "/Documents"

    override fun getCacheDirectory(): String {
        val dir = File(System.getProperty("java.io.tmpdir"), "maryme_cache")
        if (!dir.exists()) dir.mkdirs()
        return dir.absolutePath
    }

    override fun listFiles(path: String): List<FileEntry> {
        val dir = File(path)
        return dir.listFiles()?.map { FileEntry(it.name, it.absolutePath, it.isDirectory, it.extension) } ?: emptyList()
    }

    override fun getParentPath(path: String): String? = File(path).parent
    override fun getHomeDirectory(): String = System.getProperty("user.home")
    override fun openInExplorer(path: String) {
        try { Desktop.getDesktop().open(File(path).parentFile) } catch (e: Exception) {}
    }

    override fun fileExists(path: String): Boolean = File(path).exists()
    override fun deleteFile(path: String): Boolean = File(path).delete()

    override suspend fun exportAnimation(
        project: AnimationProject,
        outputPath: String,
        format: String,
        width: Int,
        height: Int,
        fps: Int,
        density: Density,
        onProgress: (Float) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        val tempDir = File(getCacheDirectory(), "export_${System.currentTimeMillis()}")
        tempDir.mkdirs()

        try {
            // 1. Рендерим кадры (всегда PNG как промежуточный)
            ExportManager.exportSequenceToPngs(project, width, height, density) { index, data ->
                File(tempDir, "frame_%04d.png".format(index)).writeBytes(data)
                onProgress(0.1f + (index.toFloat() / project.maxFrames) * 0.4f)
            }

            // 2. Используем встроенный FFmpeg (JAVE извлекает нужный бинарник под ОС)
            val locator = DefaultFFMPEGLocator()
            val ffmpegExecutable = locator.executablePath

            val cmd = mutableListOf<String>().apply {
                add(ffmpegExecutable)
                add("-y")
                add("-framerate")
                add(fps.toString())
                add("-i")
                add(File(tempDir, "frame_%04d.png").absolutePath)

                when (format.lowercase()) {
                    "gif" -> {
                        add("-vf")
                        add("split[s0][s1];[s0]palettegen[p];[s1][p]paletteuse")
                    }
                    "apng" -> {
                        add("-plays")
                        add("0")
                    }
                    "mp4" -> {
                        add("-c:v")
                        add("libx264")
                        add("-pix_fmt")
                        add("yuv420p")
                        add("-crf")
                        add("23")
                    }
                    "webm" -> {
                        add("-c:v")
                        add("libvpx-vp9")
                        add("-pix_fmt")
                        add("yuv420p")
                        add("-crf")
                        add("30")
                        add("-b:v")
                        add("0")
                    }
                    "mov" -> {
                        add("-c:v")
                        add("prores_ks")
                        add("-pix_fmt")
                        add("yuv420p")
                    }
                    "mkv" -> {
                        add("-c:v")
                        add("libx264")
                        add("-pix_fmt")
                        add("yuv420p")
                    }
                    "avi" -> {
                        add("-c:v")
                        add("mpeg4")
                        add("-q:v")
                        add("5")
                    }
                }
                add(outputPath)
            }

            val process = ProcessBuilder(cmd).inheritIO().start()
            onProgress(0.9f)
            process.waitFor() == 0
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            tempDir.deleteRecursively()
            onProgress(1.0f)
        }
    }
}
