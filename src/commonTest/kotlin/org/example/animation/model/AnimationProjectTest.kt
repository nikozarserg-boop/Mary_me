package org.example.animation.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertNotSame

class AnimationProjectTest {

    @Test
    fun `ensureFrameCount adds frames to layers`() {
        val project = AnimationProject()
        assertEquals(1, project.totalFrames)

        project.ensureFrameCount(5)
        assertEquals(5, project.totalFrames)
        for (layer in project.layers) {
            assertEquals(5, layer.frameCount)
        }
    }

    @Test
    fun `ensureFrameCount does not reduce frame count`() {
        val project = AnimationProject()
        project.addFrameGlobal(1)
        project.addFrameGlobal(1)
        assertEquals(3, project.totalFrames)

        project.ensureFrameCount(2)
        assertEquals(3, project.totalFrames)
    }

    @Test
    fun `addLayer creates layer with correct frame count`() {
        val project = AnimationProject()
        project.ensureFrameCount(10)
        assertEquals(10, project.totalFrames)

        val layer = project.addLayer("TestLayer")
        assertEquals("TestLayer", layer.name)
        assertEquals(10, layer.frameCount)
        assertEquals(2, project.layerCount)
    }

    @Test
    fun `removeLayer removes layer when more than one`() {
        val project = AnimationProject()
        project.addLayer()
        assertEquals(2, project.layerCount)

        project.removeLayer(1)
        assertEquals(1, project.layerCount)
    }

    @Test
    fun `removeLayer does not remove last layer`() {
        val project = AnimationProject()
        assertEquals(1, project.layerCount)

        project.removeLayer(0)
        assertEquals(1, project.layerCount)
    }

    @Test
    fun `removeLayer does nothing for invalid index`() {
        val project = AnimationProject()
        project.addLayer()
        assertEquals(2, project.layerCount)

        project.removeLayer(5)
        assertEquals(2, project.layerCount)

        project.removeLayer(-1)
        assertEquals(2, project.layerCount)
    }

    @Test
    fun `moveLayer reorders layers`() {
        val project = AnimationProject()
        val layer1 = project.layers[0]
        layer1.name = "First"
        val layer2 = project.addLayer("Second")

        project.moveLayer(0, 1)
        assertEquals("Second", project.layers[0].name)
        assertEquals("First", project.layers[1].name)
    }

    @Test
    fun `moveLayer does nothing for invalid indices`() {
        val project = AnimationProject()
        project.addLayer()

        project.moveLayer(0, 5)
        assertEquals(2, project.layerCount)

        project.moveLayer(-1, 0)
        assertEquals(2, project.layerCount)
    }

    @Test
    fun `totalFrames returns max across layers`() {
        val project = AnimationProject()
        assertEquals(1, project.totalFrames)

        project.layers[0].frames.add(FrameData())
        assertEquals(2, project.totalFrames)
    }

    @Test
    fun `maxFrames returns max across layers`() {
        val project = AnimationProject()
        assertEquals(1, project.maxFrames)

        project.layers[0].frames.add(FrameData())
        assertEquals(2, project.maxFrames)
    }

    @Test
    fun `duplicateFrameGlobal duplicates frame after original`() {
        val project = AnimationProject()
        project.layers[0].frames[0].strokes.add(Stroke(color = 0xFFFF0000uL))

        val insertAt = project.duplicateFrameGlobal(0)
        assertEquals(1, insertAt)
        assertEquals(2, project.totalFrames)
        assertEquals(1, project.layers[0].frames[1].strokes.size)
        assertEquals(0xFFFF0000uL, project.layers[0].frames[1].strokes[0].color)
    }

    @Test
    fun `removeFrameGlobal removes frame from all layers`() {
        val project = AnimationProject()
        project.addFrameGlobal()
        project.addFrameGlobal()
        assertEquals(3, project.totalFrames)

        project.removeFrameGlobal(1)
        assertEquals(2, project.totalFrames)
    }

    @Test
    fun `removeFrameGlobal does not remove last frame`() {
        val project = AnimationProject()
        assertEquals(1, project.totalFrames)

        project.removeFrameGlobal(0)
        assertEquals(1, project.totalFrames)
    }

    @Test
    fun `copy creates deep independent copy`() {
        val project = AnimationProject()
        project.name = "Original"
        project.layers[0].name = "Layer A"
        project.layers[0].frames[0].strokes.add(Stroke(color = 0xFFFF0000uL))

        val copy = project.copy()
        assertEquals("Original", copy.name)
        assertEquals(1, copy.layers[0].frames[0].strokes.size)

        // Изменяем оригинал — добавляем новый штрих
        project.name = "Modified"
        project.layers[0].name = "Modified Layer"
        project.layers[0].frames[0].strokes.add(Stroke(color = 0xFF0000FFuL))
        assertEquals(2, project.layers[0].frames[0].strokes.size)

        // Проверяем, что копия не изменилась
        assertEquals("Original", copy.name)
        assertEquals("Layer A", copy.layers[0].name)
        assertEquals(1, copy.layers[0].frames[0].strokes.size)
        assertEquals(0xFFFF0000uL, copy.layers[0].frames[0].strokes[0].color)

        // Проверяем независимость списков
        assertNotSame(project.layers, copy.layers)
        assertNotSame(project.layers[0].frames, copy.layers[0].frames)
        assertNotSame(project.layers[0].frames[0].strokes, copy.layers[0].frames[0].strokes)
    }

    @Test
    fun `FrameData isEmpty returns true for empty frame`() {
        val frame = FrameData()
        assertTrue(frame.isEmpty())
    }

    @Test
    fun `FrameData isEmpty returns false when strokes exist`() {
        val frame = FrameData()
        frame.strokes.add(Stroke())
        assertFalse(frame.isEmpty())
    }

    @Test
    fun `FrameData isEmpty returns false when images exist`() {
        val frame = FrameData()
        frame.images.add(ImageElement(byteArrayOf()))
        assertFalse(frame.isEmpty())
    }

    @Test
    fun `addFrameGlobal inserts at correct position`() {
        val project = AnimationProject()
        project.layers[0].frames[0].strokes.add(Stroke(color = 0xFFFF0000uL))
        project.addFrameGlobal() // appends

        assertEquals(2, project.totalFrames)
        // Первый кадр всё ещё должен содержать штрих
        assertEquals(1, project.layers[0].frames[0].strokes.size)
        // Новый кадр должен быть пустым
        assertEquals(0, project.layers[0].frames[1].strokes.size)
    }

    @Test
    fun `moveFrameGlobal moves frame correctly`() {
        val project = AnimationProject()
        project.ensureFrameCount(3)
        project.layers[0].frames[0].strokes.add(Stroke(color = 0xFFFF0000uL))
        project.layers[0].frames[2].strokes.add(Stroke(color = 0xFF0000FFuL))

        project.moveFrameGlobal(0, 2)

        assertEquals(0xFFFF0000uL, project.layers[0].frames[2].strokes[0].color)
        assertEquals(0xFF0000FFuL, project.layers[0].frames[1].strokes[0].color)
    }
}