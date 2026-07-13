package org.example.animation.model

/**
 * Типы инструментов рисования - полная классификация
 */
enum class ToolType(val displayName: String) {
    // Drawing Tools
    PEN("Перо"),
    PENCIL("Карандаш"),
    MARKER("Маркер"),
    BRUSH("Кисть"),
    AIR_BRUSH("Аэрозольная кисть"),
    SPRAY("Распылитель"),
    WATER_BRUSH("Водная кисть"),
    INK_BRUSH("Чернильная кисть"),
    PIXEL_BRUSH("Пиксельная кисть"),
    CALLIGRAPHY("Каллиграфия"),
    HIGHLIGHTER("Маркер выделения"),

    // Erasers
    HARD_ERASER("Жёсткий ластик"),
    SOFT_ERASER("Мягкий ластик"),
    PARTIAL_ERASER("Частичный ластик"),
    OBJECT_ERASER("Ластик объектов"),

    // Shapes
    LINE("Линия"),
    ARROW("Стрелка"),
    RECTANGLE("Прямоугольник"),
    ROUNDED_RECTANGLE("Скруглённый прямоугольник"),
    ELLIPSE("Эллипс"),
    CIRCLE("Круг"),
    POLYGON("Многоугольник"),
    STAR("Звезда"),
    TRIANGLE("Треугольник"),

    // Curves
    QUADRATIC_BEZIER("Квадратичная кривая"),
    CUBIC_BEZIER("Кубическая кривая"),

    // Fill
    BUCKET_FILL("Заливка"),
    GRADIENT_FILL("Градиент"),
    PATTERN_FILL("Узор"),

    // Selection
    RECT_SELECTION("Прямоугольное выделение"),
    LASSO("Лассо"),
    MAGIC_WAND("Волшебная палочка"),

    // Transform
    MOVE("Перемещение"),
    SCALE("Масштаб"),
    ROTATE("Поворот"),
    SKEW("Скос"),
    MIRROR("Зеркало"),
    FLIP("Отражение"),

    // Text
    TEXT("Текст"),
    RICH_TEXT("Форматированный текст"),

    // Stamp
    STAMP("Штамп"),
    BITMAP_STAMP("Растровый штамп"),

    // Eyedropper
    EYEDROPPER("Пипетка")
}

/**
 * Категории инструментов для группировки в UI
 */
enum class ToolCategory {
    DRAWING,
    ERASER,
    SHAPE,
    CURVE,
    FILL,
    SELECTION,
    TRANSFORM,
    TEXT,
    STAMP,
    OTHER
}

/**
 * Расширения для ToolType
 */
val ToolType.category: ToolCategory
    get() = when (this) {
        ToolType.PEN, ToolType.PENCIL, ToolType.MARKER, ToolType.BRUSH, ToolType.AIR_BRUSH, ToolType.SPRAY, ToolType.WATER_BRUSH, ToolType.INK_BRUSH, ToolType.PIXEL_BRUSH, ToolType.CALLIGRAPHY, ToolType.HIGHLIGHTER -> ToolCategory.DRAWING
        ToolType.HARD_ERASER, ToolType.SOFT_ERASER, ToolType.PARTIAL_ERASER, ToolType.OBJECT_ERASER -> ToolCategory.ERASER
        ToolType.LINE, ToolType.ARROW, ToolType.RECTANGLE, ToolType.ROUNDED_RECTANGLE, ToolType.ELLIPSE, ToolType.CIRCLE, ToolType.POLYGON, ToolType.STAR, ToolType.TRIANGLE -> ToolCategory.SHAPE
        ToolType.QUADRATIC_BEZIER, ToolType.CUBIC_BEZIER -> ToolCategory.CURVE
        ToolType.BUCKET_FILL, ToolType.GRADIENT_FILL, ToolType.PATTERN_FILL -> ToolCategory.FILL
        ToolType.RECT_SELECTION, ToolType.LASSO, ToolType.MAGIC_WAND -> ToolCategory.SELECTION
        ToolType.MOVE, ToolType.SCALE, ToolType.ROTATE, ToolType.SKEW, ToolType.MIRROR, ToolType.FLIP -> ToolCategory.TRANSFORM
        ToolType.TEXT, ToolType.RICH_TEXT -> ToolCategory.TEXT
        ToolType.STAMP, ToolType.BITMAP_STAMP -> ToolCategory.STAMP
        ToolType.EYEDROPPER -> ToolCategory.OTHER
    }