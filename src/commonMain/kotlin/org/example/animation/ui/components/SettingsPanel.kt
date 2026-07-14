package org.example.animation.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.animation.APP_VERSION
import org.example.animation.engine.AnimationEngine
import org.example.animation.localization.EditorStrings
import org.example.animation.localization.LangData
import org.example.animation.model.AnimationProject
import org.example.animation.ui.theme.*
import kotlin.math.roundToInt

@Composable
fun SettingsDialog(
    engine: AnimationEngine,
    uiScale: Float,
    currentTheme: ThemeType,
    onUiScaleChange: (Float) -> Unit,
    onThemeChange: (ThemeType) -> Unit,
    onClose: () -> Unit
) {
    val project by engine.project.collectAsState()
    var selectedCategory by remember { mutableStateOf(0) }
    
    var canvasWidth by remember(project.canvasWidth) { mutableStateOf(project.canvasWidth.toString()) }
    var canvasHeight by remember(project.canvasHeight) { mutableStateOf(project.canvasHeight.toString()) }
    var fps by remember(project.fps) { mutableStateOf(project.fps.toString()) }
    var dpi by remember(project.dpi) { mutableStateOf(project.dpi.toString()) }

    var offset by remember { mutableStateOf(IntOffset(0, 0)) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable { onClose() }, 
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .offset { offset }
                .width(600.dp.scaled())
                .height(450.dp.scaled())
                .clickable(enabled = false) {},
            color = if (currentTheme == ThemeType.GLASS) EditorColors.glassBackground else EditorColors.surface,
            shape = EditorShapes.large,
            elevation = if (currentTheme == ThemeType.GLASS) 4.dp.scaled() else 16.dp.scaled(),
            border = BorderStroke(
                if (currentTheme == ThemeType.GLASS) 1.dp.scaled() else 1.dp.scaled(),
                if (currentTheme == ThemeType.GLASS) EditorColors.glassBorder else EditorColors.divider
            )
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Заголовок
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(EditorColors.panelHeader)
                        .pointerHoverIcon(PointerIcon.Hand)
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                offset = IntOffset(
                                    offset.x + dragAmount.x.roundToInt(),
                                    offset.y + dragAmount.y.roundToInt()
                                )
                            }
                        }
                        .padding(horizontal = 12.dp.scaled(), vertical = 8.dp.scaled()),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(EditorIcons.iconSettings, null, tint = EditorColors.accent, modifier = Modifier.size(16.dp.scaled()))
                    Spacer(Modifier.width(10.dp.scaled()))
                    Text(
                        EditorStrings.observeString("settings.title"), 
                        style = EditorTypography.body().copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = onClose, modifier = Modifier.size(20.dp.scaled())) {
                        Icon(EditorIcons.iconClose, null, tint = EditorColors.textSecondary, modifier = Modifier.size(16.dp.scaled()))
                    }
                }
                
                Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    // Боковая панель
                    Column(
                        modifier = Modifier
                            .width(160.dp.scaled())
                            .fillMaxHeight()
                            .background(EditorColors.panelBackground)
                            .padding(vertical = 4.dp.scaled())
                    ) {
                        val categories = listOf(
                            "settings.canvas",
                            "settings.interface",
                            "settings.performance",
                            "settings.about"
                        )
                        
                        categories.forEachIndexed { index, key ->
                            CategoryItem(
                                title = EditorStrings.observeString(key), 
                                isSelected = selectedCategory == index
                            ) { selectedCategory = index }
                        }
                    }
                    
                    Divider(modifier = Modifier.width(1.dp.scaled()).fillMaxHeight(), color = EditorColors.divider)
                    
                    // Содержимое
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(16.dp.scaled())
                            .verticalScroll(rememberScrollState())
                    ) {
                        when (selectedCategory) {
                            0 -> CanvasSettingsContent(
                                canvasWidth, canvasHeight, fps, dpi,
                                { canvasWidth = it }, { canvasHeight = it }, { fps = it }, { dpi = it }
                            )
                            1 -> InterfaceSettingsContent(uiScale, currentTheme, onUiScaleChange, onThemeChange)
                            2 -> PerformanceSettingsContent(engine)
                            3 -> AboutSettingsContent(project)
                        }
                    }
                }
                
                Divider(color = EditorColors.divider)
                
                // Нижняя панель
                DialogButtonRow(
                    cancelText = EditorStrings.observeString("cancel"),
                    confirmText = EditorStrings.observeString("apply"),
                    onCancel = onClose,
                    onConfirm = {
                        val newProject = project.copy()
                        newProject.canvasWidth = canvasWidth.toIntOrNull()?.coerceIn(1, 10000) ?: project.canvasWidth
                        newProject.canvasHeight = canvasHeight.toIntOrNull()?.coerceIn(1, 10000) ?: project.canvasHeight
                        newProject.fps = fps.toIntOrNull()?.coerceIn(1, 120) ?: project.fps
                        newProject.dpi = dpi.toIntOrNull()?.coerceIn(1, 1200) ?: project.dpi
                        engine.setProject(newProject)
                        onClose()
                    }
                )
            }
        }
    }
}

@Composable
private fun CategoryItem(title: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp.scaled(), vertical = 1.dp.scaled())
            .height(32.dp.scaled())
            .clip(EditorShapes.medium)
            .background(if (isSelected) EditorColors.selection.copy(alpha = 0.5f) else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 8.dp.scaled()),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            title, 
            style = EditorTypography.body(),
            color = if (isSelected) Color.White else EditorColors.textSecondary,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
private fun CanvasSettingsContent(w: String, h: String, f: String, d: String, onW: (String) -> Unit, onH: (String) -> Unit, onF: (String) -> Unit, onD: (String) -> Unit) {
    Column {
        Text(EditorStrings.observeString("settings.canvas"), style = EditorTypography.body(), fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp.scaled()))
        
        SettingInputItem(EditorStrings.observeString("canvas.width"), w, onW, "px")
        SettingInputItem(EditorStrings.observeString("canvas.height"), h, onH, "px")
        SettingInputItem(EditorStrings.observeString("canvas.fps"), f, onF, "fps")
        SettingInputItem("DPI", d, onD, "dpi")
    }
}

@Composable
private fun InterfaceSettingsContent(scale: Float, theme: ThemeType, onScale: (Float) -> Unit, onTheme: (ThemeType) -> Unit) {
    Column {
        Text(EditorStrings.observeString("settings.interface"), style = EditorTypography.body(), fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp.scaled()))

        Text(EditorStrings.observeString("interface.theme"), style = EditorTypography.caption())
        Spacer(Modifier.height(8.dp.scaled()))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp.scaled())) {
            ThemeCircleBtn(EditorStrings.observeString("theme.dark"), EditorColors.accent, theme == ThemeType.DARK) { onTheme(ThemeType.DARK) }
            ThemeCircleBtn(EditorStrings.observeString("theme.light"), Color.White, theme == ThemeType.LIGHT) { onTheme(ThemeType.LIGHT) }
            ThemeCircleBtn(EditorStrings.observeString("theme.grey"), Color.Gray, theme == ThemeType.GREY) { onTheme(ThemeType.GREY) }
            ThemeCircleBtn(EditorStrings.observeString("theme.glass"), EditorColors.accentGreen, theme == ThemeType.GLASS) { onTheme(ThemeType.GLASS) }
        }

        Spacer(Modifier.height(24.dp.scaled()))
        Text(EditorStrings.observeString("settings.language"), style = EditorTypography.caption())
        Spacer(Modifier.height(8.dp.scaled()))
        
        val languages = EditorStrings.getAvailableLanguages()
        val currentCode = EditorStrings.getCurrentCode()
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp.scaled())
        ) {
            languages.forEach { lang ->
                LanguageItem(
                    lang = lang,
                    isSelected = lang.code == currentCode,
                    onClick = { EditorStrings.setLanguage(lang.code) }
                )
            }
        }
        
        Spacer(Modifier.height(24.dp.scaled()))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(EditorStrings.observeString("interface.scale"), style = EditorTypography.body(), modifier = Modifier.weight(1f))
            Text("${(scale * 100).toInt()}%", style = EditorTypography.mono(), color = EditorColors.accent)
        }
        Slider(
            value = scale, 
            onValueChange = onScale, 
            valueRange = 0.5f..2.0f, 
            colors = SliderDefaults.colors(thumbColor = EditorColors.accent, activeTrackColor = EditorColors.accent)
        )
    }
}

@Composable
private fun LanguageItem(lang: LangData, isSelected: Boolean, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    
    Surface(
        modifier = Modifier
            .height(36.dp.scaled())
            .widthIn(min = 100.dp.scaled())
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .hoverable(interactionSource),
        color = if (isSelected) EditorColors.accent.copy(alpha = 0.15f) else if (isHovered) EditorColors.hover else EditorColors.surfaceVariant,
        shape = RoundedCornerShape(8.dp.scaled()),
        border = BorderStroke(
            width = 1.dp.scaled(),
            color = if (isSelected) EditorColors.accent else if (isHovered) EditorColors.accent.copy(alpha = 0.3f) else EditorColors.divider
        )
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 12.dp.scaled())) {
            Text(
                text = lang.nameNative,
                style = EditorTypography.body().copy(fontSize = 13.sp.scaled()),
                color = if (isSelected) EditorColors.accent else EditorColors.textPrimary,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
            )
        }
    }
}

@Composable
private fun ThemeCircleBtn(label: String, color: Color, isSelected: Boolean, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally, 
        modifier = Modifier
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .hoverable(interactionSource)
            .padding(4.dp.scaled())
    ) {
        Box(
            modifier = Modifier
                .size(28.dp.scaled())
                .clip(RoundedCornerShape(14.dp.scaled()))
                .background(color)
                .border(
                    width = if (isSelected) 2.dp.scaled() else if (isHovered) 1.dp.scaled() else 0.dp, 
                    color = if (isSelected) Color.White else if (isHovered) Color.White.copy(alpha = 0.5f) else Color.Transparent, 
                    shape = RoundedCornerShape(14.dp.scaled())
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Icon(
                    EditorIcons.iconCheck, 
                    null, 
                    tint = if (color == Color.White) Color.Black else Color.White, 
                    modifier = Modifier.size(16.dp.scaled())
                )
            }
        }
        Spacer(Modifier.height(4.dp.scaled()))
        Text(
            label, 
            style = EditorTypography.toolText(), 
            color = if (isSelected) EditorColors.accent else EditorColors.textSecondary, 
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SettingInputItem(label: String, value: String, onValueChange: (String) -> Unit, suffix: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp.scaled())) {
        Text(label, style = EditorTypography.body(), color = EditorColors.textSecondary, modifier = Modifier.width(100.dp.scaled()))
        OutlinedTextField(
            value = value, 
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f).height(32.dp.scaled()),
            textStyle = EditorTypography.mono(),
            singleLine = true,
            trailingIcon = { 
                Text(suffix, style = EditorTypography.caption(), modifier = Modifier.padding(end = 4.dp.scaled())) 
            },
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = EditorColors.accent, 
                unfocusedBorderColor = EditorColors.divider, 
                backgroundColor = EditorColors.background
            )
        )
    }
}

@Composable
private fun PerformanceSettingsContent(engine: AnimationEngine) {
    val before by engine.ghostFramesBefore.collectAsState()
    val after by engine.ghostFramesAfter.collectAsState()
    val ghostColor by engine.ghostFramesColor.collectAsState()

    Column {
        Text(EditorStrings.observeString("settings.performance"), style = EditorTypography.body(), fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp.scaled()))
        
        Text(EditorStrings.observeString("perf.ghostFrames"), style = EditorTypography.body())
        Text(EditorStrings.observeString("anim.ghostDesc"), style = EditorTypography.caption(), color = EditorColors.textMuted)
        
        Spacer(Modifier.height(16.dp.scaled()))
        
        Text("${EditorStrings.observeString("anim.ghostBefore")} $before", style = EditorTypography.caption())
        Slider(value = before.toFloat(), onValueChange = { engine.setGhostFramesFramesBefore(it.toInt()) }, valueRange = 0f..10f, steps = 9)
        
        Text("${EditorStrings.observeString("anim.ghostAfter")} $after", style = EditorTypography.caption())
        Slider(value = after.toFloat(), onValueChange = { engine.setGhostFramesFramesAfter(it.toInt()) }, valueRange = 0f..10f, steps = 9)

        Spacer(Modifier.height(16.dp.scaled()))
        Text("Цвет призрачного кадра", style = EditorTypography.caption())
        Spacer(Modifier.height(8.dp.scaled()))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp.scaled())) {
            val colors = listOf(
                0xFF0099FFuL, // Blue (Default)
                0xFFF44336uL, // Red
                0xFF4CAF50uL, // Green
                0xFFFF9800uL, // Orange
                0xFF9E9E9EuL, // Grey
                0xFF000000uL  // Black
            )
            colors.forEach { colorU ->
                GhostColorItem(
                    color = Color(colorU.toLong()),
                    isSelected = ghostColor == colorU,
                    onClick = { engine.setGhostFramesColor(colorU) }
                )
            }
        }
    }
}

@Composable
private fun GhostColorItem(color: Color, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(24.dp.scaled())
            .clip(CircleShape)
            .background(color)
            .border(
                width = if (isSelected) 2.dp.scaled() else 1.dp.scaled(),
                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.2f),
                shape = CircleShape
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(EditorIcons.iconCheck, null, tint = if (color == Color.White) Color.Black else Color.White, modifier = Modifier.size(14.dp.scaled()))
        }
    }
}

@Composable
private fun AboutSettingsContent(project: AnimationProject) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .size(48.dp.scaled())
                .background(EditorColors.divider, EditorShapes.medium), 
            contentAlignment = Alignment.Center
        ) {
            Icon(EditorIcons.iconAppIcon, null, tint = EditorColors.accent, modifier = Modifier.size(28.dp.scaled()))
        }
        Spacer(Modifier.height(12.dp.scaled()))
        Text("MaryMe Animator", style = EditorTypography.body().copy(fontSize = 16.sp.scaled()), fontWeight = FontWeight.Bold)
        Text("${EditorStrings.observeString("about.version")} $APP_VERSION", style = EditorTypography.caption(), color = EditorColors.accentGreen)
        Spacer(Modifier.height(16.dp.scaled()))
        Text(
            EditorStrings.observeString("about.desc"), 
            style = EditorTypography.body().copy(fontSize = 13.sp.scaled(), color = EditorColors.textSecondary), 
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp.scaled())
        )
    }
}
