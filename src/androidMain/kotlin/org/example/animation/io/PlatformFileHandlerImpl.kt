package org.example.animation.io

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Environment
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.ContextCompat
import java.io.File

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

actual fun encodeImage(bitmap: ImageBitmap): ByteArray {
    return try {
        // TODO: Proper implementation for Android
        ByteArray(0)
    } catch (e: Exception) {
        e.printStackTrace()
        ByteArray(0)
    }
}

class AndroidPlatformFileHandler : PlatformFileHandler {

    // Проверяем, есть ли доступ к внешнему хранилищу (для API ≤ 32)
    private fun hasStoragePermission(): Boolean {
        val ctx: Context = ContextHolder.get()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            true // на API 33+ scoped storage, запись в свои каталоги не требует явного пермишена
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
        return null // Требует системного диалога (в реальном приложении нужно пробрасывать Activity/Fragment)
    }

    override fun saveToPath(path: String, data: ByteArray): Boolean {
        if (!hasStoragePermission()) return false
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
        if (!hasStoragePermission()) return null
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
        // Корректный путь к кэшу через Context (вместо захардкоженной строки)
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
        // Заглушка
    }

    override fun fileExists(path: String): Boolean = File(path).exists()

    override fun deleteFile(path: String): Boolean = File(path).delete()
}
