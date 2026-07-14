package org.example.animation.io

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat

/**
 * Android реализация проверки разрешений.
 */
actual fun hasStoragePermissions(): Boolean {
    val context = ContextHolder.get()
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_MEDIA_IMAGES
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }
}

/**
 * Android реализация запроса разрешений.
 */
actual fun requestStoragePermissions(onComplete: (Boolean) -> Unit) {
    PermissionHandler.requestPermissions(onComplete)
}

/**
 * Обработчик разрешений для Android.
 * Launcher'ы инициализируются из MainActivity.
 */
object PermissionHandler {
    private var permissionLauncher: ActivityResultLauncher<Array<String>>? = null
    private var pendingCallback: ((Boolean) -> Unit)? = null
    
    /**
     * Инициализирует launcher'ы из MainActivity.
     */
    fun initialize(
        permissionLauncher: ActivityResultLauncher<Array<String>>,
        manageStorageLauncher: ActivityResultLauncher<Intent> // Оставляем для совместимости подписи, но не используем
    ) {
        this.permissionLauncher = permissionLauncher
    }
    
    /**
     * Запускает запрос разрешений.
     */
    fun requestPermissions(onComplete: (Boolean) -> Unit) {
        pendingCallback = onComplete
        
        val permissions = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO)
            }
            Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q -> {
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
            else -> {
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
        
        val context = ContextHolder.get()
        val notGranted = permissions.filter { permission ->
            ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED
        }
        
        if (notGranted.isEmpty()) {
            onComplete(true)
            return
        }
        
        permissionLauncher?.launch(notGranted.toTypedArray()) ?: onComplete(false)
    }
    
    /**
     * Callback от manageStorageLauncher (больше не используется для новых версий, но оставим пустой метод).
     */
    fun onManageStorageResult() {
        pendingCallback?.invoke(hasStoragePermissions())
    }
    
    /**
     * Callback от permissionLauncher.
     */
    fun onPermissionResult(isGranted: Boolean) {
        pendingCallback?.invoke(isGranted)
    }
}
