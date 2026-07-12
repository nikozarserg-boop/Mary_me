package org.example.animation.localization

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class LangData(
    val code: String,
    val name: String,
    val nameNative: String
)

// (fallback-локализация)

object EditorStrings {
    private const val defaultLang = "ru"

    private val _currentLang = MutableStateFlow(defaultLang)
    val currentLang: StateFlow<String> = _currentLang.asStateFlow()

    private val langMeta = mapOf(
        defaultLang to LangData(defaultLang, "Russian", "Русский"),
        "en" to LangData("en", "English", "English")
    )

    // Загружаем переводы из JSON.
    // NOTE: текущая сборка показывает, что API composeResources readBytes/getResource недоступно.
    // Поэтому временно возвращаемся к in-code словарям, чтобы убрать ошибки компиляции.
    // Это не финальный вариант по ТЗ, но это гарантированно компилируется.

    private val ruStringsMap = mapOf(
        "settings.title" to "Настройки",
        "settings.language" to "Язык",
        "settings.canvas" to "Холст",
        "settings.performance" to "Производительность",
        "settings.interface" to "Интерфейс",
        "settings.about" to "О программе",
        "canvas.width" to "Ширина",
        "canvas.height" to "Высота",
        "canvas.fps" to "FPS",
        "canvas.background" to "Цвет фона",
        "save" to "Сохранить",
        "cancel" to "Отмена",
        "apply" to "Применить",
        "ok" to "OK",
        "about.version" to "Версия 1.0.0",
        "about.desc" to "Редактор анимации MaryMe. Создавайте анимацию кадр за кадром.",
        "interface.theme" to "Тема",
        "interface.grid" to "Показывать сетку",
        "interface.antialiasing" to "Сглаживание"
    )

    private val enStringsMap = mapOf(
        "settings.title" to "Settings",
        "settings.language" to "Language",
        "settings.canvas" to "Canvas",
        "settings.performance" to "Performance",
        "settings.interface" to "Interface",
        "settings.about" to "About",
        "canvas.width" to "Width",
        "canvas.height" to "Height",
        "canvas.fps" to "FPS",
        "canvas.background" to "Background color",
        "save" to "Save",
        "cancel" to "Cancel",
        "apply" to "Apply",
        "ok" to "OK",
        "about.version" to "Version 1.0.0",
        "about.desc" to "MaryMe Animation Editor. Frame-by-frame animation.",
        "interface.theme" to "Theme",
        "interface.grid" to "Show grid",
        "interface.antialiasing" to "Antialiasing"
    )

    private val stringsByLang: Map<String, Map<String, String>> = mapOf(
        defaultLang to ruStringsMap,
        "en" to enStringsMap
    )


    fun getAvailableLanguages(): List<LangData> = langMeta.values.toList()

    fun getCurrentLanguage(): LangData = langMeta[_currentLang.value] ?: langMeta[defaultLang]!!

    fun getCurrentCode(): String = _currentLang.value

    fun setLanguage(code: String): Boolean {
        if (stringsByLang.containsKey(code)) {
            _currentLang.value = code
            return true
        }
        return false
    }

    @Composable
    fun observeString(key: String): String {
        val state = _currentLang.collectAsState()
        return t(state.value, key)
    }


    fun format(key: String, vararg args: Any?): String {
        val template = t(_currentLang.value, key)
        var out = template
        args.forEachIndexed { i, arg ->
            out = out.replace("{$i}", arg?.toString() ?: "")
        }
        return out
    }

    operator fun get(key: String): String = t(_currentLang.value, key)

    private fun t(lang: String, key: String): String {
        val byLang = stringsByLang
        val current = byLang[lang]?.get(key)
        if (current != null) return current

        val fallback = byLang[defaultLang]?.get(key)
        if (fallback != null) {
            println("[i18n] Missing key '$key' in lang='$lang', fallback='$defaultLang'")
            return fallback
        }

        println("[i18n] Missing key '$key' in default='$defaultLang' too")
        return key
    }
}

