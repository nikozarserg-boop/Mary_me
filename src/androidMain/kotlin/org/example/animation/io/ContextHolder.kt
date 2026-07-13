package org.example.animation.io

import android.app.Activity
import android.content.Context

/**
 * Хранит ссылку на Android Context и Activity, чтобы платформенные реализации
 * (например, работа с файлами и запрос разрешений) могли получать пути к кэшу 
 * и запрашивать разрешения через ActivityResultLauncher.
 */
object ContextHolder {
    private var appContext: Context? = null
    private var activity: Activity? = null

    fun init(context: Context) {
        appContext = context.applicationContext
        if (context is Activity) {
            this.activity = context
        }
    }

    fun get(): Context =
        appContext ?: throw IllegalStateException("ContextHolder не инициализирован. Вызовите init() в onCreate Activity.")
    
    /**
     * Возвращает Activity для запроса разрешений.
     */
    fun getActivity(): Activity? = activity
}