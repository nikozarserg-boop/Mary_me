package org.example.animation.io

import org.example.animation.model.*
import org.example.animation.localization.EditorStrings
import kotlin.math.roundToInt
import kotlinx.datetime.Clock

/**
 * Сериализация/десериализация проектов в JSON строку
 */
object ProjectSerializer {

    fun serialize(project: AnimationProject): String {
        val sb = StringBuilder()
        sb.appendLine("{")
        sb.appendLine("  \"name\": \"${escapeJson(project.name)}\",")
        sb.appendLine("  \"canvasWidth\": ${project.canvasWidth},")
        sb.appendLine("  \"canvasHeight\": ${project.canvasHeight},")
        sb.appendLine("  \"fps\": ${project.fps},")
        sb.appendLine("  \"backgroundColor\": \"${colorToHex(project.backgroundColor)}\",")
        sb.appendLine("  \"workingTimeMs\": ${project.workingTimeMs},")
        sb.appendLine("  \"dpi\": ${project.dpi},")
        sb.appendLine("  \"createdTimestamp\": ${project.createdTimestamp},")
        sb.appendLine("  \"lastModifiedTimestamp\": ${project.lastModifiedTimestamp},")
        sb.appendLine("  \"layers\": [")
        for ((layerIdx, layer) in project.layers.withIndex()) {
            sb.appendLine("    {")
            sb.appendLine("      \"name\": \"${escapeJson(layer.name)}\",")
            sb.appendLine("      \"isVisible\": ${layer.isVisible},")
            sb.appendLine("      \"opacity\": ${formatFloat(layer.opacity)},")
            sb.appendLine("      \"isLocked\": ${layer.isLocked},")
            sb.appendLine("      \"frames\": [")
            for ((frameIdx, frame) in layer.frames.withIndex()) {
                sb.appendLine("        {")
                sb.appendLine("          \"durationMs\": ${frame.durationMs},")
                sb.appendLine("          \"name\": \"${escapeJson(frame.name)}\",")
                
                // Штрихи
                sb.appendLine("          \"strokes\": [")
                for ((strokeIdx, stroke) in frame.strokes.withIndex()) {
                    sb.appendLine("            {")
                    sb.appendLine("              \"color\": \"${colorToHex(stroke.color)}\",")
                    sb.appendLine("              \"strokeWidth\": ${formatFloat(stroke.strokeWidth)},")
                    sb.appendLine("              \"isEraser\": ${stroke.isEraser},")
                    sb.appendLine("              \"toolType\": \"${stroke.toolType.name}\",")
                    sb.appendLine("              \"opacity\": ${formatFloat(stroke.opacity)},")
                    sb.append("              \"points\": [")
                    for ((ptIdx, pt) in stroke.points.withIndex()) {
                        sb.append("{\"x\":${formatFloat(pt.x)},\"y\":${formatFloat(pt.y)}}")
                        if (ptIdx < stroke.points.size - 1) sb.append(",")
                    }
                    sb.appendLine("]")
                    sb.append("            }")
                    if (strokeIdx < frame.strokes.size - 1) sb.append(",")
                    sb.appendLine()
                }
                sb.appendLine("          ],")
                
                // Изображения
                sb.appendLine("          \"images\": [")
                for ((imgIdx, img) in frame.images.withIndex()) {
                    sb.appendLine("            {")
                    sb.appendLine("              \"x\": ${formatFloat(img.x)},")
                    sb.appendLine("              \"y\": ${formatFloat(img.y)},")
                    sb.appendLine("              \"scale\": ${formatFloat(img.scale)},")
                    sb.appendLine("              \"rotation\": ${formatFloat(img.rotation)},")
                    sb.appendLine("              \"id\": ${img.id},")
                    sb.appendLine("              \"data\": \"${encodeBase64(img.data)}\"")
                    sb.append("            }")
                    if (imgIdx < frame.images.size - 1) sb.append(",")
                    sb.appendLine()
                }
                sb.appendLine("          ]")
                
                sb.append("        }")
                if (frameIdx < layer.frames.size - 1) sb.append(",")
                sb.appendLine()
            }
            sb.appendLine("      ]")
            sb.append("    }")
            if (layerIdx < project.layers.size - 1) sb.append(",")
            sb.appendLine()
        }
        sb.appendLine("  ]")
        sb.appendLine("}")
        return sb.toString()
    }

    fun deserialize(jsonString: String): AnimationProject {
        val project = AnimationProject()
        project.layers.clear()

        try {
            val clean = jsonString.trim()
            project.name = extractString(clean, "name") ?: EditorStrings["project.defaultName"]
            project.canvasWidth = extractInt(clean, "canvasWidth") ?: 800
            project.canvasHeight = extractInt(clean, "canvasHeight") ?: 600
            project.fps = extractInt(clean, "fps") ?: 24
            val bgHex = extractString(clean, "backgroundColor") ?: "FFFFFFFF"
            project.backgroundColor = hexToColor(bgHex)
            project.workingTimeMs = extractLong(clean, "workingTimeMs") ?: 0
            project.dpi = extractInt(clean, "dpi") ?: 72
            project.createdTimestamp = extractLong(clean, "createdTimestamp") ?: 0
            project.lastModifiedTimestamp = extractLong(clean, "lastModifiedTimestamp") ?: 0

            val layersArray = extractArray(clean, "layers") ?: return project.apply { 
                layers.add(LayerData(EditorStrings["layer.defaultName"] + " 1")) 
            }
            val layerObjects = splitTopLevel(layersArray, '{', '}')

            for (layerJson in layerObjects) {
                val layer = LayerData(
                    name = extractString(layerJson, "name") ?: EditorStrings["layer.defaultName"],
                    isVisible = extractBoolean(layerJson, "isVisible") ?: true,
                    opacity = extractFloat(layerJson, "opacity") ?: 1f,
                    isLocked = extractBoolean(layerJson, "isLocked") ?: false
                )
                layer.frames.clear()

                val framesArray = extractArray(layerJson, "frames") ?: continue
                val frameObjects = splitTopLevel(framesArray, '{', '}')
                for (frameJson in frameObjects) {
                    val frame = FrameData(
                        durationMs = extractInt(frameJson, "durationMs") ?: 83,
                        name = extractString(frameJson, "name") ?: ""
                    )
                    frame.strokes.clear()
                    frame.images.clear()

                    // Чтение штрихов
                    val strokesArray = extractArray(frameJson, "strokes")
                    if (strokesArray != null) {
                        val strokeObjects = splitTopLevel(strokesArray, '{', '}')
                        for (strokeJson in strokeObjects) {
                            val toolTypeName = extractString(strokeJson, "toolType") ?: "PEN"
                            val toolType = try { ToolType.valueOf(toolTypeName) } catch (e: Exception) { ToolType.PEN }
                            val stroke = Stroke(
                                color = hexToColor(extractString(strokeJson, "color") ?: "FF000000"),
                                strokeWidth = extractFloat(strokeJson, "strokeWidth") ?: 4f,
                                isEraser = extractBoolean(strokeJson, "isEraser") ?: false,
                                toolType = toolType,
                                opacity = extractFloat(strokeJson, "opacity") ?: 1f
                            )

                            val pointsArray = extractArray(strokeJson, "points") ?: continue
                            val pointObjects = splitTopLevel(pointsArray, '{', '}')
                            for (ptJson in pointObjects) {
                                val x = extractFloat(ptJson, "x") ?: 0f
                                val y = extractFloat(ptJson, "y") ?: 0f
                                stroke.points.add(androidx.compose.ui.geometry.Offset(x, y))
                            }
                            frame.strokes.add(stroke)
                        }
                    }

                    // Чтение изображений
                    val imagesArray = extractArray(frameJson, "images")
                    if (imagesArray != null) {
                        val imageObjects = splitTopLevel(imagesArray, '{', '}')
                        for (imgJson in imageObjects) {
                            val base64Data = extractString(imgJson, "data") ?: ""
                            val imageData = decodeBase64(base64Data)
                            if (imageData.isNotEmpty()) {
                                val image = ImageElement(
                                    data = imageData,
                                    x = extractFloat(imgJson, "x") ?: 0f,
                                    y = extractFloat(imgJson, "y") ?: 0f,
                                    scale = extractFloat(imgJson, "scale") ?: 1f,
                                    rotation = extractFloat(imgJson, "rotation") ?: 0f,
                                    id = extractLong(imgJson, "id") ?: Clock.System.now().toEpochMilliseconds()
                                )
                                frame.images.add(image)
                            }
                        }
                    }

                    layer.frames.add(frame)
                }
                if (layer.frames.isEmpty()) layer.frames.add(FrameData())
                project.layers.add(layer)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (project.layers.isEmpty()) {
            project.layers.add(LayerData(EditorStrings["layer.defaultName"] + " 1"))
        }
        return project
    }

    fun serializeToBytes(project: AnimationProject): ByteArray = serialize(project).encodeToByteArray()
    fun deserializeFromBytes(bytes: ByteArray): AnimationProject = deserialize(bytes.decodeToString())

    private fun extractString(json: String, key: String): String? {
        val regex = "\"$key\"\\s*:\\s*\"([^\"]*)\"".toRegex()
        return regex.find(json)?.groupValues?.getOrNull(1)
    }

    private fun extractInt(json: String, key: String): Int? {
        val regex = "\"$key\"\\s*:\\s*(-?\\d+)".toRegex()
        return regex.find(json)?.groupValues?.getOrNull(1)?.toIntOrNull()
    }

    private fun extractLong(json: String, key: String): Long? {
        val regex = "\"$key\"\\s*:\\s*(-?\\d+)".toRegex()
        return regex.find(json)?.groupValues?.getOrNull(1)?.toLongOrNull()
    }

    private fun extractFloat(json: String, key: String): Float? {
        val regex = "\"$key\"\\s*:\\s*(-?\\d+\\.?\\d*)".toRegex()
        val match = regex.find(json)
        return match?.groupValues?.getOrNull(1)?.toFloatOrNull()
    }

    private fun extractBoolean(json: String, key: String): Boolean? {
        val regex = "\"$key\"\\s*:\\s*(true|false)".toRegex()
        return regex.find(json)?.groupValues?.getOrNull(1)?.toBooleanStrictOrNull()
    }

    private fun extractArray(json: String, key: String): String? {
        val regex = "\"$key\"\\s*:\\s*(\\[)".toRegex()
        val match = regex.find(json) ?: return null
        val startIdx = match.range.last + 1
        var depth = 1
        var idx = startIdx
        while (idx < json.length && depth > 0) {
            when (json[idx]) {
                '[' -> depth++
                ']' -> depth--
                '"' -> {
                    idx++
                    while (idx < json.length && json[idx] != '"') {
                        if (json[idx] == '\\') idx++
                        idx++
                    }
                }
            }
            idx++
        }
        return json.substring(startIdx - 1, idx)
    }

    private fun splitTopLevel(array: String, open: Char, close: Char): List<String> {
        val results = mutableListOf<String>()
        var depth = 0
        var start = -1
        var i = 0
        while (i < array.length) {
            when (array[i]) {
                open -> {
                    if (depth == 0) start = i
                    depth++
                }
                close -> {
                    depth--
                    if (depth == 0 && start >= 0) {
                        results.add(array.substring(start, i + 1))
                        start = -1
                    }
                }
            }
            i++
        }
        return results
    }

    private fun escapeJson(s: String): String {
        return s.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }

    private fun colorToHex(color: ULong): String {
        return color.toString(16).padStart(8, '0').uppercase()
    }

    private fun hexToColor(hex: String): ULong {
        return try {
            hex.removePrefix("#").toULong(16)
        } catch (e: Exception) {
            0xFF000000uL
        }
    }

    private fun formatFloat(value: Float): String {
        val rounded = (value * 1000).roundToInt() / 1000f
        return if (rounded == rounded.toInt().toFloat()) rounded.toInt().toString() else rounded.toString()
    }

    // Простой Base64 для кроссплатформенности без зависимостей
    private const val BASE64_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"

    private fun encodeBase64(data: ByteArray): String {
        val result = StringBuilder()
        var i = 0
        while (i < data.size) {
            val b1 = data[i].toInt() and 0xFF
            val b2 = if (i + 1 < data.size) data[i + 1].toInt() and 0xFF else -1
            val b3 = if (i + 2 < data.size) data[i + 2].toInt() and 0xFF else -1

            result.append(BASE64_ALPHABET[(b1 shr 2) and 0x3F])
            if (b2 != -1) {
                result.append(BASE64_ALPHABET[((b1 shl 4) or (b2 shr 4)) and 0x3F])
                if (b3 != -1) {
                    result.append(BASE64_ALPHABET[((b2 shl 2) or (b3 shr 6)) and 0x3F])
                    result.append(BASE64_ALPHABET[b3 and 0x3F])
                } else {
                    result.append(BASE64_ALPHABET[(b2 shl 2) and 0x3F])
                    result.append('=')
                }
            } else {
                result.append(BASE64_ALPHABET[(b1 shl 4) and 0x3F])
                result.append("==")
            }
            i += 3
        }
        return result.toString()
    }

    private fun decodeBase64(base64: String): ByteArray {
        if (base64.isEmpty()) return byteArrayOf()
        val clean = base64.replace("=", "").replace("\n", "").replace("\r", "")
        val result = mutableListOf<Byte>()
        var i = 0
        while (i < clean.length) {
            val c1 = BASE64_ALPHABET.indexOf(clean[i])
            val c2 = if (i + 1 < clean.length) BASE64_ALPHABET.indexOf(clean[i + 1]) else 0
            val c3 = if (i + 2 < clean.length) BASE64_ALPHABET.indexOf(clean[i + 2]) else -1
            val c4 = if (i + 3 < clean.length) BASE64_ALPHABET.indexOf(clean[i + 3]) else -1

            result.add(((c1 shl 2) or (c2 shr 4)).toByte())
            if (c3 != -1) {
                result.add(((c2 shl 4) or (c3 shr 2)).toByte())
                if (c4 != -1) {
                    result.add(((c3 shl 6) or c4).toByte())
                }
            }
            i += 4
        }
        return result.toByteArray()
    }
}
