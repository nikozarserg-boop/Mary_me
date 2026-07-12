package org.example.animation.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.animation.engine.AnimationEngine
import org.example.animation.localization.EditorStrings
import org.example.animation.localization.LangData
import org.example.animation.model.AnimationProject
import org.example.animation.ui.theme.EditorColors
import org.example.animation.ui.theme.EditorIcons

@Composable
fun SettingsDialog(
    engine: AnimationEngine,
    uiScale: Float,
    onUiScaleChange: (Float) -> Unit,
    onClose: () -> Unit
) {
    val project by engine.project.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    var canvasWidth by remember(project.canvasWidth) { mutableStateOf(project.canvasWidth.toString()) }
    var canvasHeight by remember(project.canvasHeight) { mutableStateOf(project.canvasHeight.toString()) }
    var fps by remember(project.fps) { mutableStateOf(project.fps.toString()) }
    var onionBefore by remember { mutableIntStateOf(engine.onionSkinFramesBefore.value) }
    var onionAfter by remember { mutableIntStateOf(engine.onionSkinFramesAfter.value) }
    var selectedLang by remember { mutableStateOf(EditorStrings.getCurrentCode()) }
    val langs = EditorStrings.getAvailableLanguages()

    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)), contentAlignment = Alignment.Center) {
        Surface(modifier = Modifier.width(520.dp).height(440.dp), color = EditorColors.darkSurface, shape = RoundedCornerShape(10.dp), elevation = 10.dp) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(modifier = Modifier.fillMaxWidth().background(EditorColors.panelHeader).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(EditorStrings["settings.title"], color = EditorColors.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.weight(1f))
                    Box(modifier = Modifier.size(26.dp).clip(RoundedCornerShape(6.dp)).clickable { onClose() }, contentAlignment = Alignment.Center) {
                        Icon(EditorIcons.iconClose, "Close", tint = EditorColors.textSecondary, modifier = Modifier.size(16.dp))
                    }
                }
                Divider(color = EditorColors.dividerColor, thickness = 1.dp)
                Row(modifier = Modifier.fillMaxWidth().background(EditorColors.panelBackground).padding(horizontal = 10.dp, vertical = 6.dp)) {
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = onClose) { Text("Отмена", color = EditorColors.textSecondary) }
                        Spacer(Modifier.weight(1f))
                        Text(EditorStrings["settings.title"], color = EditorColors.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = {
                            val current = engine.project.value
val newProject = AnimationProject(
                                name = current.name,
                                layers = current.layers,
                                canvasWidth = canvasWidth.toIntOrNull()?.coerceIn(1, 8192) ?: current.canvasWidth,
                                canvasHeight = canvasHeight.toIntOrNull()?.coerceIn(1, 8192) ?: current.canvasHeight,
                                fps = fps.toIntOrNull()?.coerceIn(1, 120) ?: current.fps,
                                backgroundColor = current.backgroundColor
                            )
                            engine.setProject(newProject)
                            engine.setOnionSkinFramesBefore(onionBefore.coerceIn(0, 10))
                            engine.setOnionSkinFramesAfter(onionAfter.coerceIn(0, 10))
                            EditorStrings.setLanguage(selectedLang)
                            onClose()
                        }) { Text(EditorStrings["apply"], color = EditorColors.accentBlue, fontWeight = FontWeight.SemiBold) }
                    }
                }
                Divider(color = EditorColors.dividerColor, thickness = 1.dp)
                Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
SettingsSection(title = EditorStrings["settings.canvas"]) {
                        CanvasSettings(
                            width = canvasWidth,
                            height = canvasHeight,
                            fps = fps,
                            onWidthChange = { canvasWidth = it },
                            onHeightChange = { canvasHeight = it },
                            onFpsChange = { fps = it }
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    SettingsSection(title = EditorStrings["settings.performance"]) {
                        PerformanceSettings(onionBefore, onionAfter, onBeforeChange = { onionBefore = it }, onAfterChange = { onionAfter = it })
                    }
                    Spacer(Modifier.height(12.dp))
                    SettingsSection(title = EditorStrings["settings.interface"]) {
                        Column {
                            InterfaceSettings()
                            Spacer(Modifier.height(10.dp))

                            Text(
                                "UI Scale",
                                color = EditorColors.textPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.height(6.dp))
                            Slider(
                                value = uiScale,
                                onValueChange = onUiScaleChange,
                                valueRange = 0.8f..1.5f,
                                modifier = Modifier.fillMaxWidth(),
                                colors = SliderDefaults.colors(
                                    thumbColor = EditorColors.accentBlue,
                                    activeTrackColor = EditorColors.accentBlue,
                                    inactiveTrackColor = EditorColors.darkSurfaceVariant
                                )
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "${(uiScale * 100).toInt()}%",
                                color = EditorColors.textSecondary,
                                fontSize = 11.sp
                            )

                            Spacer(Modifier.height(8.dp))
                            Text(EditorStrings["settings.language"], color = EditorColors.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(4.dp))
                            LanguageSettings(langs, selectedLang) { selectedLang = it }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    SettingsSection(title = EditorStrings["settings.about"]) {
                        AboutSection()
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Column {
        Text(title, color = EditorColors.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Surface(modifier = Modifier.fillMaxWidth(), color = EditorColors.darkSurfaceVariant, shape = RoundedCornerShape(8.dp), elevation = 0.dp) {
            Column(modifier = Modifier.padding(12.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun CanvasSettings(
    width: String,
    height: String,
    fps: String,
    onWidthChange: (String) -> Unit,
    onHeightChange: (String) -> Unit,
    onFpsChange: (String) -> Unit
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(value = width, onValueChange = onWidthChange, modifier = Modifier.weight(1f), singleLine = true, textStyle = LocalTextStyle.current.copy(fontSize = 13.sp, color = EditorColors.textPrimary), colors = TextFieldDefaults.outlinedTextFieldColors(focusedBorderColor = EditorColors.accentBlue, unfocusedBorderColor = EditorColors.dividerColor, cursorColor = EditorColors.accentBlue))
            Spacer(Modifier.width(8.dp))
            OutlinedTextField(value = height, onValueChange = onHeightChange, modifier = Modifier.weight(1f), singleLine = true, textStyle = LocalTextStyle.current.copy(fontSize = 13.sp, color = EditorColors.textPrimary), colors = TextFieldDefaults.outlinedTextFieldColors(focusedBorderColor = EditorColors.accentBlue, unfocusedBorderColor = EditorColors.dividerColor, cursorColor = EditorColors.accentBlue))
            Spacer(Modifier.width(8.dp))
            OutlinedTextField(value = fps, onValueChange = onFpsChange, modifier = Modifier.weight(1f), singleLine = true, textStyle = LocalTextStyle.current.copy(fontSize = 13.sp, color = EditorColors.textPrimary), colors = TextFieldDefaults.outlinedTextFieldColors(focusedBorderColor = EditorColors.accentBlue, unfocusedBorderColor = EditorColors.dividerColor, cursorColor = EditorColors.accentBlue))
        }
    }
}

@Composable
private fun PerformanceSettings(onionBefore: Int, onionAfter: Int, onBeforeChange: (Int) -> Unit, onAfterChange: (Int) -> Unit) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Slider(value = onionBefore.toFloat(), onValueChange = { onBeforeChange(it.toInt()) }, valueRange = 0f..10f, steps = 9, modifier = Modifier.weight(1f), colors = SliderDefaults.colors(thumbColor = EditorColors.accentBlue, activeTrackColor = EditorColors.accentBlue, inactiveTrackColor = EditorColors.darkSurfaceVariant))
            Text("${onionBefore}", color = EditorColors.textPrimary, fontSize = 12.sp, modifier = Modifier.width(28.dp))
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Slider(value = onionAfter.toFloat(), onValueChange = { onAfterChange(it.toInt()) }, valueRange = 0f..10f, steps = 9, modifier = Modifier.weight(1f), colors = SliderDefaults.colors(thumbColor = EditorColors.accentBlue, activeTrackColor = EditorColors.accentBlue, inactiveTrackColor = EditorColors.darkSurfaceVariant))
            Text("${onionAfter}", color = EditorColors.textPrimary, fontSize = 12.sp, modifier = Modifier.width(28.dp))
        }
    }
}

@Composable
private fun InterfaceSettings() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(EditorStrings["interface.theme"], color = EditorColors.textPrimary, fontSize = 12.sp)
            Text("Dark", color = EditorColors.textMuted, fontSize = 11.sp)
        }
        Switch(checked = true, onCheckedChange = {}, colors = SwitchDefaults.colors(checkedThumbColor = EditorColors.accentBlue, checkedTrackColor = EditorColors.accentBlue.copy(alpha = 0.5f)))
    }
}

@Composable
private fun LanguageSettings(langs: List<LangData>, selected: String, onLangChange: (String) -> Unit) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Язык:", color = EditorColors.textPrimary, fontSize = 12.sp, modifier = Modifier.padding(end = 8.dp))
            langs.forEach { lang ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 12.dp)) {
                    RadioButton(selected = lang.code == selected, onClick = { onLangChange(lang.code) }, colors = RadioButtonDefaults.colors(selectedColor = EditorColors.accentBlue), modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(lang.nameNative, color = EditorColors.textPrimary, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun AboutSection() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(EditorStrings["about.version"], color = EditorColors.textSecondary, fontSize = 12.sp)
        Spacer(Modifier.height(4.dp))
        Text("© 2024 MaryMe", color = EditorColors.textMuted, fontSize = 11.sp)
    }
}
