package org.example.animation.io

import org.example.animation.model.AnimationProject
import org.example.animation.model.FrameData
import org.example.animation.model.ImageElement
import org.example.animation.model.LayerData
import org.example.animation.model.Stroke
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertNotNull
import kotlin.test.assertFailsWith
import kotlin.math.abs

class ProjectSerializerTest {

    @Test
    fun `round-trip serialize deserialize preserves project properties`() {
        val project = AnimationProject()
        project.name = "TestProject"
        project.canvasWidth = 1920
        project.canvasHeight = 1080
        project.fps = 30
        project.backgroundColor = 0xFFFF0000uL
        project.dpi = 300

        val json = ProjectSerializer.serialize(project)
        val restored = ProjectSerializer.deserialize(json)

        assertEquals("TestProject", restored.name)
        assertEquals(1920, restored.canvasWidth)
        assertEquals(1080, restored.canvasHeight)
        assertEquals(30, restored.fps)
        assertEquals(0xFFFF0000uL, restored.backgroundColor)
        assertEquals(300, restored.dpi)
    }

    @Test
    fun `round-trip preserves layers count and names`() {
        val project = AnimationProject()
        project.layers.clear()
        project.layers.add(LayerData("Layer One"))
        project.layers.add(LayerData("Layer Two"))
        project.layers.add(LayerData("Layer Three"))

        val json = ProjectSerializer.serialize(project)
        val restored = ProjectSerializer.deserialize(json)

        assertEquals(3, restored.layerCount)
        assertEquals("Layer One", restored.layers[0].name)
        assertEquals("Layer Two", restored.layers[1].name)
        assertEquals("Layer Three", restored.layers[2].name)
    }

    @Test
    fun `round-trip preserves strokes with coordinates`() {
        val project = AnimationProject()
        project.layers[0].frames[0].strokes.add(
            Stroke(
                points = mutableListOf(
                    androidx.compose.ui.geometry.Offset(10.5f, 20.3f),
                    androidx.compose.ui.geometry.Offset(30.7f, 40.9f)
                ),
                color = 0xFFFF00FFuL,
                strokeWidth = 5.5f,
                toolType = org.example.animation.model.ToolType.BRUSH,
                opacity = 0.75f
            )
        )

        val json = ProjectSerializer.serialize(project)
        val restored = ProjectSerializer.deserialize(json)

        assertEquals(1, restored.layers[0].frames[0].strokes.size)
        val stroke = restored.layers[0].frames[0].strokes[0]
        assertEquals(0xFFFF00FFuL, stroke.color)
        assertFloatsEqual(5.5f, stroke.strokeWidth)
        assertEquals(org.example.animation.model.ToolType.BRUSH, stroke.toolType)
        assertFloatsEqual(0.75f, stroke.opacity)
        assertEquals(2, stroke.points.size)
        assertFloatsEqual(10.5f, stroke.points[0].x)
        assertFloatsEqual(20.3f, stroke.points[0].y)
        assertFloatsEqual(30.7f, stroke.points[1].x)
        assertFloatsEqual(40.9f, stroke.points[1].y)
    }

    @Test
    fun `serializeToBytes and deserializeFromBytes round-trip`() {
        val project = AnimationProject()
        project.name = "BytesTest"
        project.canvasWidth = 400
        project.canvasHeight = 300
        project.fps = 12

        val bytes = ProjectSerializer.serializeToBytes(project)
        assertTrue(bytes.isNotEmpty())

        val restored = ProjectSerializer.deserializeFromBytes(bytes)
        assertEquals("BytesTest", restored.name)
        assertEquals(400, restored.canvasWidth)
        assertEquals(300, restored.canvasHeight)
        assertEquals(12, restored.fps)
    }

    @Test
    fun `base64 encode decode various lengths`() {
        val project = AnimationProject()
        project.layers[0].frames[0].images.add(
            ImageElement(data = byteArrayOf(0x41, 0x42, 0x43)) // 3 байта
        )
        project.layers[0].frames[0].images.add(
            ImageElement(data = byteArrayOf(0x41, 0x42)) // 2 байта
        )
        project.layers[0].frames[0].images.add(
            ImageElement(data = byteArrayOf(0x41)) // 1 байт
        )

        val json = ProjectSerializer.serialize(project)
        val restored = ProjectSerializer.deserialize(json)

        assertEquals(3, restored.layers[0].frames[0].images.size)
        assertTrue(restored.layers[0].frames[0].images[0].data.contentEquals(byteArrayOf(0x41, 0x42, 0x43)))
        assertTrue(restored.layers[0].frames[0].images[1].data.contentEquals(byteArrayOf(0x41, 0x42)))
        assertTrue(restored.layers[0].frames[0].images[2].data.contentEquals(byteArrayOf(0x41)))
    }

    @Test
    fun `base64 empty array round-trip`() {
        // Пустой массив данных не сохраняется (проверка isNotEmpty в десериализации)
        val project = AnimationProject()
        project.layers[0].frames[0].images.add(
            ImageElement(data = byteArrayOf())
        )

        val json = ProjectSerializer.serialize(project)
        val restored = ProjectSerializer.deserialize(json)

        // Пустой массив не восстанавливается — ожидаем 0 изображений
        assertEquals(0, restored.layers[0].frames[0].images.size)
    }

    @Test
    fun `deserialize empty string returns valid project`() {
        val project = ProjectSerializer.deserialize("")
        assertNotNull(project)
        assertTrue(project.layerCount >= 1)
        assertTrue(project.totalFrames >= 1)
    }

    @Test
    fun `deserialize empty object returns valid project`() {
        val project = ProjectSerializer.deserialize("{}")
        assertNotNull(project)
        assertTrue(project.layerCount >= 1)
        assertTrue(project.totalFrames >= 1)
    }

    @Test
    fun `layer names with special characters round-trip`() {
        val project = AnimationProject()
        project.layers.clear()
        project.layers.add(LayerData("Layer \"Quoted\""))
        project.layers.add(LayerData("Layer with \\ backslash"))
        project.layers.add(LayerData("Layer with / slash and \n newline"))

        val json = ProjectSerializer.serialize(project)
        val restored = ProjectSerializer.deserialize(json)

        assertEquals(3, restored.layerCount)
        assertEquals("Layer \"Quoted\"", restored.layers[0].name)
        assertEquals("Layer with \\ backslash", restored.layers[1].name)
        assertEquals("Layer with / slash and \n newline", restored.layers[2].name)
    }

    @Test
    fun `serialize preserves canvas dimensions and fps`() {
        val project = AnimationProject()
        project.canvasWidth = 640
        project.canvasHeight = 480
        project.fps = 60

        val json = ProjectSerializer.serialize(project)
        val restored = ProjectSerializer.deserialize(json)

        assertEquals(640, restored.canvasWidth)
        assertEquals(480, restored.canvasHeight)
        assertEquals(60, restored.fps)
    }

    private fun assertFloatsEqual(expected: Float, actual: Float, tolerance: Float = 0.01f) {
        assertTrue(abs(expected - actual) < tolerance, "Expected $expected but got $actual")
    }
}