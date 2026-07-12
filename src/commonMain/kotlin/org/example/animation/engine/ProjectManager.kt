package org.example.animation.engine

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.example.animation.model.AnimationProject

class ProjectManager {
    private val _engines = MutableStateFlow<List<AnimationEngine>>(listOf(AnimationEngine()))
    val engines: StateFlow<List<AnimationEngine>> = _engines.asStateFlow()

    private val _activeEngineIndex = MutableStateFlow(0)
    val activeEngineIndex: StateFlow<Int> = _activeEngineIndex.asStateFlow()

    private val _activeEngine = MutableStateFlow(_engines.value[0])
    val activeEngine: StateFlow<AnimationEngine> = _activeEngine.asStateFlow()

    fun addProject(project: AnimationProject = AnimationProject(), filePath: String? = null) {
        val newEngine = AnimationEngine(project)
        if (filePath != null) newEngine.markAsSaved(filePath)
        
        val newList = _engines.value.toMutableList()
        newList.add(newEngine)
        _engines.value = newList
        setActiveProject(newList.size - 1)
    }

    fun setActiveProject(index: Int) {
        if (index in _engines.value.indices) {
            _activeEngineIndex.value = index
            _activeEngine.value = _engines.value[index]
        }
    }

    fun closeProject(index: Int) {
        val list = _engines.value.toMutableList()
        if (index in list.indices) {
            val removed = list.removeAt(index)
            removed.cleanup()
            
            if (list.isEmpty()) {
                list.add(AnimationEngine())
            }
            
            _engines.value = list
            val nextIndex = if (_activeEngineIndex.value >= list.size) list.size - 1 else _activeEngineIndex.value
            setActiveProject(nextIndex.coerceAtLeast(0))
        }
    }

    fun moveProject(from: Int, to: Int) {
        val list = _engines.value.toMutableList()
        if (from in list.indices && to in list.indices) {
            val activeId = _engines.value[_activeEngineIndex.value].id
            val item = list.removeAt(from)
            list.add(to, item)
            _engines.value = list
            
            // Восстанавливаем индекс активного проекта по ID
            val newActiveIndex = list.indexOfFirst { it.id == activeId }
            if (newActiveIndex != -1) {
                _activeEngineIndex.value = newActiveIndex
            }
        }
    }

    fun togglePin(index: Int) {
        val engine = _engines.value.getOrNull(index) ?: return
        engine.togglePinned()
        
        // Если закрепили, перемещаем в начало списка среди закрепленных
        if (engine.isPinned.value) {
            val list = _engines.value.toMutableList()
            val item = list.removeAt(index)
            // Ищем место после последнего закрепленного
            var insertPos = 0
            for (i in list.indices) {
                if (list[i].isPinned.value) insertPos = i + 1 else break
            }
            list.add(insertPos, item)
            _engines.value = list
            setActiveProject(list.indexOf(item))
        }
    }
}
