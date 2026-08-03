package com.woodenfish.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

enum class ThemeMode { SYSTEM, LIGHT, DARK }

@Composable
fun WoodenFishTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    themeColorIndex: Int = THEME_BROWN,
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    val useDark = when (themeMode) { ThemeMode.DARK -> true; ThemeMode.LIGHT -> false; ThemeMode.SYSTEM -> darkTheme }
    val scheme = if (useDark) allThemes[themeColorIndex].darkScheme else allThemes[themeColorIndex].lightScheme
    MaterialTheme(colorScheme = scheme, content = content)
}
