package org.example.animation.model

/**
 * Типы инструментов рисования - полная классификация
 */
enum class ToolType(val localizationKey: String) {
    // Инструменты рисования
    PEN("tool.pen"),
    PENCIL("tool.pencil"),
    MARKER("tool.marker"),
    BRUSH("tool.brush"),
    AIR_BRUSH("tool.airbrush"),
    SPRAY("tool.spray"),
    WATER_BRUSH("tool.waterbrush"),
    INK_BRUSH("tool.inkbrush"),
    PIXEL_BRUSH("tool.pixelbrush"),
    CALLIGRAPHY("tool.calligraphy"),
    HIGHLIGHTER("tool.highlighter"),

    // Ластики
    HARD_ERASER("tool.hardEraser"),
    SOFT_ERASER("tool.softEraser"),
    PARTIAL_ERASER("tool.partialEraser"),
    OBJECT_ERASER("tool.objectEraser"),

    // Фигуры
    LINE("tool.line"),
    ARROW("tool.arrow"),
    RECTANGLE("tool.rectangle"),
    ROUNDED_RECTANGLE("tool.roundedRectangle"),
    ELLIPSE("tool.ellipse"),
    CIRCLE("tool.circle"),
    POLYGON("tool.polygon"),
    STAR("tool.star"),
    TRIANGLE("tool.triangle"),

    // Кривые
    QUADRATIC_BEZIER("tool.quadraticBezier"),
    CUBIC_BEZIER("tool.cubicBezier"),

    // Заливка
    BUCKET_FILL("tool.bucketFill"),
    GRADIENT_FILL("tool.gradientFill"),
    PATTERN_FILL("tool.patternFill"),

    // Выделение
    RECT_SELECTION("tool.rectSelection"),
    LASSO("tool.lasso"),
    MAGIC_WAND("tool.magicWand"),

    // Трансформация
    MOVE("tool.move"),
    SCALE("tool.scale"),
    ROTATE("tool.rotate"),
    SKEW("tool.skew"),
    MIRROR("tool.mirror"),
    FLIP("tool.flip"),

    // Текст
    TEXT("tool.text"),
    RICH_TEXT("tool.richText"),

    // Штамп
    STAMP("tool.stamp"),
    BITMAP_STAMP("tool.bitmapStamp"),

    // Пипетка
    EYEDROPPER("tool.eyedropper")
}

/**
 * Категории инструментов для группировки в UI
 */
enum class ToolCategory(val localizationKey: String) {
    DRAWING("category.drawing"),
    ERASER("category.eraser"),
    SHAPE("category.shape"),
    CURVE("category.curve"),
    FILL("category.fill"),
    SELECTION("category.selection"),
    TRANSFORM("category.transform"),
    TEXT("category.text"),
    STAMP("category.stamp"),
    OTHER("category.other")
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