package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
  primary = PhoneBlue,
  onPrimary = Color.White,
  primaryContainer = PhoneBlueLight,
  onPrimaryContainer = PhoneBlueDark,
  secondary = PhoneBlueText,
  secondaryContainer = PhoneBlueLight,
  onSecondaryContainer = PhoneBlueDark,
  background = BackgroundLight,
  surface = SurfaceLight,
  surfaceVariant = DialpadBg,
  onBackground = OnSurfaceLight,
  onSurface = OnSurfaceLight,
  onSurfaceVariant = OnSurfaceVariantLight,
  outline = DialpadKeyBorder,
)

private val DarkColorScheme = darkColorScheme(
  primary = Color(0xFFA8C7FA),
  onPrimary = Color(0xFF041E49),
  primaryContainer = Color(0xFF004A77),
  onPrimaryContainer = Color(0xFFC2E7FF),
  secondary = Color(0xFFA8C7FA),
  background = Color(0xFF111318),
  surface = Color(0xFF1E2024),
  surfaceVariant = Color(0xFF282A2F),
  onBackground = Color(0xFFE2E2E6),
  onSurface = Color(0xFFE2E2E6),
  onSurfaceVariant = Color(0xFFC4C6D0),
  outline = Color(0xFF44474E),
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false, // Use our crisp dialed M3 colors for 1:1 match
  content: @Composable () -> Unit,
) {
  val colorScheme = when {
    dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
      val context = LocalContext.current
      if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    }
    darkTheme -> DarkColorScheme
    else -> LightColorScheme
  }

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content
  )
}
