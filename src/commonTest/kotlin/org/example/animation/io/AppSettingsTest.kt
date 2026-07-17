package org.example.animation.io

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertNotNull

class AppSettingsTest {

    private val json = kotlinx.serialization.json.Json { 
        ignoreUnknownKeys = true 
        encodeDefaults = true 
    }

    @Test
    fun `сериализация AppSettings сохраняет все поля`() {
        val settings = AppSettings(
            uiScale = 1.5f,
            language = "en",
            theme = "LIGHT",
            autoSaveEnabled = false,
            autoSaveIntervalMin = 5,
            ghostFramesColor = 0xFFFF0000uL,
            ghostFramesEnabled = false,
            ghostFramesBefore = 3,
            ghostFramesAfter = 2,
            brushSize = 10f,
            smoothingLevel = 2,
            antiAliasingEnabled = false,
            lastActiveTool = "ERASER",
            currentColor = 0xFF00FF00uL,
            currentOpacity = 0.75f,
            currentBrushIndex = 3,
            recentProjects = mutableListOf(
                RecentProject(path = "/path1", name = "Project 1", lastAccessed = 1000L),
                RecentProject(path = "/path2", name = "Project 2", lastAccessed = 2000L)
            )
        )

        val jsonString = json.encodeToString(AppSettings.serializer(), settings)
        val restored = json.decodeFromString(AppSettings.serializer(), jsonString)

        assertEquals(1.5f, restored.uiScale)
        assertEquals("en", restored.language)
        assertEquals("LIGHT", restored.theme)
        assertEquals(false, restored.autoSaveEnabled)
        assertEquals(5, restored.autoSaveIntervalMin)
        assertEquals(0xFFFF0000uL, restored.ghostFramesColor)
        assertEquals(false, restored.ghostFramesEnabled)
        assertEquals(3, restored.ghostFramesBefore)
        assertEquals(2, restored.ghostFramesAfter)
        assertEquals(10f, restored.brushSize)
        assertEquals(2, restored.smoothingLevel)
        assertEquals(false, restored.antiAliasingEnabled)
        assertEquals("ERASER", restored.lastActiveTool)
        assertEquals(0xFF00FF00uL, restored.currentColor)
        assertEquals(0.75f, restored.currentOpacity)
        assertEquals(3, restored.currentBrushIndex)
        assertEquals(2, restored.recentProjects.size)
        assertEquals("Project 1", restored.recentProjects[0].name)
        assertEquals("Project 2", restored.recentProjects[1].name)
    }

    @Test
    fun `ignoreUnknownKeys позволяет парсить JSON с неизвестными ключами`() {
        val jsonWithUnknown = """{
            "uiScale": 2.0,
            "language": "ru",
            "theme": "DARK",
            "unknownKey": "value",
            "anotherUnknown": 123
        }"""

        val restored = json.decodeFromString(AppSettings.serializer(), jsonWithUnknown)

        assertEquals(2.0f, restored.uiScale)
        assertEquals("ru", restored.language)
        assertEquals("DARK", restored.theme)
    }

    @Test
    fun `ULong поля сериализуются корректно`() {
        val settings = AppSettings(
            ghostFramesColor = 0xFF123456uL,
            currentColor = 0xFFFEDCBAuL
        )

        val jsonString = json.encodeToString(AppSettings.serializer(), settings)
        val restored = json.decodeFromString(AppSettings.serializer(), jsonString)

        assertEquals(0xFF123456uL, restored.ghostFramesColor)
        assertEquals(0xFFFEDCBAuL, restored.currentColor)
    }

    @Test
    fun `recentProjects список сериализуется корректно`() {
        val settings = AppSettings(
            recentProjects = mutableListOf(
                RecentProject(path = "/docs/proj1.maryme", name = "Project 1", lastAccessed = 123456789L),
                RecentProject(path = "/docs/proj2.maryme", name = "Project 2", lastAccessed = 987654321L)
            )
        )

        val jsonString = json.encodeToString(AppSettings.serializer(), settings)
        val restored = json.decodeFromString(AppSettings.serializer(), jsonString)

        assertNotNull(restored.recentProjects)
        assertEquals(2, restored.recentProjects.size)
        assertEquals("/docs/proj1.maryme", restored.recentProjects[0].path)
        assertEquals("/docs/proj2.maryme", restored.recentProjects[1].path)
    }
}
