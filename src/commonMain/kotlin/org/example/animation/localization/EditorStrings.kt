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

object EditorStrings {
    private const val defaultLang = "ru"

    private val _currentLang = MutableStateFlow(defaultLang)
    val currentLang: StateFlow<String> = _currentLang.asStateFlow()

    private val langMeta = mapOf(
        "ru" to LangData("ru", "Russian", "Русский"),
        "en" to LangData("en", "English", "English")
    )

    private val ruStringsMap = mutableMapOf<String, String>()
    private val enStringsMap = mutableMapOf<String, String>()

    private val stringsByLang: Map<String, MutableMap<String, String>> = mapOf(
        "ru" to ruStringsMap,
        "en" to enStringsMap
    )

    init {
        // Инициализация базовыми значениями на случай задержки загрузки ресурсов
        fillDefaults()
    }

    private fun fillDefaults() {
        ruStringsMap.putAll(mapOf(
            "app.name" to "MaryMe Аниматор",
            "menu.file" to "Файл",
            "save" to "Сохранить",
            "cancel" to "Отмена",
            "apply" to "Применить",
            "ok" to "OK",
            "project.defaultName" to "Новый проект",
            "layer.defaultName" to "Слой",
            "category.drawing" to "Рисование",
            "category.eraser" to "Ластик",
            "category.shape" to "Фигуры",
            "category.curve" to "Кривые",
            "category.fill" to "Заливка",
            "category.selection" to "Выделение",
            "category.transform" to "Трансформация",
            "category.text" to "Текст",
            "category.stamp" to "Штамп",
            "category.other" to "Прочее"
        ))
        enStringsMap.putAll(mapOf(
            "app.name" to "MaryMe Animator",
            "menu.file" to "File",
            "save" to "Save",
            "cancel" to "Cancel",
            "apply" to "Apply",
            "ok" to "OK",
            "project.defaultName" to "New Project",
            "layer.defaultName" to "Layer",
            "category.drawing" to "Drawing",
            "category.eraser" to "Eraser",
            "category.shape" to "Shapes",
            "category.curve" to "Curves",
            "category.fill" to "Fill",
            "category.selection" to "Selection",
            "category.transform" to "Transform",
            "category.text" to "Text",
            "category.stamp" to "Stamp",
            "category.other" to "Other"
        ))
    }

    fun loadStrings(lang: String, content: String) {
        val parsed = StringsLoader.parseStringMap(content)
        stringsByLang[lang]?.putAll(parsed)
    }

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
        val current = stringsByLang[lang]?.get(key)
        if (current != null) return current

        val fallback = stringsByLang[defaultLang]?.get(key)
        if (fallback != null) return fallback

        return key
    }
}
