package org.example.animation.io

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.Density
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.example.animation.engine.ExportManager
import org.example.animation.model.AnimationProject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream
 
/**
 * Android реализация платформенно-зависимого файлового ввода/вывода
 */
actual fun createPlatformFileHandler(): PlatformFileHandler = AndroidPlatformFileHandler()
 
/**
 * Декодирование изображения для Android
 */
actual fun decodeImage(data: ByteArray): ImageBitmap? {
    return try {
        BitmapFactory.decodeByteArray(data, 0, data.size)?.asImageBitmap()
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

actual fun encodeImage(bitmap: ImageBitmap, format: String): ByteArray {
    return try {
        val androidBitmap = bitmap.asAndroidBitmap()
        val stream = ByteArrayOutputStream()
        
        val compressFormat = when (format.lowercase()) {
            "jpg", "jpeg" -> Bitmap.CompressFormat.JPEG
            "webp" -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Bitmap.CompressFormat.WEBP_LOSSLESS
            } else {
                @Suppress("DEPRECATION")
                Bitmap.CompressFormat.WEBP
            }
            else -> Bitmap.CompressFormat.PNG
        }
        
        androidBitmap.compress(compressFormat, 90, stream)
        stream.toByteArray()
    } catch (e: Exception) {
        e.printStackTrace()
        ByteArray(0)
    }
}

// Кодируем необработанные пиксели в PNG для Android
actual fun encodeRawToPng(pixels: ByteArray, width: Int, height: Int, bytesPerPixel: Int): ByteArray {
    return try {
        val config = if (bytesPerPixel == 1) {
            Bitmap.Config.ALPHA_8
        } else {
            Bitmap.Config.ARGB_8888
        }
        val bitmap = Bitmap.createBitmap(width, height, config)
        
        if (bytesPerPixel == 1) {
            // Для grayscale копируем как есть
            val buffer = java.nio.ByteBuffer.wrap(pixels)
            bitmap.copyPixelsFromBuffer(buffer)
        } else {
            // Для RGBA преобразуем в формат ARGB, который ожидает Android Bitmap
            val argbPixels = IntArray(width * height)
            for (i in 0 until width * height) {
                val offset = i * 4
                // GBR формат: R, G, B, A -> Android ожидает ARGB
                val a = (pixels[offset + 3].toInt() and 0xFF)
                val r = (pixels[offset + 0].toInt() and 0xFF)
                val g = (pixels[offset + 1].toInt() and 0xFF)
                val b = (pixels[offset + 2].toInt() and 0xFF)
                argbPixels[i] = (a shl 24) or (r shl 16) or (g shl 8) or b
            }
            bitmap.eraseColor(android.graphics.Color.TRANSPARENT)
            for (i in argbPixels.indices) {
                val x = i % width
                val y = i / width
                bitmap.setPixel(x, y, argbPixels[i])
            }
        }
        
        // Сжимаем в PNG
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        stream.toByteArray()
    } catch (e: Exception) {
        e.printStackTrace()
        ByteArray(0)
    }
}

// Утилита unzip для Android
actual fun unzip(bytes: ByteArray): Map<String, ByteArray> {
    val result = mutableMapOf<String, ByteArray>()
    ZipInputStream(ByteArrayInputStream(bytes)).use { zis ->
        var entry = zis.nextEntry
        while (entry != null) {
            if (!entry.isDirectory) {
                result[entry.name] = zis.readAllBytes()
            }
            entry = zis.nextEntry
        }
    }
    return result
}

class AndroidPlatformFileHandler : PlatformFileHandler {

    init {
        FFmpegKit.executeAsync("-encoders") { session ->
            Log.d("FFmpeg", "Available encoders: " + session.allLogsAsString)
        }
    }

    private fun hasStoragePermission(): Boolean {
        val ctx: Context = ContextHolder.get()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            true
        } else {
            ContextCompat.checkSelfPermission(
                ctx,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    override fun saveFile(defaultName: String, extension: String, data: ByteArray): Boolean {
        if (!hasStoragePermission()) return false
        return saveToPath(getDocumentsDirectory() + "/" + defaultName + "." + extension, data)
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
        return ContextHolder.get().cacheDir.absolutePath
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
        shareFile(path)
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
        val tempDir = File(getCacheDirectory(), "export_frames_" + System.currentTimeMillis())
        tempDir.mkdirs()
        
        try {
            ExportManager.exportSequenceToPngs(project, width, height, density) { index, data ->
                val frameFile = File(tempDir, "frame_%04d.png".format(index))
                frameFile.writeBytes(data)
                onProgress(0.1f + (index.toFloat() / (project.maxFrames.takeIf { it > 0 } ?: 1)) * 0.4f)
            }

            val inputPattern = File(tempDir, "frame_%04d.png").absolutePath
            val cmd = when (format.lowercase()) {
                "gif" -> "-y -framerate $fps -i \"$inputPattern\" -vf \"split[s0][s1];[s0]palettegen[p];[s1][p]paletteuse\" \"$outputPath\""
                "apng" -> "-y -framerate $fps -i \"$inputPattern\" -plays 0 \"$outputPath\""
                "mp4" -> "-y -framerate $fps -i \"$inputPattern\" -c:v libx264 -pix_fmt yuv420p -crf 23 \"$outputPath\""
                "webm" -> "-y -framerate $fps -i \"$inputPattern\" -c:v libvpx-vp9 -pix_fmt yuv420p -crf 30 -b:v 0 \"$outputPath\""
                "mov" -> "-y -framerate $fps -i \"$inputPattern\" -c:v prores_ks -pix_fmt yuva444p10le \"$outputPath\""
                "mkv" -> "-y -framerate $fps -i \"$inputPattern\" -c:v libx264 -pix_fmt yuv420p \"$outputPath\""
                "avi" -> "-y -framerate $fps -i \"$inputPattern\" -c:v mpeg4 -q:v 5 \"$outputPath\""
                else -> "-y -framerate $fps -i \"$inputPattern\" \"$outputPath\""
            }

            val session = FFmpegKit.execute(cmd)
            onProgress(0.9f)
            
            if (ReturnCode.isSuccess(session.returnCode)) {
                null
            } else {
                session.allLogsAsString
            }
        } catch (e: Exception) {
            e.message ?: "Unknown error"
        } finally {
            tempDir.deleteRecursively()
            onProgress(1.0f)
        }
    }

    override suspend fun extractVideoFrames(path: String, fps: Int, maxFrames: Int): List<ByteArray> = withContext(Dispatchers.IO) {
        val tempDir = File(getCacheDirectory(), "extract_" + System.currentTimeMillis())
        tempDir.mkdirs()
        val result = mutableListOf<ByteArray>()
        try {
            val outputPattern = File(tempDir, "frame_%04d.png").absolutePath
            val cmd = "-y -i \"$path\" -vf \"fps=$fps\" -vframes $maxFrames \"$outputPattern\""
            val session = FFmpegKit.execute(cmd)
            if (ReturnCode.isSuccess(session.returnCode)) {
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
            val context = ContextHolder.get()
            val file = File(path)
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = context.contentResolver.getType(uri) ?: "application/octet-stream"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Share via").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun copyFileToClipboard(path: String) {
        try {
            val context = ContextHolder.get()
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val file = File(path)
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val clip = ClipData.newUri(context.contentResolver, "File", uri)
            clipboard.setPrimaryClip(clip)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}