package org.example.animation.io

import android.app.Activity
import android.content.Context
import androidx.activity.result.ActivityResultLauncher
import kotlinx.coroutines.CompletableDeferred

/**
 * Хранит ссылку на Android Context и Activity.
 */
object ContextHolder {
    private var appContext: Context? = null
    private var activity: Activity? = null
    
    // Для асинхронного открытия файлов через SAF
    var filePickerLauncher: ActivityResultLauncher<Array<String>>? = null
    var pendingFilePick: CompletableDeferred<ByteArray?>? = null

    fun init(context: Context) {
        appContext = context.applicationContext
        if (context is Activity) {
            this.activity = context
        }
    }

    fun get(): Context =
        appContext ?: throw IllegalStateException("ContextHolder не инициализирован. Вызовите init() в onCreate Activity.")
    
    fun getActivity(): Activity? = activity
}
