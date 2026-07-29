package io.github.conflux_org.conflux

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() =
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Conflux",
        ) {
            App()
        }
    }
