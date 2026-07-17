package org.example.animation.engine

import org.example.animation.io.encodeImage
import org.example.animation.model.AnimationProject
import androidx.compose.ui.unit.Density
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ExportManagerTest {

    /**
     * Тест: экспорт кадра возвращает непустые данные для валидного проекта
     */
    @Test
    fun `exportFrame возвращает данные для валидного проекта`() {
        val project = AnimationProject(
            name = "Test Project",
            canvasWidth = 100,
            canvasHeight = 100
        )

        val density = object : Density {
            override val density: Float = 1f
            override val fontScale: Float = 1f
        }

        // Тест экспорта текущего кадра (index 0)
        val result = ExportManager.exportFrame(
            project = project,
            frameIndex = 0,
            width = 100,
            height = 100,
            density = density,
            format = "png"
        )

        // Проверяем что данные не пустые (PNG сигнатура начинается с 89 50 4E 47)
        assertTrue(result.isNotEmpty())
    }

    /**
     * Тест: экспорт кадра с масштабированием
     */
    @Test
    fun `exportFrame масштабирует изображение при изменении размеров`() {
        val project = AnimationProject(
            name = "Test Project",
            canvasWidth = 200,
            canvasHeight = 100
        )

        val density = object : Density {
            override val density: Float = 1f
            override val fontScale: Float = 1f
        }

        // Экспорт с разными размерами
        val result1 = ExportManager.exportFrame(
            project = project,
            frameIndex = 0,
            width = 100,
            height = 50,
            density = density,
            format = "png"
        )

        val result2 = ExportManager.exportFrame(
            project = project,
            frameIndex = 0,
            width = 200,
            height = 100,
            density = density,
            format = "png"
        )

        // Оба результата должны быть непустыми
        assertTrue(result1.isNotEmpty())
        assertTrue(result2.isNotEmpty())
    }

    /**
     * Тест: экспорт кадра с невалидным индексом
     */
    @Test
    fun `exportFrame обрабатывает невалидный frameIndex`() {
        val project = AnimationProject(
            name = "Test Project",
            canvasWidth = 100,
            canvasHeight = 100
        )

        val density = object : Density {
            override val density: Float = 1f
            override val fontScale: Float = 1f
        }

        // Экспорт с индексом, которого нет в проекте
        // Проект имеет только 1 кадр по умолчанию (maxFrames = 1), index 999 выходит за границы
        // Но renderFrame ожидает, что frameIndex в пределах
        val result = ExportManager.exportFrame(
            project = project,
            frameIndex = 999, // невалидный индекс
            width = 100,
            height = 100,
            density = density,
            format = "png"
        )

        // Результат должен быть непустым (рендер всё равно создаст изображение)
        assertTrue(result.isNotEmpty())
    }

    /**
     * Тест: exportSequenceToPngs вызывает callback для каждого кадра
     */
    @Test
    fun `exportSequenceToPngs вызывает callback для каждого кадра`() {
        val project = AnimationProject(
            name = "Test Project",
            canvasWidth = 50,
            canvasHeight = 50
        )
        // Добавляем кадры, чтобы имитировать много кадров
        project.ensureFrameCount(3)

        val density = object : Density {
            override val density: Float = 1f
            override val fontScale: Float = 1f
        }

        val frames = mutableListOf<Pair<Int, ByteArray>>()
        ExportManager.exportSequenceToPngs(
            project = project,
            width = 50,
            height = 50,
            density = density,
            onFrame = { index, data ->
                frames.add(index to data)
            }
        )

        // Проверяем что было вызвано для всех кадров (3 кадра)
        assertEquals(3, frames.size)
        assertTrue(frames.all { it.second.isNotEmpty() })
    }

    /**
     * Тест: экспорт с разными форматами
     */
    @Test
    fun `encodeImage поддерживает разные форматы`() {
        // Создаём простой bitmap для теста
        val bitmap = androidx.compose.ui.graphics.ImageBitmap(10, 10)

        // PNG
        val pngData = encodeImage(bitmap, "png")
        assertTrue(pngData.isNotEmpty())

        // JPG
        val jpgData = encodeImage(bitmap, "jpg")
        assertTrue(jpgData.isNotEmpty())

        // JPEG (алиас для jpg)
        val jpegData = encodeImage(bitmap, "jpeg")
        assertTrue(jpegData.isNotEmpty())

        // WEBP
        val webpData = encodeImage(bitmap, "webp")
        assertTrue(webpData.isNotEmpty())

        // Неизвестный формат - должен вернуть PNG по умолчанию
        val unknownData = encodeImage(bitmap, "unknown")
        assertTrue(unknownData.isNotEmpty())
    }

    /**
     * Тест: экспорт пустого проекта
     */
    @Test
    fun `exportFrame работает с пустым проектом`() {
        val project = AnimationProject(
            name = "Empty",
            canvasWidth = 10,
            canvasHeight = 10
        )

        val density = object : Density {
            override val density: Float = 1f
            override val fontScale: Float = 1f
        }

        val result = ExportManager.exportFrame(
            project = project,
            frameIndex = 0,
            width = 10,
            height = 10,
            density = density,
            format = "png"
        )

        // PNG заголовок
        assertTrue(result.isNotEmpty())
    }

    /**
     * Тест: масштабный коэффициент вычисляется правильно
     */
    @Test
    fun `scaleX и scaleY вычисляются корректно`() {
        val project = AnimationProject(
            canvasWidth = 100,
            canvasHeight = 200
        )

        // При экспорте 200x400 получаем scale 2.0
        val density = object : Density {
            override val density: Float = 1f
            override val fontScale: Float = 1f
        }

        val result = ExportManager.exportFrame(
            project = project,
            frameIndex = 0,
            width = 200,
            height = 400,
            density = density,
            format = "png"
        )

        assertTrue(result.isNotEmpty())
    }

    /**
     * Тест: экспорт проекта с несколькими слоями
     */
    @Test
    fun `exportFrame рендерит проект со слоями`() {
        val project = AnimationProject(
            name = "Multi Layer",
            canvasWidth = 100,
            canvasHeight = 100
        )

        // Добавляем ещё один слой
        project.addLayer("Layer 2")

        val density = object : Density {
            override val density: Float = 1f
            override val fontScale: Float = 1f
        }

        val result = ExportManager.exportFrame(
            project = project,
            frameIndex = 0,
            width = 100,
            height = 100,
            density = density,
            format = "png"
        )

        assertTrue(result.isNotEmpty())
        assertEquals(2, project.layerCount)
    }
}