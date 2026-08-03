package com.woodenfish.app.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// ─── Theme color definitions ───
data class ThemeColors(
    val name: String, val nameTW: String, val nameEN: String,
    val lightScheme: ColorScheme, val darkScheme: ColorScheme
)

private fun light(pri: Long, sec: Long, ter: Long) = lightColorScheme(
    primary = Color(pri), onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(pri).copy(alpha = 0.15f), onPrimaryContainer = Color(0xFF1A1A1A),
    secondary = Color(sec), onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(sec).copy(alpha = 0.15f), onSecondaryContainer = Color(0xFF1A1A1A),
    tertiary = Color(ter), onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(ter).copy(alpha = 0.15f), onTertiaryContainer = Color(0xFF1A1A1A),
    surface = Color(0xFFF8F8F7), onSurface = Color(0xFF1C1B1A),
    surfaceVariant = Color(0xFFE7E2DE), onSurfaceVariant = Color(0xFF494643),
    background = Color(0xFFF8F8F7), onBackground = Color(0xFF1C1B1A),
    outline = Color(0xFF7A7572),
)

private fun dark(pri: Long, sec: Long, ter: Long) = darkColorScheme(
    primary = Color(pri).copy(alpha = 0.9f), onPrimary = Color(0xFF1A1A1A),
    primaryContainer = Color(pri).copy(alpha = 0.3f), onPrimaryContainer = Color(pri).copy(alpha = 0.9f),
    secondary = Color(sec).copy(alpha = 0.85f), onSecondary = Color(0xFF1A1A1A),
    secondaryContainer = Color(sec).copy(alpha = 0.25f), onSecondaryContainer = Color(sec).copy(alpha = 0.85f),
    tertiary = Color(ter).copy(alpha = 0.85f), onTertiary = Color(0xFF1A1A1A),
    tertiaryContainer = Color(ter).copy(alpha = 0.25f), onTertiaryContainer = Color(ter).copy(alpha = 0.85f),
    surface = Color(0xFF141312), onSurface = Color(0xFFE4E2E0),
    surfaceVariant = Color(0xFF494643), onSurfaceVariant = Color(0xFFCAC6C1),
    background = Color(0xFF141312), onBackground = Color(0xFFE4E2E0),
    outline = Color(0xFF94908B),
)

// ─── 8 theme color sets ───
val allThemes = listOf(
    ThemeColors("木棕", "木棕", "Brown", light(0xFF6D4C41, 0xFF5D7344, 0xFFBA5A3C), dark(0xFFD2BBAF, 0xFFC4CCB3, 0xFFFFB59B)),
    ThemeColors("枫红", "楓紅", "Red", light(0xFFC62828, 0xFF6D4C41, 0xFFE65100), dark(0xFFEF9A9A, 0xFFBCAAA4, 0xFFFFAB91)),
    ThemeColors("暖橙", "暖橙", "Orange", light(0xFFE65100, 0xFF5D7344, 0xFFBA5A3C), dark(0xFFFFAB91, 0xFFC4CCB3, 0xFFFFD180)),
    ThemeColors("琥珀黄", "琥珀黃", "Yellow", light(0xFFF9A825, 0xFF6D4C41, 0xFFBA5A3C), dark(0xFFFFE082, 0xFFBCAAA4, 0xFFFFD180)),
    ThemeColors("森绿", "森綠", "Green", light(0xFF2E7D32, 0xFF6D4C41, 0xFFBF360C), dark(0xFFA5D6A7, 0xFFBCAAA4, 0xFFFFAB91)),
    ThemeColors("海蓝", "海藍", "Blue", light(0xFF1565C0, 0xFF5D7344, 0xFFBA5A3C), dark(0xFF90CAF9, 0xFFC4CCB3, 0xFFFFD180)),
    ThemeColors("藤紫", "藤紫", "Purple", light(0xFF7B1FA2, 0xFF5D7344, 0xFFBA5A3C), dark(0xFFCE93D8, 0xFFC4CCB3, 0xFFFFD180)),
    ThemeColors("湖青", "湖青", "Cyan", light(0xFF00838F, 0xFF6D4C41, 0xFFBF360C), dark(0xFF80DEEA, 0xFFBCAAA4, 0xFFFFAB91)),
)

// ─── +1 rainbow colors ───
val PlusOneColors = listOf(
    Color(0xFFE53935), Color(0xFFFF6D00), Color(0xFFFFC107),
    Color(0xFF43A047), Color(0xFF00ACC1), Color(0xFF1E88E5), Color(0xFF8E24AA),
)

val THEME_BROWN = 0; val THEME_RED = 1; val THEME_ORANGE = 2; val THEME_YELLOW = 3
val THEME_GREEN = 4; val THEME_BLUE = 5; val THEME_PURPLE = 6; val THEME_CYAN = 7
