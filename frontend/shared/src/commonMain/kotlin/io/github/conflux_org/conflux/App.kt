package io.github.conflux_org.conflux

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import io.github.conflux_org.conflux.di.appModule
import io.github.conflux_org.conflux.features.auth.presentation.AuthScreen
import io.github.conflux_org.conflux.features.auth.presentation.AuthViewModel
import org.koin.compose.KoinApplication
import org.koin.compose.viewmodel.koinViewModel
import org.koin.dsl.koinConfiguration

@Composable
@Preview
@Suppress("ktlint:standard:function-naming")
fun App() {
    MaterialTheme {
        KoinApplication(
            configuration =
                koinConfiguration {
                    modules(appModule)
                },
        ) {
            val viewModel: AuthViewModel = koinViewModel()
            AuthScreen(viewModel = viewModel)
        }
    }
}
