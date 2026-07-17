package org.example.animation.io

import java.io.File
import java.awt.Desktop
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.unit.Density
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import org.example.animation.engine.ExportManager
import org.example.animation.model.AnimationProject
import org.jetbrains.skia.Image
import org.jetbrains.skia.EncodedImageFormat
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.ArrayList

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
    private val ffmpegPath: String by lazy {
        org.bytedeco.javacpp.Loader.load(org.bytedeco.ffmpeg.ffmpeg::class.java)
    }

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
    override fun isDirectory(path: String): Boolean = File(path).isDirectory
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
    ): String? = withContext(Dispatchers.IO) {
        val tempDir = File(getCacheDirectory(), "export_${System.currentTimeMillis()}")
        tempDir.mkdirs()

        try {
            ExportManager.exportSequenceToPngs(project, width, height, density) { index, data ->
                File(tempDir, "frame_%04d.png".format(index)).writeBytes(data)
                onProgress(0.1f + (index.toFloat() / (project.maxFrames.takeIf { it > 0 } ?: 1)) * 0.4f)
            }

            val inputPattern = File(tempDir, "frame_%04d.png").absolutePath
            val cmd = mutableListOf<String>().apply {
                add(ffmpegPath)
                add("-y")
                add("-framerate")
                add(fps.toString())
                add("-i")
                add(inputPattern)
                
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
                        add("yuva444p10le")
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

            val process = ProcessBuilder(cmd).redirectErrorStream(true).start()
            
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            val log = StringBuilder()
            
            val progressJob = launch {
                while (process.isAlive) {
                    delay(200)
                    onProgress(0.9f)
                }
            }
            
            while (reader.readLine().also { line = it } != null) {
                log.append(line).append("\n")
            }
            
            val exitCode = process.waitFor()
            progressJob.cancel()
            
            if (exitCode == 0) null else log.toString()
        } catch (e: Exception) {
            e.message ?: "Unknown error"
        } finally {
            tempDir.deleteRecursively()
            onProgress(1.0f)
        }
    }

    override suspend fun extractVideoFrames(path: String, fps: Int, maxFrames: Int): List<ByteArray> = withContext(Dispatchers.IO) {
        val tempDir = File(getCacheDirectory(), "extract_${System.currentTimeMillis()}")
        tempDir.mkdirs()
        val result = mutableListOf<ByteArray>()
        try {
            val outputPattern = File(tempDir, "frame_%04d.png").absolutePath
            val cmd = listOf(ffmpegPath, "-y", "-i", path, "-vf", "fps=$fps", "-vframes", maxFrames.toString(), outputPattern)
            val process = ProcessBuilder(cmd).start()
            if (process.waitFor() == 0) {
                tempDir.listFiles()?.sortedBy { it.name }?.forEach {
                    result.add(it.readBytes())
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            tempDir.deleteRecursively()
        }
        result
    }

    override fun shareFile(path: String) {
        try {
            Desktop.getDesktop().open(File(path))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun copyFileToClipboard(path: String) {
        try {
            val file = File(path)
            val selection = object : Transferable {
                override fun getTransferDataFlavors(): Array<DataFlavor> = arrayOf(DataFlavor.javaFileListFlavor)
                override fun isDataFlavorSupported(flavor: DataFlavor): Boolean = flavor == DataFlavor.javaFileListFlavor
                override fun getTransferData(flavor: DataFlavor): Any {
                    if (flavor != DataFlavor.javaFileListFlavor) throw Exception("Unsupported flavor")
                    val list = ArrayList<File>()
                    list.add(file)
                    return list
                }
            }
            Toolkit.getDefaultToolkit().systemClipboard.setContents(selection, null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}