package org.example.animation.io

import android.content.Context

/**
 * Хранит ссылку на Android Context, чтобы платформенные реализации
 * (например, работа с файлами) могли получать пути к кэшу и проверять разрешения.
 */
object ContextHolder {
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun get(): Context =
        appContext ?: throw IllegalStateException("ContextHolder не инициализирован. Вызовите init() в onCreate Activity.")
}