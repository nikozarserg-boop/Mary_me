package org.example.animation.io

import androidx.compose.ui.geometry.Offset
import org.example.animation.model.AnimationProject
import org.example.animation.model.ImageElement
import org.example.animation.model.Stroke
import java.awt.image.BufferedImage
import java.awt.Graphics2D
import java.awt.Color

/**
 * Flood fill реализация для JVM (использует AWT BufferedImage)
 * Работает только с текущим кадром активного слоя (без призрачных кадров)
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

    // Создаём BufferedImage
    val bufferedImage = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
    val graphics = bufferedImage.createGraphics()

    // Рендерим ТОЛЬКО текущий кадр активного слоя (без призрачных кадров)
    renderCurrentFrameToBufferedImage(project, graphics, w, h, layerIndex, frameIndex)

    val px = point.x.toInt().coerceIn(0, w - 1)
    val py = point.y.toInt().coerceIn(0, h - 1)

    // Цвет замены (из ULong в int AWT)
    val replacement = ((fillColor shr 24) and 0xFFuL).toInt() shl 24 or
                      ((fillColor shr 16) and 0xFFuL).toInt() shl 16 or
                      ((fillColor shr 8) and 0xFFuL).toInt() shl 8 or
                      (fillColor and 0xFFuL).toInt()

    // Заливка области (flood fill)
    val target = bufferedImage.getRGB(px, py)
    if (target == replacement) {
        graphics.dispose()
        return
    }

    floodFillBufferedImage(bufferedImage, px, py, target, replacement)
    graphics.dispose()

    // Кодируем BufferedImage в PNG
    val imageData = encodeBufferedImageToPNG(bufferedImage)

    // Добавляем изображение на текущий кадр
    project.ensureFrameCount(frameIndex + 1)
    project.layers[layerIndex].frames.getOrNull(frameIndex)?.images?.add(
        ImageElement(data = imageData)
    )
    project.lastModifiedTimestamp = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
}

/**
 * Пипетка - получение цвета из bitmap для JVM
 * Работает только с текущим кадром активного слоя (без призрачных кадров)
 */
actual fun pickColorFromBitmap(
    project: AnimationProject,
    point: Offset,
    layerIndex: Int,
    frameIndex: Int
): ULong? {
    val w = project.canvasWidth
    val h = project.canvasHeight
    if (point.x < 0 || point.y < 0 || point.x >= w || point.y >= h) return null

    // Создаём BufferedImage
    val bufferedImage = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
    val graphics = bufferedImage.createGraphics()

    // Рендерим ТОЛЬКО текущий кадр (без призрачных кадров)
    renderCurrentFrameToBufferedImage(project, graphics, w, h, layerIndex, frameIndex)

    val px = point.x.toInt().coerceIn(0, w - 1)
    val py = point.y.toInt().coerceIn(0, h - 1)

    val colorInt = bufferedImage.getRGB(px, py)
    graphics.dispose()

    // Конвертируем обратно в ULong (ARGB)
    val a = (colorInt shr 24) and 0xFF
    val r = (colorInt shr 16) and 0xFF
    val g = (colorInt shr 8) and 0xFF
    val b = colorInt and 0xFF

    return ((a.toULong() shl 24) or (r.toULong() shl 16) or (g.toULong() shl 8) or b.toULong())
}

/**
 * Рендерим ТОЛЬКО текущий кадр активного слоя на BufferedImage (без призрачных кадров)
 */
private fun renderCurrentFrameToBufferedImage(
    project: AnimationProject, 
    graphics: Graphics2D, 
    w: Int, 
    h: Int,
    layerIndex: Int,
    frameIndex: Int
) {
    graphics.clearRect(0, 0, w, h)
    graphics.color = Color.WHITE
    graphics.fillRect(0, 0, w, h)

    // Рендерим только один слой и один кадр (без призрачных кадров)
    val layer = project.layers.getOrNull(layerIndex) ?: return
    val frame = layer.frames.getOrNull(frameIndex) ?: return
    
    for (stroke in frame.strokes) {
        drawStrokeOnGraphics(graphics, stroke)
    }
}

/**
 * Рисуем штрих на Graphics2D
 */
private fun drawStrokeOnGraphics(graphics: Graphics2D, stroke: Stroke) {
    val pts = stroke.points
    if (pts.isEmpty()) return

    // Цвет штриха
    val colorULong = stroke.color
    val awtColor = Color(
        ((colorULong shr 16) and 0xFFuL).toInt(), // R
        ((colorULong shr 8) and 0xFFuL).toInt(),  // G
        (colorULong and 0xFFuL).toInt(),          // B
        ((colorULong shr 24) and 0xFFuL).toInt()   // A
    )
    graphics.color = awtColor
    graphics.stroke = java.awt.BasicStroke(stroke.strokeWidth.toFloat())

    if (pts.size == 1) {
        graphics.fillOval(
            pts[0].x.toInt() - stroke.strokeWidth.toInt() / 2,
            pts[0].y.toInt() - stroke.strokeWidth.toInt() / 2,
            stroke.strokeWidth.toInt(),
            stroke.strokeWidth.toInt()
        )
        return
    }

    // Рисуем линию через точки
    for (i in 0 until pts.size - 1) {
        graphics.drawLine(pts[i].x.toInt(), pts[i].y.toInt(), pts[i + 1].x.toInt(), pts[i + 1].y.toInt())
    }
}

/**
 * Flood fill на BufferedImage
 */
private fun floodFillBufferedImage(image: BufferedImage, x: Int, y: Int, target: Int, replacement: Int) {
    if (target == replacement) return
    val w = image.width
    val h = image.height
    val stack = ArrayDeque<Pair<Int, Int>>()
    stack.addLast(x to y)
    val visited = mutableSetOf<Pair<Int, Int>>()

    while (stack.isNotEmpty()) {
        val (cx, cy) = stack.removeLast()
        if (cx < 0 || cy < 0 || cx >= w || cy >= h) continue
        if (cx to cy in visited) continue

        if (image.getRGB(cx, cy) != target) continue

        visited.add(cx to cy)
        image.setRGB(cx, cy, replacement)
        stack.addLast(cx + 1 to cy)
        stack.addLast(cx - 1 to cy)
        stack.addLast(cx to cy + 1)
        stack.addLast(cx to cy - 1)
    }
}

/**
 * Кодируем BufferedImage в PNG bytes
 */
private fun encodeBufferedImageToPNG(image: BufferedImage): ByteArray {
    val outputStream = java.io.ByteArrayOutputStream()
    javax.imageio.ImageIO.write(image, "PNG", outputStream)
    return outputStream.toByteArray()
}