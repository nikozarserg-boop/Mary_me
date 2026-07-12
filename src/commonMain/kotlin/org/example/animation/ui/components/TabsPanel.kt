package org.example.animation.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import org.example.animation.engine.AnimationEngine
import org.example.animation.engine.ProjectManager
import org.example.animation.ui.theme.*

@Composable
fun TabsPanel(
    projectManager: ProjectManager,
    onCloseRequested: (Int) -> Unit
) {
    val engines by projectManager.engines.collectAsState()
    val activeIndex by projectManager.activeEngineIndex.collectAsState()

    if (engines.size <= 1) return

    Surface(
        modifier = Modifier.fillMaxWidth().height(32.dp.scaled()),
        color = EditorColors.panelHeader,
        elevation = 2.dp.scaled()
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 4.dp.scaled()),
            verticalAlignment = Alignment.Bottom
        ) {
            engines.forEachIndexed { index, engine ->
                // Используем явный вызов key из runtime, чтобы избежать конфликтов
                androidx.compose.runtime.key(engine.id) {
                    ProjectTab(
                        engine = engine,
                        isActive = index == activeIndex,
                        onClick = { projectManager.setActiveProject(index) },
                        onClose = { onCloseRequested(index) },
                        onPin = { projectManager.togglePin(index) },
                        onMove = { delta ->
                            val targetIndex = (index + (if (delta > 0) 1 else -1)).coerceIn(engines.indices)
                            if (targetIndex != index) {
                                projectManager.moveProject(index, targetIndex)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ProjectTab(
    engine: AnimationEngine,
    isActive: Boolean,
    onClick: () -> Unit,
    onClose: () -> Unit,
    onPin: () -> Unit,
    onMove: (Float) -> Unit
) {
    val project by engine.project.collectAsState()
    val hasChanges by engine.hasUnsavedChanges.collectAsState()
    val isPinned by engine.isPinned.collectAsState()
    
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    
    val backgroundColor by animateColorAsState(
        when {
            isActive -> EditorColors.background
            isHovered -> EditorColors.hover
            else -> Color.Transparent
        }
    )

    var dragAccumulator by remember { mutableStateOf(0f) }

    Box(
        modifier = Modifier
            .width(160.dp.scaled())
            .height(28.dp.scaled())
            .clip(RoundedCornerShape(topStart = 6.dp.scaled(), topEnd = 6.dp.scaled()))
            .background(backgroundColor)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragAccumulator += dragAmount.x
                        if (kotlin.math.abs(dragAccumulator) > 100.dp.toPx()) {
                            onMove(dragAccumulator)
                            dragAccumulator = 0f
                        }
                    },
                    onDragEnd = { dragAccumulator = 0f }
                )
            }
            .padding(horizontal = 8.dp.scaled()),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Unsaved changes dot
            if (hasChanges) {
                Box(
                    modifier = Modifier
                        .size(6.dp.scaled())
                        .clip(RoundedCornerShape(3.dp.scaled()))
                        .background(EditorColors.accentOrange)
                )
                Spacer(Modifier.width(6.dp.scaled()))
            }

            Text(
                text = project.name,
                style = EditorTypography.menu().copy(
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 12.sp.scaled()
                ),
                color = if (isActive) EditorColors.accent else EditorColors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            if (isHovered || isActive || isPinned) {
                IconButton(
                    onClick = onPin,
                    modifier = Modifier.size(18.dp.scaled())
                ) {
                    Icon(
                        if (isPinned) EditorIcons.iconPushPin else EditorIcons.iconPushPinOutlined,
                        null,
                        tint = if (isPinned) EditorColors.accent else EditorColors.textMuted,
                        modifier = Modifier.size(12.dp.scaled())
                    )
                }
                
                if (!isPinned) {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.size(18.dp.scaled())
                    ) {
                        Icon(
                            EditorIcons.iconClose,
                            null,
                            tint = EditorColors.textMuted,
                            modifier = Modifier.size(12.dp.scaled())
                        )
                    }
                }
            }
        }

        // Active indicator line
        if (isActive) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp.scaled())
                    .background(EditorColors.accent)
                    .align(Alignment.TopCenter)
            )
        }
    }
}
