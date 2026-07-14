package org.example.animation.io

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Environment
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.Density
import androidx.core.content.ContextCompat
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.example.animation.engine.ExportManager
import org.example.animation.model.AnimationProject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

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

class AndroidPlatformFileHandler : PlatformFileHandler {

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
        val tempDir = File(getCacheDirectory(), "export_frames_" + System.currentTimeMillis())
        tempDir.mkdirs()
        
        try {
            // 1. Рендерим все кадры в PNG (промежуточный формат)
            ExportManager.exportSequenceToPngs(project, width, height, density) { index, data ->
                val frameFile = File(tempDir, "frame_%04d.png".format(index))
                frameFile.writeBytes(data)
                onProgress(0.1f + (index.toFloat() / project.maxFrames) * 0.4f)
            }

            // 2. Формируем команду FFmpeg для разных форматов
            val inputPattern = File(tempDir, "frame_%04d.png").absolutePath
            val cmd = when (format.lowercase()) {
                "gif" -> "-y -framerate $fps -i $inputPattern -vf \"split[s0][s1];[s0]palettegen[p];[s1][p]paletteuse\" $outputPath"
                "apng" -> "-y -framerate $fps -i $inputPattern -plays 0 $outputPath"
                "mp4" -> "-y -framerate $fps -i $inputPattern -c:v libx264 -pix_fmt yuv420p -crf 23 $outputPath"
                "webm" -> "-y -framerate $fps -i $inputPattern -c:v libvpx-vp9 -pix_fmt yuv420p -crf 30 -b:v 0 $outputPath"
                "mov" -> "-y -framerate $fps -i $inputPattern -c:v prores_ks -pix_fmt yuva444p10le $outputPath"
                "mkv" -> "-y -framerate $fps -i $inputPattern -c:v libx264 -pix_fmt yuv420p $outputPath"
                "avi" -> "-y -framerate $fps -i $inputPattern -c:v mpeg4 -q:v 5 $outputPath"
                else -> "-y -framerate $fps -i $inputPattern $outputPath"
            }

            // 3. Запускаем FFmpeg
            val session = FFmpegKit.execute(cmd)
            onProgress(0.9f)
            
            val returnCode = session.returnCode
            ReturnCode.isSuccess(returnCode)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            tempDir.deleteRecursively()
            onProgress(1.0f)
        }
    }
}
