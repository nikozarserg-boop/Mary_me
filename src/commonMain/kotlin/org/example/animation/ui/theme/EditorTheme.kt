package org.example.animation.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Цветовая палитра в стиле Photoshop/Krita (тёмная тема)
object EditorColors {
    val darkBackground = Color(0xFF1E1E1E)
    val darkSurface = Color(0xFF2D2D2D)
    val darkSurfaceVariant = Color(0xFF3C3C3C)
    val darkSurfaceLight = Color(0xFF4A4A4A)
    val panelBackground = Color(0xFF252526)
    val panelHeader = Color(0xFF2D2D2D)
    val canvasBackground = Color(0xFF404040)
    val accentBlue = Color(0xFF409EFF)
    val accentGreen = Color(0xFF67C23A)
    val accentRed = Color(0xFFFF6B6B)
    val accentOrange = Color(0xFFE6A23C)
    val accentPurple = Color(0xFFB37FEB)
    val textPrimary = Color(0xFFE0E0E0)
    val textSecondary = Color(0xFF9E9E9E)
    val textMuted = Color(0xFF6E6E6E)
    val dividerColor = Color(0xFF3A3A3A)
    val selectionColor = Color(0xFF264F78)
    val hoverColor = Color(0xFF3A3A3A)
    val buttonColor = Color(0xFF3A3A3A)
    val buttonHoverColor = Color(0xFF4A4A4A)
    val activeTabColor = Color(0xFF37373D)
    val timelineBackground = Color(0xFF1A1A1A)
    val timelineCell = Color(0xFF2D2D2D)
    val timelineCellActive = Color(0xFF1E3A5F)
    val timelineCellHasContent = Color(0xFF409EFF)
    val onionSkinColor = Color(0x440000FF)

    // Цвета для инструментов
    val toolActive = accentBlue
    val toolInactive = textSecondary

    // Специфические цвета для канваса
    val checkerLight = Color(0xFFCCCCCC)
    val checkerDark = Color(0xFF999999)
    val canvasBorder = Color(0xFF555555)
}

// Шейпы
object EditorShapes {
    val smallRounded = RoundedCornerShape(4.dp)
    val mediumRounded = RoundedCornerShape(6.dp)
    val largeRounded = RoundedCornerShape(8.dp)
    val toolBarRounded = RoundedCornerShape(8.dp)
    val panelRounded = RoundedCornerShape(8.dp)
    val buttonRounded = RoundedCornerShape(4.dp)
}

// Текст
object EditorTypography {
    val toolText = TextStyle(
        fontSize = 11.sp,
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        color = EditorColors.textPrimary
    )
    val panelTitle = TextStyle(
        fontSize = 11.sp,
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        color = EditorColors.textSecondary,
        letterSpacing = 0.5.sp
    )
    val frameNumber = TextStyle(
        fontSize = 10.sp,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Normal,
        color = EditorColors.textSecondary
    )
    val layerName = TextStyle(
        fontSize = 12.sp,
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        color = EditorColors.textPrimary
    )
    val statusBar = TextStyle(
        fontSize = 11.sp,
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        color = EditorColors.textSecondary
    )
}

/**
 * Основная тёмная тема редактора
 */
@Composable
fun EditorTheme(content: @Composable () -> Unit) {
    val colors = darkColors(
        primary = EditorColors.accentBlue,
        primaryVariant = EditorColors.accentBlue,
        secondary = EditorColors.accentGreen,
        background = EditorColors.darkBackground,
        surface = EditorColors.darkSurface,
        error = EditorColors.accentRed,
        onPrimary = Color.White,
        onSecondary = Color.White,
        onBackground = EditorColors.textPrimary,
        onSurface = EditorColors.textPrimary,
        onError = Color.White
    )

    MaterialTheme(
        colors = colors,
        typography = Typography(
            body1 = TextStyle(fontSize = 14.sp, color = EditorColors.textPrimary),
            body2 = TextStyle(fontSize = 12.sp, color = EditorColors.textSecondary),
            caption = TextStyle(fontSize = 11.sp, color = EditorColors.textMuted)
        ),
        shapes = MaterialTheme.shapes.copy(
            small = EditorShapes.smallRounded,
            medium = EditorShapes.mediumRounded,
            large = EditorShapes.largeRounded
        ),
        content = content
    )
}