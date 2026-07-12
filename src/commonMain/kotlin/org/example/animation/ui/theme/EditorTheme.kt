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
    // Reactive colors via states
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
    
    val glassBackground = Color(0xAA121212) 
    val glassBorder = Color(0x44FFFFFF)
    
    var canvasBackground by mutableStateOf(Color(0xFF181818))
    val canvasBorder = Color(0xFF444444)

    // Aliases and extra UI colors used across panels
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
                // Professional Light Theme: "Modern Paper"
                background = Color(0xFFF0F0F0)
                surface = Color(0xFFFFFFFF)
                surfaceVariant = Color(0xFFE5E5E5)
                panelBackground = Color(0xFFFAFAFA)
                panelHeader = Color(0xFFE0E0E0)
                accent = Color(0xFF0066CC)
                textPrimary = Color(0xFF222222)
                textSecondary = Color(0xFF444444)
                textMuted = Color(0xFF999999)
                divider = Color(0xFFDCDCDC)
                selection = Color(0xFFB3D7FF)
                hover = Color(0xFFF0F0F0)
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
                background = Color(0xFF05050A); surface = Color(0xAA1A1A1A); surfaceVariant = Color(0x662D2D2D)
                panelBackground = Color(0x441A1A1A); panelHeader = Color(0x88252525); accent = Color(0xAA007ACC)
                textPrimary = Color(0xFFFFFFFF); textSecondary = Color(0xFFCCCCCC); textMuted = Color(0xFF888888)
                divider = Color(0x22FFFFFF); selection = Color(0xAA007ACC); hover = Color(0x33FFFFFF)
                canvasBackground = Color(0xFF0A0A0F)
            }
        }
    }
}

// Global UI Scaling System
val LocalUiScale = compositionLocalOf { 1.0f }

// Non-composable global scale so scaled() can be used inside non-composable lambdas
private var _uiScale = 1.0f
var currentUiScale: Float
    get() = _uiScale
    set(value) { _uiScale = value }

fun Dp.scaled(): Dp = this * _uiScale

fun TextUnit.scaled(): TextUnit = this * _uiScale

object EditorShapes {
    val small = RoundedCornerShape(2.dp)
    val medium = RoundedCornerShape(4.dp)
    val large = RoundedCornerShape(8.dp)
}

object EditorTypography {
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

// Base sizes for scaling reference
object UiDimensions {
    val ToolBarWidth = 32.dp
    val IconButtonSize = 24.dp
    val IconSize = 16.dp
    val PaddingSmall = 4.dp
    val PaddingMedium = 8.dp
    val PaddingLarge = 16.dp
    val TopBarHeight = 36.dp
    val StatusBarHeight = 22.dp
    
    // Panel constraints (Base values, will be scaled in UI)
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
    SideEffect { EditorColors.applyTheme(themeType); _uiScale = uiScale }
    
    val colors = if (themeType == ThemeType.LIGHT) {
        lightColors(
            primary = EditorColors.accent,
            background = EditorColors.background,
            surface = EditorColors.surface,
            onPrimary = Color.White
        )
    } else {
        darkColors(
            primary = EditorColors.accent,
            background = EditorColors.background,
            surface = EditorColors.surface,
            onPrimary = Color.White
        )
    }
    
    CompositionLocalProvider(
        LocalThemeType provides themeType,
        LocalUiScale provides uiScale
    ) {
        MaterialTheme(
            colors = colors, 
            shapes = MaterialTheme.shapes.copy(
                small = EditorShapes.small, 
                medium = EditorShapes.medium, 
                large = EditorShapes.large
            ), 
            content = content
        )
    }
}
