package org.example.animation.io

import org.example.animation.model.*
import kotlin.math.roundToInt

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
                sb.append("          ]")
                sb.appendLine()
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
            project.name = extractString(clean, "name") ?: "Новый проект"
            project.canvasWidth = extractInt(clean, "canvasWidth") ?: 800
            project.canvasHeight = extractInt(clean, "canvasHeight") ?: 600
            project.fps = extractInt(clean, "fps") ?: 12
            val bgHex = extractString(clean, "backgroundColor") ?: "FFFFFFFF"
            project.backgroundColor = hexToColor(bgHex)

            val layersArray = extractArray(clean, "layers") ?: return project.apply { layers.add(LayerData("Слой 1")) }
            val layerObjects = splitTopLevel(layersArray, '{', '}')

            for (layerJson in layerObjects) {
                val layer = LayerData(
                    name = extractString(layerJson, "name") ?: "Слой",
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

                    val strokesArray = extractArray(frameJson, "strokes") ?: continue
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
                    layer.frames.add(frame)
                }
                if (layer.frames.isEmpty()) layer.frames.add(FrameData())
                project.layers.add(layer)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (project.layers.isEmpty()) {
            project.layers.add(LayerData("Слой 1"))
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

    private fun extractFloat(json: String, key: String): Float? {
        val regex = "\"$key\"\\s*:\\s*(-?\\d+\\.?\\d*)(?:[,\\n\\r\\s}])".toRegex()
        val match = regex.find(json)
        return match?.groupValues?.getOrNull(1)?.toFloatOrNull()
    }

    private fun extractBoolean(json: String, key: String): Boolean? {
        val regex = "\"$key\"\\s*:\\s*(true|false)".toRegex()
        return regex.find(json)?.groupValues?.getOrNull(1)?.toBooleanStrictOrNull()
    }

    private fun extractArray(json: String, key: String): String? {
        val regex = "\"$key\"\\s*:\\s*(\\[|\\{)".toRegex()
        val match = regex.find(json) ?: return null
        val startIdx = match.range.last + 1
        val openChar = json[startIdx - 1]
        val closeChar = if (openChar == '[') ']' else '}'

        var depth = 1
        var idx = startIdx
        while (idx < json.length && depth > 0) {
            when (json[idx]) {
                openChar -> depth++
                closeChar -> depth--
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
}