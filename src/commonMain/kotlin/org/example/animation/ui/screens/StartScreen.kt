package org.example.animation.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.example.animation.engine.Renderer
import org.example.animation.io.*
import org.example.animation.localization.EditorStrings
import org.example.animation.model.AnimationProject
import org.example.animation.ui.theme.*
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Composable
fun StartScreen(
    onNewProject: () -> Unit,
    onOpenProject: () -> Unit,
    onOpenRecent: (String) -> Unit,
    uiScale: Float
) {
    val recentProjects = remember { AppSettingsManager.getRecentProjects() }
    val scope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize().background(EditorColors.background)) {
        // Фоновый декор
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(EditorColors.accent.copy(alpha = 0.08f), Color.Transparent),
                        center = androidx.compose.ui.geometry.Offset(0f, 0f),
                        radius = 1200f
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 60.dp.scaled(), vertical = 40.dp.scaled()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Заголовок по центру
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = EditorStrings.observeString("app.name"),
                    style = EditorTypography.h1(),
                    color = EditorColors.textPrimary,
                    fontSize = 36.sp.scaled(),
                    fontWeight = FontWeight.Black
                )
                Spacer(Modifier.height(8.dp.scaled()))
                Text(
                    text = EditorStrings.observeString("about.desc"),
                    style = EditorTypography.body(),
                    color = EditorColors.textSecondary,
                    fontSize = 14.sp.scaled()
                )
            }

            Spacer(Modifier.height(48.dp.scaled()))

            // Кнопки под заголовком
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (recentProjects.isNotEmpty()) {
                    StartActionBtn(
                        text = EditorStrings.observeString("start.openLast"),
                        icon = EditorIcons.iconPlayArrow,
                        color = EditorColors.accentGreen,
                        onClick = { onOpenRecent(recentProjects.first().path) }
                    )
                }
                Spacer(Modifier.width(16.dp.scaled()))
                StartActionBtn(
                    text = EditorStrings.observeString("file.new"),
                    icon = EditorIcons.iconAdd,
                    color = EditorColors.accent,
                    onClick = onNewProject
                )
                Spacer(Modifier.width(16.dp.scaled()))
                StartActionBtn(
                    text = EditorStrings.observeString("file.open"),
                    icon = EditorIcons.iconFolderOpen,
                    color = EditorColors.surfaceVariant,
                    onClick = onOpenProject
                )
            }

            Spacer(Modifier.height(48.dp.scaled()))

            Text(
                text = EditorStrings.observeString("file.recent"),
                style = EditorTypography.h2(),
                color = EditorColors.textPrimary,
                fontSize = 20.sp.scaled(),
                fontWeight = FontWeight.Bold
            )
            
            Spacer(Modifier.height(24.dp.scaled()))

            if (recentProjects.isNotEmpty()) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 280.dp.scaled()),
                    horizontalArrangement = Arrangement.spacedBy(24.dp.scaled()),
                    verticalArrangement = Arrangement.spacedBy(24.dp.scaled()),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 40.dp.scaled())
                ) {
                    items(recentProjects) { project ->
                        RecentProjectCard(
                            project = project,
                            onClick = { onOpenRecent(project.path) }
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxSize().padding(bottom = 100.dp.scaled()), 
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            EditorIcons.iconHistory, 
                            null, 
                            modifier = Modifier.size(80.dp.scaled()), 
                            tint = EditorColors.textMuted.copy(alpha = 0.15f)
                        )
                        Spacer(Modifier.height(20.dp.scaled()))
                        Text(
                            EditorStrings.observeString("start.noRecent"),
                            color = EditorColors.textMuted,
                            style = EditorTypography.body(),
                            fontSize = 16.sp.scaled()
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun StartActionBtn(
    text: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val scale by animateFloatAsState(if (isHovered) 1.04f else 1f)
    val elevation by animateDpAsState(if (isHovered) 12.dp else 2.dp)

    Surface(
        onClick = onClick,
        modifier = Modifier
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .hoverable(interactionSource),
        shape = RoundedCornerShape(14.dp.scaled()),
        color = color,
        elevation = elevation.scaled()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 24.dp.scaled(), vertical = 14.dp.scaled()),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(20.dp.scaled()))
            Spacer(Modifier.width(12.dp.scaled()))
            Text(text, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp.scaled())
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun RecentProjectCard(
    project: RecentProject,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val cardScale by animateFloatAsState(if (isHovered) 1.03f else 1f)
    
    val fileHandler = remember { createPlatformFileHandler() }
    
    var loadedProject by remember { mutableStateOf<AnimationProject?>(null) }
    val frames = remember { mutableStateListOf<ImageBitmap>() }
    var currentPreviewFrame by remember { mutableStateOf(0) }
    var isPreviewLoading by remember { mutableStateOf(false) }

    // Ленивая загрузка первого кадра сразу, анимация - при наведении
    LaunchedEffect(Unit) {
        withContext(Dispatchers.Default) {
            try {
                val data = fileHandler.readFromPath(project.path)
                if (data != null) {
                    val proj = ProjectSerializer.deserializeFromBytes(data)
                    loadedProject = proj
                    
                    // Рендерим первый кадр
                    val bitmap = renderProjectFrame(proj, 0)
                    withContext(Dispatchers.Main) {
                        frames.add(bitmap)
                    }
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    // Анимация превью при наведении
    LaunchedEffect(isHovered) {
        if (isHovered && loadedProject != null) {
            val proj = loadedProject!!
            if (frames.size < minOf(proj.maxFrames, 12)) {
                isPreviewLoading = true
                withContext(Dispatchers.Default) {
                    val previewFrameCount = minOf(proj.maxFrames, 12)
                    for (i in frames.size until previewFrameCount) {
                        val bitmap = renderProjectFrame(proj, i)
                        withContext(Dispatchers.Main) {
                            frames.add(bitmap)
                        }
                    }
                }
                isPreviewLoading = false
            }
            
            // Цикл анимации
            while (isHovered && frames.size > 1) {
                delay(1000L / proj.fps.coerceIn(1, 120))
                currentPreviewFrame = (currentPreviewFrame + 1) % frames.size
            }
        } else {
            currentPreviewFrame = 0
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp.scaled())
            .graphicsLayer(scaleX = cardScale, scaleY = cardScale)
            .hoverable(interactionSource)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp.scaled()),
        backgroundColor = EditorColors.surface,
        elevation = if (isHovered) 16.dp.scaled() else 4.dp.scaled(),
        border = if (isHovered) BorderStroke(2.dp.scaled(), EditorColors.accent.copy(alpha = 0.5f)) else null
    ) {
        Column {
            // Превью область
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.3f)
                    .background(EditorColors.surfaceVariant)
                    .clip(RoundedCornerShape(topStart = 16.dp.scaled(), topEnd = 16.dp.scaled())),
                contentAlignment = Alignment.Center
            ) {
                if (frames.isNotEmpty()) {
                    Image(
                        bitmap = frames[currentPreviewFrame.coerceIn(0, frames.lastIndex)],
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Icon(
                        EditorIcons.iconImage, 
                        null, 
                        tint = EditorColors.textMuted.copy(alpha = 0.2f),
                        modifier = Modifier.size(56.dp.scaled())
                    )
                }
                
                if (isPreviewLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp.scaled()).align(Alignment.TopEnd).padding(12.dp.scaled()), 
                        color = EditorColors.accent, 
                        strokeWidth = 2.dp.scaled()
                    )
                }
            }

            // Инфо область
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp.scaled())
            ) {
                Text(
                    text = project.name,
                    style = EditorTypography.body(),
                    fontWeight = FontWeight.Bold,
                    color = EditorColors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 15.sp.scaled()
                )
                
                Spacer(Modifier.height(4.dp.scaled()))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(EditorIcons.iconDateRange, null, tint = EditorColors.textMuted, modifier = Modifier.size(12.dp.scaled()))
                    Spacer(Modifier.width(6.dp.scaled()))
                    Text(
                        text = formatTimestamp(project.lastAccessed),
                        style = EditorTypography.caption(),
                        color = EditorColors.textMuted,
                        fontSize = 11.sp.scaled()
                    )
                }
            }
        }
    }
}

private fun renderProjectFrame(proj: AnimationProject, frameIndex: Int): ImageBitmap {
    val bitmap = ImageBitmap(proj.canvasWidth, proj.canvasHeight)
    val canvas = Canvas(bitmap)
    val drawScope = CanvasDrawScope()
    drawScope.draw(
        density = Density(1f),
        layoutDirection = LayoutDirection.Ltr,
        canvas = canvas,
        size = androidx.compose.ui.geometry.Size(proj.canvasWidth.toFloat(), proj.canvasHeight.toFloat())
    ) {
        Renderer.renderFrame(this, proj, frameIndex, backgroundColor = Renderer.ulongToColor(proj.backgroundColor))
    }
    return bitmap
}

private fun formatTimestamp(timestamp: Long): String {
    return try {
        val dt = Instant.fromEpochMilliseconds(timestamp).toLocalDateTime(TimeZone.currentSystemDefault())
        "${dt.dayOfMonth.toString().padStart(2, '0')}.${dt.monthNumber.toString().padStart(2, '0')}.${dt.year} ${dt.hour.toString().padStart(2, '0')}:${dt.minute.toString().padStart(2, '0')}"
    } catch (e: Exception) { "" }
}
