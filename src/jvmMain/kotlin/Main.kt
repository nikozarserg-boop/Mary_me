package org.example

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.runtime.*
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import org.jetbrains.compose.resources.painterResource
import org.example.mary_me.generated.resources.Res
import org.example.mary_me.generated.resources.icon

@Preview
@Composable
fun AppPreview() {
    App()
}

fun main() = application {
    val windowState = rememberWindowState(placement = WindowPlacement.Maximized)
    var exitRequested by remember { mutableStateOf(false) }

    Window(
        onCloseRequest = { exitRequested = true },
        state = windowState,
        title = "MaryMe",
        icon = painterResource(Res.drawable.icon)
    ) {
        App(
            exitRequested = exitRequested,
            onExitConfirm = { exitApplication() },
            onExitCancel = { exitRequested = false }
        )
    }
}
