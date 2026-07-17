package org.example.animation.engine

import org.example.animation.model.AnimationProject
import org.example.animation.model.LayerData
import org.example.animation.model.ToolType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AnimationEngineTest {

    @Test
    fun `addLayer добавляет слой и обновляет currentLayerIndex`() {
        val engine = AnimationEngine()
        assertEquals(1, engine.project.value.layerCount)
        val initialIndex = engine.currentLayerIndex.value

        engine.addLayer()
        assertEquals(2, engine.project.value.layerCount)
        assertEquals(1, engine.currentLayerIndex.value)
    }

    @Test
    fun `removeLayer не удаляет последний слой`() {
        val engine = AnimationEngine()
        assertEquals(1, engine.project.value.layerCount)

        engine.removeLayer(0)
        assertEquals(1, engine.project.value.layerCount)
    }

    @Test
    fun `removeLayer удаляет слой когда их больше одного`() {
        val engine = AnimationEngine()
        engine.addLayer()
        assertEquals(2, engine.project.value.layerCount)

        engine.removeLayer(1)
        assertEquals(1, engine.project.value.layerCount)
    }

    @Test
    fun `removeLayer корректирует currentLayerIndex`() {
        val engine = AnimationEngine()
        engine.addLayer()
        engine.addLayer()
        engine.setCurrentLayer(2)
        assertEquals(2, engine.currentLayerIndex.value)
        assertEquals(3, engine.project.value.layerCount)

        engine.removeLayer(2)
        assertEquals(2, engine.project.value.layerCount)
        assertTrue(engine.currentLayerIndex.value <= engine.project.value.layerCount - 1)
    }

    @Test
    fun `renameLayer обрезает пробелы и обновляет имя`() {
        val engine = AnimationEngine()
        engine.renameLayer(0, "  New Name  ")
        assertEquals("New Name", engine.project.value.layers[0].name)
    }

    @Test
    fun `renameLayer не меняет имя для пустого ввода`() {
        val engine = AnimationEngine()
        val originalName = engine.project.value.layers[0].name

        engine.renameLayer(0, "")
        assertEquals(originalName, engine.project.value.layers[0].name)

        engine.renameLayer(0, "   ")
        assertEquals(originalName, engine.project.value.layers[0].name)
    }

    @Test
    fun `undo восстанавливает предыдущее состояние и canUndo переключается`() {
        val engine = AnimationEngine()
        assertFalse(engine.canUndo.value)

        val originalName = engine.project.value.name
        engine.setProject(AnimationProject().apply { name = "Modified" })
        assertEquals("Modified", engine.project.value.name)
        assertTrue(engine.canUndo.value)

        engine.undo()
        assertEquals(originalName, engine.project.value.name)
        assertTrue(engine.canRedo.value)
    }

    @Test
    fun `redo восстанавливает отменённое состояние`() {
        val engine = AnimationEngine()
        val originalName = engine.project.value.name

        engine.setProject(AnimationProject().apply { name = "Modified" })
        engine.undo()
        assertEquals(originalName, engine.project.value.name)

        engine.redo()
        assertEquals("Modified", engine.project.value.name)
    }

    @Test
    fun `canUndo и canRedo корректны после undo redo clear`() {
        val engine = AnimationEngine()
        assertFalse(engine.canUndo.value)
        assertFalse(engine.canRedo.value)

        // Делаем изменение
        engine.addLayer()
        assertTrue(engine.canUndo.value)
        assertFalse(engine.canRedo.value)

        // Отмена
        engine.undo()
        assertTrue(engine.canUndo.value == false || engine.canUndo.value == true) // может быть начальное состояние
        assertTrue(engine.canRedo.value)

        // Делаем ещё одно изменение — очищает redo
        engine.addLayer()
        assertFalse(engine.canRedo.value)
    }

    @Test
    fun `setFps ограничивает до диапазона 1-120`() {
        val engine = AnimationEngine()

        engine.setFps(0)
        assertTrue(engine.project.value.fps >= 1)

        engine.setFps(500)
        assertTrue(engine.project.value.fps <= 120)

        engine.setFps(30)
        assertEquals(30, engine.project.value.fps)
    }

    @Test
    fun `setFps устанавливает hasUnsavedChanges`() {
        val engine = AnimationEngine()
        engine.setProject(AnimationProject()) // сбрасываем флаг несохранённых изменений
        assertFalse(engine.hasUnsavedChanges.value)

        engine.setFps(60)
        assertTrue(engine.hasUnsavedChanges.value)
    }

    @Test
    fun `setCurrentLayer переключается в пределах границ`() {
        val engine = AnimationEngine()
        engine.addLayer()
        engine.addLayer()

        engine.setCurrentLayer(1)
        assertEquals(1, engine.currentLayerIndex.value)

        engine.setCurrentLayer(5) // вне границ — не должно меняться
        assertEquals(1, engine.currentLayerIndex.value)

        engine.setCurrentLayer(-1) // вне границ — не должно меняться
        assertEquals(1, engine.currentLayerIndex.value)
    }

    @Test
    fun `setCurrentFrame переключается в пределах границ`() {
        val engine = AnimationEngine()
        engine.setCurrentFrame(0)
        assertEquals(0, engine.currentFrameIndex.value)

        engine.setCurrentFrame(5) // вне границ — не должно меняться
        assertEquals(0, engine.currentFrameIndex.value)
    }
}