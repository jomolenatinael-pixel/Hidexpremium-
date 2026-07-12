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

private val DarkColorScheme =
  darkColorScheme(primary = Purple80, secondary = PurpleGrey80, tertiary = Pink80)

private val LightColorScheme =
  lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40,
  )

// AMOLED-optimised scheme: true-black backgrounds (0xFF000000) for every surface, which
// lets OLED panels switch off pixels entirely and save significant battery. The accent
// colours are kept from the dark scheme so the app still looks like "HideX".
private val AmoledColorScheme =
  darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80,
    background = Color.Black,
    onBackground = Color(0xFFE6E1E5),
    surface = Color.Black,
    onSurface = Color(0xFFE6E1E5),
    surfaceVariant = Color(0xFF121212),
    onSurfaceVariant = Color(0xFFCAC4D0),
    surfaceTint = Purple80,
    inverseSurface = Color(0xFFE6E1E5),
    inverseOnSurface = Color(0xFF121212),
    surfaceContainer = Color(0xFF0A0A0A),
    surfaceContainerHigh = Color(0xFF141414),
    surfaceContainerHighest = Color(0xFF1A1A1A),
    surfaceContainerLow = Color(0xFF0D0D0D),
    surfaceContainerLowest = Color(0xFF000000),
  )

/**
 * The resolved theme mode. MainActivity maps the stored string preference to this enum so
 * that the theme composable can pick the correct colour scheme (including the distinct
 * AMOLED true-black scheme).
 */
enum class ThemeMode { SYSTEM, LIGHT, DARK, AMOLED }

/**
 * Application theme wrapper.
 *
 * @param themeMode the user's selected theme mode; defaults to SYSTEM which follows the
 *   platform light/dark setting.
 * @param dynamicColor when true (and on Android 12+) the dynamic colour scheme derived from
 *   the user's wallpaper is used. AMOLED always overrides this to keep backgrounds true-black.
 */
@Composable
fun MyApplicationTheme(
  themeMode: ThemeMode = ThemeMode.SYSTEM,
  dynamicColor: Boolean = true,
  content: @Composable () -> Unit,
) {
  val systemDark = isSystemInDarkTheme()
  val useDark = when (themeMode) {
    ThemeMode.LIGHT -> false
    ThemeMode.DARK, ThemeMode.AMOLED -> true
    ThemeMode.SYSTEM -> systemDark
  }

  val colorScheme =
    when {
      // AMOLED always uses the true-black scheme, regardless of dynamic colour, so the
      // OLED battery savings are preserved even on Android 12+ devices.
      themeMode == ThemeMode.AMOLED -> AmoledColorScheme

      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (useDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      useDark -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
