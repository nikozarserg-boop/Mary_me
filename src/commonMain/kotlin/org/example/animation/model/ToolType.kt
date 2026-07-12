package org.example.animation.model

/**
 * Типы инструментов рисования
 */
enum class ToolType(val displayName: String, val icon: String) {
    // Эмодзи удалены: используйте EditorIcons в UI.
    PEN("Перо", "pen"),
    BRUSH("Кисть", "brush"),
    PENCIL("Карандаш", "pencil"),
    ERASER("Ластик", "eraser"),
    LINE("Линия", "line"),
    RECTANGLE("Прямоугольник", "rectangle"),
    ELLIPSE("Эллипс", "ellipse"),
    FILL("Заливка", "fill"),
    EYEDROPPER("Пипетка", "eyedropper"),
    SELECT("Выделение", "select"),
    MOVE("Перемещение", "move")
}
