package org.example

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.runtime.Composable
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
    Window(
        onCloseRequest = { exitApplication() },
        state = windowState,
        title = "MaryMe",
        icon = painterResource(Res.drawable.icon)
    ) {
        App()
    }
}