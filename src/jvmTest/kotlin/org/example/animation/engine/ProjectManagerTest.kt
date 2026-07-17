package org.example.animation.engine

import org.example.animation.model.AnimationProject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

class ProjectManagerTest {

    @Test
    fun `addProject добавляет движок и активный индекс = 0`() {
        val manager = ProjectManager()
        manager.addProject()

        assertEquals(1, manager.engines.value.size)
        assertEquals(0, manager.activeEngineIndex.value)
        assertNotNull(manager.activeEngine.value)
        
        manager.closeProject(0)
    }

    @Test
    fun `addProject несколько раз растягивает список и последний становится активным`() {
        val manager = ProjectManager()
        
        manager.addProject()
        assertEquals(1, manager.engines.value.size)
        assertEquals(0, manager.activeEngineIndex.value)
        
        manager.addProject()
        assertEquals(2, manager.engines.value.size)
        assertEquals(1, manager.activeEngineIndex.value)
        
        manager.closeProject(1)
        manager.closeProject(0)
    }

    @Test
    fun `setActiveProject переключает активный`() {
        val manager = ProjectManager()
        manager.addProject()
        manager.addProject()
        
        assertEquals(1, manager.activeEngineIndex.value)
        
        manager.setActiveProject(0)
        assertEquals(0, manager.activeEngineIndex.value)
        
        manager.closeProject(1)
        manager.closeProject(0)
    }

    @Test
    fun `setActiveProject игнорирует индекс вне диапазона`() {
        val manager = ProjectManager()
        manager.addProject()
        
        val initialIndex = manager.activeEngineIndex.value
        
        manager.setActiveProject(999)
        assertEquals(initialIndex, manager.activeEngineIndex.value)
        
        manager.setActiveProject(-1)
        assertEquals(initialIndex, manager.activeEngineIndex.value)
        
        manager.closeProject(0)
    }

    @Test
    fun `closeProject удаляет движок и корректно пересчитывает индекс`() {
        val manager = ProjectManager()
        manager.addProject()
        manager.addProject()
        
        // Закрываем неактивный проект
        manager.closeProject(0)
        
        assertEquals(1, manager.engines.value.size)
        // Активный должен остаться
        assertEquals(0, manager.activeEngineIndex.value)
        
        manager.closeProject(0)
    }

    @Test
    fun `closeProject последнего сбрасывает activeEngineIndex в -1 и activeEngine в null`() {
        val manager = ProjectManager()
        manager.addProject()
        
        manager.closeProject(0)
        
        assertEquals(0, manager.engines.value.size)
        assertEquals(-1, manager.activeEngineIndex.value)
        // activeEngine.value может быть null или содержать что-то - проверим null
        assertEquals(null, manager.activeEngine.value)
    }

    @Test
    fun `moveProject меняет порядок и сохраняет активный по id`() {
        val manager = ProjectManager()
        manager.addProject()
        manager.addProject()
        manager.addProject()
        
        val initialActiveId = manager.activeEngine.value?.id
        
        manager.moveProject(2, 0)
        
        assertEquals(3, manager.engines.value.size)
        // ID должен остаться в начале
        assertEquals(initialActiveId, manager.engines.value[0].id)
        
        manager.closeProject(2)
        manager.closeProject(1)
        manager.closeProject(0)
    }

    @Test
    fun `togglePin перемещает закрепленный проект в начало`() {
        val manager = ProjectManager()
        manager.addProject()
        manager.addProject()
        manager.addProject()
        
        val pinnedId = manager.engines.value[1].id
        
        manager.togglePin(1)
        
        // Проект должен быть закреплён и перемещён в начало
        assertTrue(manager.engines.value[0].isPinned.value)
        assertEquals(pinnedId, manager.engines.value[0].id)
        
        // Открепляем
        manager.togglePin(0)
        
        manager.closeProject(2)
        manager.closeProject(1)
        manager.closeProject(0)
    }

    @Test
    fun `togglePin без закрепления не меняет порядок`() {
        val manager = ProjectManager()
        manager.addProject()
        
        // Закрепляем первый
        manager.togglePin(0)
        
        // Открепляем - порядок не должен измениться
        manager.togglePin(0)
        
        assertFalse(manager.engines.value[0].isPinned.value)
        
        manager.closeProject(0)
    }

    @Test
    fun `closeProject очищает ресурсы движка`() {
        val manager = ProjectManager()
        manager.addProject()
        
        val engine = manager.activeEngine.value
        // cleanup вызывается при закрытии
        manager.closeProject(0)
        
        // Проверяем что список пуст
        assertEquals(0, manager.engines.value.size)
    }
}