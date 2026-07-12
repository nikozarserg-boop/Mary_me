package org.example.animation.localization

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.int
import kotlinx.serialization.json.double
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonArray

/**
 * Вынесено отдельно, чтобы [EditorStrings] не зависел от конкретного способа загрузки.
 * Сейчас поддерживаем загрузку из JSON-словарей, которые лежат в composeResources/files.
 */
internal object StringsLoader {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun parseStringMap(rawJson: String): Map<String, String> {
        val root = json.parseToJsonElement(rawJson).jsonObject
        return root.mapValues { (_, v) ->
            v.jsonPrimitive.content
        }
    }
}

