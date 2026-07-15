package org.example.animation.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class ThemeType {
    DARK, LIGHT, GREY, GLASS
}

object EditorColors {
    // Реактивные цвета через состояния (State)
    private val _background = mutableStateOf(Color(0xFF121212))
    var background: Color by _background

    private val _surface = mutableStateOf(Color(0xFF1E1E1E))
    var surface: Color by _surface

    private val _surfaceVariant = mutableStateOf(Color(0xFF2D2D2D))
    var surfaceVariant: Color by _surfaceVariant

    private val _panelBackground = mutableStateOf(Color(0xFF1A1A1A))
    var panelBackground: Color by _panelBackground

    private val _panelHeader = mutableStateOf(Color(0xFF252525))
    var panelHeader: Color by _panelHeader

    private val _textPrimary = mutableStateOf(Color(0xFFE0E0E0))
    var textPrimary: Color by _textPrimary

    private val _textSecondary = mutableStateOf(Color(0xFFAAAAAA))
    var textSecondary: Color by _textSecondary

    private val _textMuted = mutableStateOf(Color(0xFF666666))
    var textMuted: Color by _textMuted

    private val _divider = mutableStateOf(Color(0xFF2D2D2D))
    var divider: Color by _divider

    private val _selection = mutableStateOf(Color(0xFF005A9E))
    var selection: Color by _selection

    private val _hover = mutableStateOf(Color(0xFF2A2A2A))
    var hover: Color by _hover

    private val _accent = mutableStateOf(Color(0xFF007ACC))
    var accent: Color by _accent

    val accentGreen = Color(0xFF4EC9B0)
    val accentRed = Color(0xFFF44336)
    val accentOrange = Color(0xFFCE9178)
    
    // Более тёмный, мягкий и приятный голубой для стеклянной темы
    val glassBackground = Color(0x99121C2A) // Глубокий тёмно-голубой полупрозрачный фон стекла
    val glassBorder = Color(0x556E9FCB) // Мягкая приглушённо-голубая стеклянная граница
    // Цвет линзового блика (эффект «рыбьего глаза» стекла)
    val glassSheen = Color(0x22Bcd8F5)

    var canvasBackground by mutableStateOf(Color(0xFF181818))
    val canvasBorder = Color(0xFF444444)

    // Псевдонимы (Aliases) для обратной совместимости
    val dividerColor: Color get() = _divider.value
    val accentBlue: Color get() = _accent.value
    val darkSurface: Color get() = _surface.value
    val darkSurfaceVariant: Color get() = _surfaceVariant.value
    val darkSurfaceLight: Color get() = _panelHeader.value
    val selectionColor: Color get() = _selection.value
    val buttonColor: Color get() = _surfaceVariant.value
    val buttonHoverColor: Color get() = _hover.value
    val timelineBackground: Color get() = _panelBackground.value
    val timelineCell: Color get() = _surface.value
    val timelineCellActive: Color get() = _selection.value
    val timelineCellHasContent: Color get() = _accent.value

    fun applyTheme(type: ThemeType) {
        when (type) {
            ThemeType.DARK -> {
                background = Color(0xFF121212); surface = Color(0xFF1E1E1E); surfaceVariant = Color(0xFF2D2D2D)
                panelBackground = Color(0xFF1A1A1A); panelHeader = Color(0xFF252525); accent = Color(0xFF007ACC)
                textPrimary = Color(0xFFE0E0E0); textSecondary = Color(0xFFAAAAAA); textMuted = Color(0xFF666666)
                divider = Color(0xFF2D2D2D); selection = Color(0xFF005A9E); hover = Color(0xFF2A2A2A)
                canvasBackground = Color(0xFF181818)
            }
            ThemeType.LIGHT -> {
                background = Color(0xFFF0F0F0); surface = Color(0xFFFFFFFF); surfaceVariant = Color(0xFFE5E5E5)
                panelBackground = Color(0xFFFAFAFA); panelHeader = Color(0xFFE0E0E0); accent = Color(0xFF0066CC)
                textPrimary = Color(0xFF222222); textSecondary = Color(0xFF444444); textMuted = Color(0xFF999999)
                divider = Color(0xFFDCDCDC); selection = Color(0xFFB3D7FF); hover = Color(0xFFF0F0F0)
                canvasBackground = Color(0xFFD8D8D8)
            }
            ThemeType.GREY -> {
                background = Color(0xFF333333); surface = Color(0xFF3C3C3C); surfaceVariant = Color(0xFF454545)
                panelBackground = Color(0xFF333333); panelHeader = Color(0xFF2D2D2D); accent = Color(0xFF5A5A5A)
                textPrimary = Color(0xFFF5F5F5); textSecondary = Color(0xFFBBBBBB); textMuted = Color(0xFF888888)
                divider = Color(0xFF2D2D2D); selection = Color(0xFF5A5A5A); hover = Color(0xFF444444)
                canvasBackground = Color(0xFF2D2D2D)
            }
            ThemeType.GLASS -> {
                // Тёмная мягкая голубая стеклянная тема: спокойные приятные оттенки везде
                background = Color(0xFF03080F)        // Очень тёмный сине-голубой фон
                surface = Color(0x9913202C)           // Тёмная голубая стеклянная поверхность
                surfaceVariant = Color(0x66182638)
                panelBackground = Color(0x40111928)   // Тёмный голубой полупрозрачный фон панелей
                panelHeader = Color(0x70182436)       // Тёмный голубой заголовок
                accent = Color(0xCC5E97C6)            // Мягкий приятный голубой акцент
                textPrimary = Color(0xFFDCE8F5)       // Светло-голубой основной текст
                textSecondary = Color(0xFFA9C2DB)     // Мягкий голубой вторичный текст
                textMuted = Color(0xFF6E88A8)         // Приглушённый голубой
                divider = Color(0x336E9FCB)           // Мягкая голубая разделительная линия
                selection = Color(0xCC4A82BE)         // Мягкое голубое выделение
                hover = Color(0x335E97C6)             // Мягкая голубая подсветка при наведении
                canvasBackground = Color(0xFF05101A)  // Тёмный голубоватый фон холста
            }
        }
    }
}

// Глобальная система масштабирования интерфейса
val LocalUiScale = compositionLocalOf { 1.0f }

// Глобальный масштаб для не-композабельных случаев
private val _uiScaleState = mutableStateOf(1.0f)
var currentUiScale: Float
    get() = _uiScaleState.value
    set(value) { _uiScaleState.value = value }

@Composable
fun Dp.scaled(): Dp = this * LocalUiScale.current

@Composable
fun TextUnit.scaled(): TextUnit = this * LocalUiScale.current

// Вспомогательная функция для не-композабельного контекста
fun Dp.scaledNonReactive(): Dp = this * _uiScaleState.value
fun TextUnit.scaledNonReactive(): TextUnit = this * _uiScaleState.value

object EditorShapes {
    val small @Composable get() = RoundedCornerShape(2.dp.scaled())
    val medium @Composable get() = RoundedCornerShape(4.dp.scaled())
    val large @Composable get() = RoundedCornerShape(8.dp.scaled())
}

object EditorTypography {
    @Composable
    fun h1() = TextStyle(fontSize = 24.sp.scaled(), fontWeight = FontWeight.Black, color = EditorColors.textPrimary)
    @Composable
    fun h2() = TextStyle(fontSize = 18.sp.scaled(), fontWeight = FontWeight.Bold, color = EditorColors.textPrimary)
    @Composable
    fun toolText() = TextStyle(fontSize = 9.sp.scaled(), fontWeight = FontWeight.Normal, color = EditorColors.textPrimary)
    @Composable
    fun panelTitle() = TextStyle(fontSize = 10.sp.scaled(), fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp.scaled(), color = EditorColors.textSecondary)
    @Composable
    fun caption() = TextStyle(fontSize = 9.sp.scaled(), fontWeight = FontWeight.Normal, color = EditorColors.textMuted)
    @Composable
    fun body() = TextStyle(fontSize = 11.sp.scaled(), fontWeight = FontWeight.Normal, color = EditorColors.textPrimary)
    @Composable
    fun mono() = TextStyle(fontSize = 10.sp.scaled(), fontFamily = FontFamily.Monospace, color = EditorColors.textPrimary)
    @Composable
    fun statusBar() = TextStyle(fontSize = 10.sp.scaled(), fontWeight = FontWeight.Normal, color = EditorColors.textSecondary)
    @Composable
    fun menu() = TextStyle(fontSize = 12.sp.scaled(), fontWeight = FontWeight.Normal, color = EditorColors.textPrimary)
    @Composable
    fun layerName() = TextStyle(fontSize = 11.sp.scaled(), fontWeight = FontWeight.Normal, color = EditorColors.textPrimary)
}

object UiDimensions {
    val ToolBarWidth = 32.dp
    val IconButtonSize = 24.dp
    val IconSize = 16.dp
    val PaddingSmall = 4.dp
    val PaddingMedium = 8.dp
    val PaddingLarge = 16.dp
    val TopBarHeight = 36.dp
    val StatusBarHeight = 22.dp
    
    val MinSidePanelWidth = 120.dp
    val MaxSidePanelWidth = 400.dp
    val MinTimelineHeight = 100.dp
    val MaxTimelineHeight = 500.dp
}

val LocalThemeType = compositionLocalOf { ThemeType.DARK }

@Composable
fun EditorTheme(
    themeType: ThemeType = ThemeType.DARK, 
    uiScale: Float = 1.0f,
    content: @Composable () -> Unit
) {
    SideEffect { 
        EditorColors.applyTheme(themeType)
        _uiScaleState.value = uiScale 
    }
    
    val colors = if (themeType == ThemeType.LIGHT) {
        lightColors(primary = EditorColors.accent, background = EditorColors.background, surface = EditorColors.surface, onPrimary = Color.White)
    } else {
        darkColors(primary = EditorColors.accent, background = EditorColors.background, surface = EditorColors.surface, onPrimary = Color.White)
    }
    
    CompositionLocalProvider(
        LocalThemeType provides themeType,
        LocalUiScale provides uiScale
    ) {
        MaterialTheme(
            colors = colors, 
            shapes = Shapes(
                small = RoundedCornerShape(2.dp.scaled()),
                medium = RoundedCornerShape(4.dp.scaled()),
                large = RoundedCornerShape(8.dp.scaled())
            ), 
            content = content
        )
    }
}
