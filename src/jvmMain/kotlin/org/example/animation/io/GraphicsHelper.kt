package org.example.animation.io

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.example.animation.model.AnimationProject
import org.example.animation.model.FrameData
import org.example.animation.model.ImageElement
import org.example.animation.model.Stroke
import org.jetbrains.skia.*
import kotlin.math.*

/**
 * Flood fill реализация для JVM (использует Skia)
 */
actual fun floodFillOnBitmap(
    project: AnimationProject,
    layerIndex: Int,
    frameIndex: Int,
    point: Offset,
    fillColor: ULong
) {
    val w = project.canvasWidth
    val h = project.canvasHeight
    if (point.x < 0 || point.y < 0 || point.x >= w || point.y >= h) return

    // Создаем bitmap Skia
    val skiaBitmap = Bitmap()
    skiaBitmap.setImageInfo(ImageInfo.makeN32Premul(w, h))
    skiaBitmap.allocPixels()

    // Рендерим текущий кадр на bitmap
    renderProjectToBitmap(project, skiaBitmap)

    val px = point.x.toInt().coerceIn(0, w - 1)
    val py = point.y.toInt().coerceIn(0, h - 1)

    // Цвет замены (из ULong в Skia Color int)
    val replacement = ((fillColor shr 24) and 0xFFuL).toInt() shl 24 or
                      ((fillColor shr 16) and 0xFFuL).toInt() shl 16 or
                      ((fillColor shr 8) and 0xFFuL).toInt() shl 8 or
                      (fillColor and 0xFFuL).toInt()

    // Flood fill алгоритм через Skia
    // Используем readPixels для получения пикселей в массив
    val pixels = IntArray(w * h)
    skiaBitmap.readPixels(pixels, 0, 0, w, h)
    val target = pixels[py * w + px]
    if (target == replacement) return

    floodFillPixels(pixels, w, h, px, py, target, replacement)

    // Обновляем bitmap с измененными пикселями
    skiaBitmap.installPixels(pixels)

    // Кодируем bitmap обратно в ByteArray и сохраняем как ImageElement
    val image = skiaBitmap.makeImageSnapshot()
    val encoded = image.encodeToData(EncodedImageFormat.PNG)
    val imageData = encoded?.bytes ?: ByteArray(0)

    // Добавляем изображение на текущий кадр
    project.ensureFrameCount(frameIndex + 1)
    project.layers[layerIndex].frames.getOrNull(frameIndex)?.images?.add(
        ImageElement(data = imageData)
    )
    project.lastModifiedTimestamp = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
}

/**
 * Пипетка - получение цвета из bitmap для JVM
 */
actual fun pickColorFromBitmap(
    project: AnimationProject,
    point: Offset
): ULong? {
    val w = project.canvasWidth
    val h = project.canvasHeight
    if (point.x < 0 || point.y < 0 || point.x >= w || point.y >= h) return null

    // Создаем bitmap Skia и рендерим
    val skiaBitmap = Bitmap()
    skiaBitmap.setImageInfo(ImageInfo.makeN32Premul(w, h))
    skiaBitmap.allocPixels()

    renderProjectToBitmap(project, skiaBitmap)

    val px = point.x.toInt().coerceIn(0, w - 1)
    val py = point.y.toInt().coerceIn(0, h - 1)

    val pixels = IntArray(w * h)
    skiaBitmap.readPixels(pixels, 0, 0, w, h)
    val colorInt = pixels[py * w + px]

    // Конвертируем обратно в ULong (ARGB)
    val a = (colorInt shr 24) and 0xFF
    val r = (colorInt shr 16) and 0xFF
    val g = (colorInt shr 8) and 0xFF
    val b = colorInt and 0xFF

    return ((a.toULong() shl 24) or (r.toULong() shl 16) or (g.toULong() shl 8) or b.toULong())
}

/**
 * Рендерим проект на bitmap Skia
 */
private fun renderProjectToBitmap(project: AnimationProject, bitmap: Bitmap) {
    val canvas = Canvas(bitmap)

    // Белый фон
    canvas.clear(Color.WHITE)

    // Рендерим слои
    for (layer in project.layers.filter { it.isVisible }) {
        for (frame in layer.frames) {
            // Рендерим изображения
            for (img in frame.images) {
                try {
                    val image = Image.makeFromEncoded(img.data)
                    if (image != null) {
                        canvas.save()
                        canvas.translate(img.x, img.y)
                        canvas.rotate(img.rotation * 180f / PI.toFloat())
                        canvas.scale(img.scale, img.scale)
                        canvas.drawImage(image, 0f, 0f)
                        canvas.restore()
                    }
                } catch (e: Exception) {
                    // Игнорируем ошибки изображений
                }
            }

            for (stroke in frame.strokes) {
                drawStrokeOnCanvas(canvas, stroke)
            }
        }
    }
}

/**
 * Рисуем штрих на canvas Skia
 */
private fun drawStrokeOnCanvas(canvas: Canvas, stroke: Stroke) {
    val pts = stroke.points
    if (pts.isEmpty()) return

    // Цвет штриха
    val paint = Paint()
    val colorULong = stroke.color
    paint.color = ((colorULong shr 24) and 0xFFuL).toInt() shl 24 or
                 ((colorULong shr 16) and 0xFFuL).toInt() shl 16 or
                 ((colorULong shr 8) and 0xFFuL).toInt() shl 8 or
                 (colorULong and 0xFFuL).toInt()
    paint.strokeWidth = stroke.strokeWidth
    paint.isAntiAlias = true

    if (pts.size == 1) {
        canvas.drawCircle(pts[0].x, pts[0].y, stroke.strokeWidth / 2f, paint)
        return
    }

    // Рисуем линию через точки
    for (i in 0 until pts.size - 1) {
        canvas.drawLine(pts[i].x, pts[i].y, pts[i + 1].x, pts[i + 1].y, paint)
    }
}

/**
 * Flood fill на массив пикселей
 */
private fun floodFillPixels(pixels: IntArray, w: Int, h: Int, x: Int, y: Int, target: Int, replacement: Int) {
    if (target == replacement) return
    val stack = ArrayDeque<Pair<Int, Int>>()
    stack.addLast(x to y)
    val visited = mutableSetOf<Pair<Int, Int>>()

    while (stack.isNotEmpty()) {
        val (cx, cy) = stack.removeLast()
        if (cx < 0 || cy < 0 || cx >= w || cy >= h) continue
        if (cx to cy in visited) continue

        if (pixels[cy * w + cx] != target) continue

        visited.add(cx to cy)
        pixels[cy * w + cx] = replacement
        stack.addLast(cx + 1 to cy)
        stack.addLast(cx - 1 to cy)
        stack.addLast(cx to cy + 1)
        stack.addLast(cx to cy - 1)
    }
}