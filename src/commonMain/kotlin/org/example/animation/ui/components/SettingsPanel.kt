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
import org.example.animation.engine.AnimationEngine
import org.example.animation.localization.EditorStrings
import org.example.animation.model.AnimationProject
import org.example.animation.ui.theme.EditorColors
import org.example.animation.ui.theme.EditorIcons
import org.example.animation.ui.theme.EditorShapes
import kotlin.math.roundToInt

@Composable
fun SettingsDialog(
    engine: AnimationEngine,
    uiScale: Float,
    onUiScaleChange: (Float) -> Unit,
    onClose: () -> Unit
) {
    val project by engine.project.collectAsState()
    var selectedCategory by remember { mutableStateOf(0) }
    
    // Temporary states for editing
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
                .width(750.dp)
                .height(550.dp)
                .clickable(enabled = false) {},
            color = EditorColors.darkSurface,
            shape = RoundedCornerShape(12.dp),
            elevation = 16.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header (Draggable)
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
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(EditorIcons.iconSettings, null, tint = EditorColors.accentBlue, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(
                        EditorStrings.observeString("settings.title"), 
                        color = EditorColors.textPrimary, 
                        fontSize = 18.sp, 
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
                        Icon(EditorIcons.iconClose, null, tint = EditorColors.textSecondary)
                    }
                }
                
                Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    // Sidebar
                    Column(
                        modifier = Modifier
                            .width(220.dp)
                            .fillMaxHeight()
                            .background(EditorColors.panelBackground)
                            .padding(vertical = 8.dp)
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
                    
                    Divider(modifier = Modifier.width(1.dp).fillMaxHeight(), color = EditorColors.dividerColor)
                    
                    // Content
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(24.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        when (selectedCategory) {
                            0 -> CanvasSettingsContent(
                                canvasWidth, canvasHeight, fps, dpi,
                                { canvasWidth = it }, { canvasHeight = it }, { fps = it }, { dpi = it }
                            )
                            1 -> InterfaceSettingsContent(uiScale, onUiScaleChange)
                            2 -> PerformanceSettingsContent(engine)
                            3 -> AboutSettingsContent(project)
                        }
                    }
                }
                
                Divider(color = EditorColors.dividerColor)
                
                // Footer
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp), 
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onClose) {
                        Text(EditorStrings.observeString("cancel"), color = EditorColors.textSecondary, fontSize = 14.sp)
                    }
                    Spacer(Modifier.width(16.dp))
                    Button(
                        onClick = {
                            val newProject = project.copy()
                            newProject.canvasWidth = canvasWidth.toIntOrNull()?.coerceIn(1, 10000) ?: project.canvasWidth
                            newProject.canvasHeight = canvasHeight.toIntOrNull()?.coerceIn(1, 10000) ?: project.canvasHeight
                            newProject.fps = fps.toIntOrNull()?.coerceIn(1, 120) ?: project.fps
                            newProject.dpi = dpi.toIntOrNull()?.coerceIn(1, 1200) ?: project.dpi
                            engine.setProject(newProject)
                            onClose()
                        },
                        colors = ButtonDefaults.buttonColors(backgroundColor = EditorColors.accentBlue),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(40.dp).widthIn(min = 120.dp)
                    ) {
                        Text(
                            EditorStrings.observeString("apply"), 
                            color = Color.White, 
                            fontSize = 14.sp, 
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryItem(title: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .height(44.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) EditorColors.selectionColor else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            title, 
            color = if (isSelected) Color.White else EditorColors.textSecondary, 
            fontSize = 14.sp, 
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun CanvasSettingsContent(w: String, h: String, f: String, d: String, onW: (String) -> Unit, onH: (String) -> Unit, onF: (String) -> Unit, onD: (String) -> Unit) {
    Column {
        Text(EditorStrings.observeString("settings.canvas"), color = EditorColors.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))
        
        SettingItem(EditorStrings.observeString("canvas.width"), w, onW, "px")
        SettingItem(EditorStrings.observeString("canvas.height"), h, onH, "px")
        SettingItem(EditorStrings.observeString("canvas.fps"), f, onF, "fps")
        SettingItem("DPI", d, onD, "dpi")
    }
}

@Composable
private fun InterfaceSettingsContent(scale: Float, onScale: (Float) -> Unit) {
    Column {
        Text(EditorStrings.observeString("settings.interface"), color = EditorColors.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))

        Text(EditorStrings.observeString("settings.language"), color = EditorColors.textSecondary, fontSize = 13.sp)
        Spacer(Modifier.height(8.dp))
        Row {
            LanguageBtn("Русский", EditorStrings.getCurrentCode() == "ru") { 
                EditorStrings.setLanguage("ru")
            }
            Spacer(Modifier.width(8.dp))
            LanguageBtn("English", EditorStrings.getCurrentCode() == "en") { 
                EditorStrings.setLanguage("en")
            }
        }
        
        Spacer(Modifier.height(32.dp))
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("UI Scale", color = EditorColors.textSecondary, fontSize = 13.sp, modifier = Modifier.weight(1f))
            Text("${(scale * 100).toInt()}%", color = EditorColors.accentBlue, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        Slider(
            value = scale, 
            onValueChange = onScale, 
            valueRange = 0.7f..1.5f, 
            colors = SliderDefaults.colors(thumbColor = EditorColors.accentBlue, activeTrackColor = EditorColors.accentBlue)
        )
    }
}

@Composable
private fun PerformanceSettingsContent(engine: AnimationEngine) {
    val onionBefore by engine.onionSkinFramesBefore.collectAsState()
    val onionAfter by engine.onionSkinFramesAfter.collectAsState()
    
    Column {
        Text(EditorStrings.observeString("settings.performance"), color = EditorColors.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))

        Text(EditorStrings.observeString("perf.onionSkin"), color = EditorColors.textPrimary, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(16.dp))
        
        Text("${EditorStrings.observeString("perf.onionBefore")}: $onionBefore", fontSize = 13.sp, color = EditorColors.textSecondary)
        Slider(value = onionBefore.toFloat(), onValueChange = { engine.setOnionSkinFramesBefore(it.toInt()) }, valueRange = 0f..10f, steps = 9)
        
        Spacer(Modifier.height(8.dp))
        
        Text("${EditorStrings.observeString("perf.onionAfter")}: $onionAfter", fontSize = 13.sp, color = EditorColors.textSecondary)
        Slider(value = onionAfter.toFloat(), onValueChange = { engine.setOnionSkinFramesAfter(it.toInt()) }, valueRange = 0f..10f, steps = 9)
    }
}

@Composable
private fun AboutSettingsContent(project: AnimationProject) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(top = 20.dp)) {
        Surface(modifier = Modifier.size(80.dp), color = EditorColors.darkSurfaceVariant, shape = RoundedCornerShape(16.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(EditorIcons.iconAppIcon, null, tint = EditorColors.accentBlue, modifier = Modifier.size(48.dp))
            }
        }
        Spacer(Modifier.height(20.dp))
        Text("MaryMe Animator", color = EditorColors.textPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text(EditorStrings.observeString("about.version"), color = EditorColors.accentGreen, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        
        Spacer(Modifier.height(16.dp))
        
        // Project Stats
        val hours = project.workingTimeMs / 3600000
        val minutes = (project.workingTimeMs % 3600000) / 60000
        Text("Время работы над проектом: ${hours}ч ${minutes}м", color = EditorColors.textSecondary, fontSize = 13.sp)

        Spacer(Modifier.height(32.dp))
        
        Surface(
            color = EditorColors.darkSurfaceVariant.copy(alpha = 0.5f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                EditorStrings.observeString("about.desc"), 
                color = EditorColors.textSecondary, 
                fontSize = 14.sp, 
                lineHeight = 20.sp,
                modifier = Modifier.padding(16.dp)
            )
        }
        
        Spacer(Modifier.weight(1f))
        Text("Created by niko. MIT License.", color = EditorColors.textMuted, fontSize = 11.sp)
    }
}

@Composable
private fun SettingItem(label: String, value: String, onValueChange: (String) -> Unit, suffix: String = "") {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 16.dp)) {
        Text(label, color = EditorColors.textSecondary, fontSize = 14.sp, modifier = Modifier.width(150.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f).height(50.dp),
            textStyle = LocalTextStyle.current.copy(color = EditorColors.textPrimary, fontSize = 15.sp),
            singleLine = true,
            trailingIcon = { Text(suffix, color = EditorColors.textMuted, fontSize = 12.sp, modifier = Modifier.padding(end = 8.dp)) },
            colors = TextFieldDefaults.outlinedTextFieldColors(focusedBorderColor = EditorColors.accentBlue, unfocusedBorderColor = EditorColors.dividerColor, cursorColor = EditorColors.accentBlue, backgroundColor = EditorColors.darkSurfaceVariant)
        )
    }
}

@Composable
private fun LanguageBtn(text: String, isSelected: Boolean, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.height(40.dp).width(140.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            backgroundColor = if (isSelected) EditorColors.accentBlue.copy(alpha = 0.15f) else Color.Transparent,
            contentColor = if (isSelected) EditorColors.accentBlue else EditorColors.textSecondary
        ),
        border = BorderStroke(1.dp, if (isSelected) EditorColors.accentBlue else EditorColors.dividerColor),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(text, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
    }
}
