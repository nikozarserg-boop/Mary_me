package org.example.animation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.animation.ui.theme.EditorColors

@Composable
fun ConfirmExitDialog(
    onSaveAndExit: () -> Unit,
    onExitWithoutSaving: () -> Unit,
    onDismiss: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)).clickable { onDismiss() }, contentAlignment = Alignment.Center) {
        Surface(modifier = Modifier.width(360.dp).height(140.dp), color = EditorColors.darkSurface, shape = RoundedCornerShape(10.dp), elevation = 10.dp) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Text("Есть несохраненные изменения", color = EditorColors.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Text("Сохранить проект перед выходом?", color = EditorColors.textSecondary, fontSize = 12.sp)
                Spacer(Modifier.weight(1f))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onExitWithoutSaving) { Text("Без сохранения", color = EditorColors.textMuted) }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = onSaveAndExit, colors = ButtonDefaults.buttonColors(backgroundColor = EditorColors.accentBlue)) {
                        Text("Сохранить", color = Color.White, fontWeight = FontWeight.Medium)
                    }
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = onDismiss) { Text("Отмена", color = EditorColors.textSecondary) }
                }
            }
        }
    }
}