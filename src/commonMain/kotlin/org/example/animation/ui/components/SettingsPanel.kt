package org.example.animation.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
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
                // Header
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
                    // Sidebar
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
                    
                    // Content
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
                
                // Footer
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
        Spacer(Modifier.height(6.dp.scaled()))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp.scaled())) {
            ThemeCircleBtn(EditorStrings.observeString("theme.dark"), EditorColors.accent, theme == ThemeType.DARK) { onTheme(ThemeType.DARK) }
            ThemeCircleBtn(EditorStrings.observeString("theme.light"), Color.White, theme == ThemeType.LIGHT) { onTheme(ThemeType.LIGHT) }
            ThemeCircleBtn(EditorStrings.observeString("theme.grey"), Color.Gray, theme == ThemeType.GREY) { onTheme(ThemeType.GREY) }
            ThemeCircleBtn(EditorStrings.observeString("theme.glass"), EditorColors.accentGreen, theme == ThemeType.GLASS) { onTheme(ThemeType.GLASS) }
        }

        Spacer(Modifier.height(20.dp.scaled()))
        Text(EditorStrings.observeString("settings.language"), style = EditorTypography.caption())
        Spacer(Modifier.height(6.dp.scaled()))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp.scaled())) {
            LanguageBtn("RU", EditorStrings.getCurrentCode() == "ru") { EditorStrings.setLanguage("ru") }
            LanguageBtn("EN", EditorStrings.getCurrentCode() == "en") { EditorStrings.setLanguage("en") }
        }
        
        Spacer(Modifier.height(20.dp.scaled()))
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
private fun ThemeCircleBtn(label: String, color: Color, isSelected: Boolean, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onClick() }.padding(4.dp.scaled())) {
        Box(
            modifier = Modifier
                .size(24.dp.scaled())
                .clip(RoundedCornerShape(12.dp.scaled()))
                .background(color)
                .border(if (isSelected) 2.dp.scaled() else 0.dp, Color.White, RoundedCornerShape(12.dp.scaled()))
        )
        Text(label.uppercase(), style = EditorTypography.toolText(), color = if (isSelected) EditorColors.accent else EditorColors.textSecondary, maxLines = 1)
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
    Column {
        Text(EditorStrings.observeString("settings.performance"), style = EditorTypography.body(), fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp.scaled()))
        Text(EditorStrings.observeString("perf.ghostFrames"), style = EditorTypography.body())
        Text(EditorStrings.observeString("anim.ghostDesc"), style = EditorTypography.caption(), color = EditorColors.textMuted)
        
        Spacer(Modifier.height(16.dp.scaled()))
        
        Text("${EditorStrings.observeString("anim.ghostBefore")} $before", style = EditorTypography.caption())
        Slider(value = before.toFloat(), onValueChange = { engine.setGhostFramesFramesBefore(it.toInt()) }, valueRange = 0f..10f, steps = 9)
        
        Text("${EditorStrings.observeString("anim.ghostAfter")} $after", style = EditorTypography.caption())
        Slider(value = after.toFloat(), onValueChange = { engine.setGhostFramesFramesAfter(it.toInt()) }, valueRange = 0f..10f, steps = 9)
    }
}

@Composable
private fun AboutSettingsContent(project: AnimationProject) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .size(40.dp.scaled())
                .background(EditorColors.divider, EditorShapes.medium), 
            contentAlignment = Alignment.Center
        ) {
            Icon(EditorIcons.iconAppIcon, null, tint = EditorColors.accent, modifier = Modifier.size(24.dp.scaled()))
        }
        Spacer(Modifier.height(12.dp.scaled()))
        Text("MaryMe Animator", style = EditorTypography.body(), fontWeight = FontWeight.Bold)
        Text("${EditorStrings.observeString("about.version")} $APP_VERSION", style = EditorTypography.caption(), color = EditorColors.accentGreen)
        Spacer(Modifier.height(8.dp.scaled()))
        Text(EditorStrings.observeString("about.desc"), style = EditorTypography.caption(), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

@Composable
private fun LanguageBtn(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.height(26.dp.scaled()).width(50.dp.scaled()).clickable { onClick() },
        color = if (isSelected) EditorColors.accent.copy(alpha = 0.2f) else EditorColors.divider,
        shape = EditorShapes.small,
        border = BorderStroke(1.dp.scaled(), if (isSelected) EditorColors.accent else EditorColors.divider)
    ) {
        Box(contentAlignment = Alignment.Center) { 
            Text(text, style = EditorTypography.toolText(), color = if (isSelected) EditorColors.accent else EditorColors.textPrimary) 
        }
    }
}
