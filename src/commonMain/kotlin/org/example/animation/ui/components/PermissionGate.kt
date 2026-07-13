package org.example.animation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.animation.localization.EditorStrings
import org.example.animation.ui.theme.EditorColors

/**
 * Composable-обёртка, проверяющая разрешения к файлам.
 * Показывает экран с просьбой предоставить разрешения, если они не получены.
 */
@Composable
fun PermissionGate(
    hasPermissions: Boolean,
    onRequestPermissions: () -> Unit,
    onOpenSettings: () -> Unit,
    content: @Composable () -> Unit
) {
    // Проверяем разрешения каждый раз при отрисовке
    // Это обеспечивает возврат к приложению после предоставления разрешений
    if (hasPermissions) {
        content()
    } else {
        PermissionScreen(
            onRequestPermissions = onRequestPermissions,
            onOpenSettings = onOpenSettings
        )
    }
}

@Composable
private fun PermissionScreen(
    onRequestPermissions: () -> Unit,
    onOpenSettings: () -> Unit
) {
    var showSettingsHint by remember { mutableStateOf(false) }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(EditorColors.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                text = EditorStrings["permission.title"],
                color = EditorColors.textPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = EditorStrings["permission.message"],
                color = EditorColors.textSecondary,
                fontSize = 14.sp
            )
            
            if (showSettingsHint) {
                Text(
                    text = EditorStrings["permission.settingsHint"],
                    color = EditorColors.textMuted,
                    fontSize = 12.sp
                )
            }
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onRequestPermissions,
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = EditorColors.accent,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = EditorStrings["permission.allowBtn"],
                        fontSize = 14.sp
                    )
                }
                
                Button(
                    onClick = { 
                        showSettingsHint = !showSettingsHint
                        if (showSettingsHint) {
                            onOpenSettings()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = EditorColors.panelHeader,
                        contentColor = EditorColors.textPrimary
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = EditorStrings["permission.openSettingsBtn"],
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}