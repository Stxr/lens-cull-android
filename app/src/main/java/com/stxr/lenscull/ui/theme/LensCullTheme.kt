package com.stxr.lenscull.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
  primary = Color(0xFFFFC857),
  secondary = Color(0xFF73D2DE),
  background = Color(0xFF0D0F12),
  surface = Color(0xFF15181D),
  surfaceVariant = Color(0xFF242830),
  onBackground = Color(0xFFF2F3F5),
  onSurface = Color(0xFFF2F3F5),
)

private val LightColors = lightColorScheme(
  primary = Color(0xFF755600),
  secondary = Color(0xFF006874),
  background = Color(0xFFF7F7F9),
  surface = Color.White,
)

@Composable
fun LensCullTheme(darkTheme: Boolean = true, content: @Composable () -> Unit) {
  MaterialTheme(
    colorScheme = if (darkTheme) DarkColors else LightColors,
    typography = Typography(),
    content = content,
  )
}
