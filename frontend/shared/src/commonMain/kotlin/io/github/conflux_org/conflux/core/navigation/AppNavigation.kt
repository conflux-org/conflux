package io.github.conflux_org.conflux.core.navigation

import androidx.compose.animation.Crossfade
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import io.github.conflux_org.conflux.features.auth.presentation.AuthScreen
import io.github.conflux_org.conflux.features.auth.presentation.AuthViewModel
import io.github.conflux_org.conflux.features.main.presentation.MainScreen
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AppNavigation() {
    val backStack = remember { mutableStateListOf<NavKey>(NavKey.Auth) }
    val currentKey = backStack.lastOrNull() ?: NavKey.Auth

    Crossfade(targetState = currentKey) { key ->
        when (key) {
            is NavKey.Auth -> {
                val authViewModel: AuthViewModel = koinViewModel()
                authViewModel.onLoginSuccess = {
                    backStack.clear()
                    backStack.add(NavKey.Main)
                }
                authViewModel.onSignUpSuccess = {
                    backStack.clear()
                    backStack.add(NavKey.Main)
                }

                AuthScreen(viewModel = authViewModel)
            }

            is NavKey.Main -> {
                MainScreen(
                    onLogoutClick = {
                        backStack.clear()
                        backStack.add(NavKey.Auth)
                    },
                )
            }
        }
    }
}
