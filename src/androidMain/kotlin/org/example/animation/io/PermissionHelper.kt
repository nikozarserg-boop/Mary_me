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
    return when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
            // Android 11+ (API 30+): проверяем MANAGE_EXTERNAL_STORAGE
            Environment.isExternalStorageManager()
        }
        else -> {
            // Android 10 и ниже: проверяем READ_EXTERNAL_STORAGE
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
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
    private var manageStorageLauncher: ActivityResultLauncher<Intent>? = null
    private var pendingCallback: ((Boolean) -> Unit)? = null
    
    /**
     * Инициализирует launcher'ы из MainActivity.
     */
    fun initialize(
        permissionLauncher: ActivityResultLauncher<Array<String>>,
        manageStorageLauncher: ActivityResultLauncher<Intent>
    ) {
        this.permissionLauncher = permissionLauncher
        this.manageStorageLauncher = manageStorageLauncher
    }
    
    /**
     * Запускает запрос разрешений.
     */
    fun requestPermissions(onComplete: (Boolean) -> Unit) {
        pendingCallback = onComplete
        
        val context = ContextHolder.get()
        
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                // Android 11+: запрос через Settings для MANAGE_EXTERNAL_STORAGE
                if (Environment.isExternalStorageManager()) {
                    onComplete(true)
                    return
                }
                
                val intent = Intent(
                    "android.settings.MANAGE_APP_ALL_FILES_ACCESS_PERMISSION",
                    Uri.parse("package:${context.packageName}")
                )
                
                manageStorageLauncher?.launch(intent) ?: run {
                    try {
                        context.startActivity(intent)
                        onComplete(true)
                    } catch (e: Exception) {
                        onComplete(false)
                    }
                }
            }
            else -> {
                // Android 6-10: обычные runtime-разрешения
                val permissions = buildList {
                    add(Manifest.permission.READ_EXTERNAL_STORAGE)
                    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
                        add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    }
                }
                
                val notGranted = permissions.filter { permission ->
                    ContextCompat.checkSelfPermission(
                        context,
                        permission
                    ) != PackageManager.PERMISSION_GRANTED
                }
                
                if (notGranted.isEmpty()) {
                    onComplete(true)
                    return
                }
                
                permissionLauncher?.launch(notGranted.toTypedArray())
                    ?: onComplete(false)
            }
        }
    }
    
    /**
     * Callback от manageStorageLauncher.
     */
    fun onManageStorageResult() {
        pendingCallback?.invoke(Environment.isExternalStorageManager())
    }
    
    /**
     * Callback от permissionLauncher.
     */
    fun onPermissionResult(isGranted: Boolean) {
        pendingCallback?.invoke(isGranted)
    }
}