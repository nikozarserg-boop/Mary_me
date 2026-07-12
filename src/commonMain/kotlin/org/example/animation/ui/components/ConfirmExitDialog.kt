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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.animation.localization.EditorStrings
import org.example.animation.ui.theme.*

@Composable
fun ConfirmExitDialog(
    onSaveAndExit: () -> Unit,
    onExitWithoutSaving: () -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable { onDismiss() }, 
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .width(420.dp.scaled())
                .clickable(enabled = false) {},
            color = EditorColors.darkSurface, 
            shape = RoundedCornerShape(12.dp.scaled()), 
            elevation = 16.dp.scaled()
        ) {
            Column(modifier = Modifier.padding(24.dp.scaled())) {
                Text(
                    text = EditorStrings.observeString("exit.unsavedTitle"), 
                    color = EditorColors.textPrimary, 
                    fontSize = 18.sp.scaled(), 
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(Modifier.height(12.dp.scaled()))
                
                Text(
                    text = EditorStrings.observeString("exit.unsavedDesc"), 
                    color = EditorColors.textSecondary, 
                    fontSize = 14.sp.scaled(),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                    lineHeight = 20.sp.scaled()
                )
                
                Spacer(Modifier.height(32.dp.scaled()))
                
                Row(
                    modifier = Modifier.fillMaxWidth(), 
                    horizontalArrangement = Arrangement.spacedBy(12.dp.scaled())
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(42.dp.scaled()),
                        shape = RoundedCornerShape(8.dp.scaled()),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = EditorColors.textPrimary)
                    ) {
                        Text(EditorStrings.observeString("cancel"), fontSize = 13.sp.scaled())
                    }
                    
                    TextButton(
                        onClick = onExitWithoutSaving,
                        modifier = Modifier.weight(1.2f).height(42.dp.scaled()),
                        shape = RoundedCornerShape(8.dp.scaled()),
                        colors = ButtonDefaults.textButtonColors(contentColor = EditorColors.accentRed)
                    ) {
                        Text(EditorStrings.observeString("exit.noSave"), fontSize = 13.sp.scaled())
                    }
                }
                
                Spacer(Modifier.height(12.dp.scaled()))
                
                Button(
                    onClick = onSaveAndExit, 
                    modifier = Modifier.fillMaxWidth().height(46.dp.scaled()),
                    colors = ButtonDefaults.buttonColors(backgroundColor = EditorColors.accentBlue),
                    shape = RoundedCornerShape(8.dp.scaled())
                ) {
                    Text(EditorStrings.observeString("exit.saveAndExit"), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp.scaled())
                }
            }
        }
    }
}
