package io.github.conflux_org.conflux.core.navigation

/**
 * Navigation 3 鍵值定義 (Type-safe Navigation Keys)
 */
sealed interface NavKey {
    data object Auth : NavKey

    data object Main : NavKey
}
