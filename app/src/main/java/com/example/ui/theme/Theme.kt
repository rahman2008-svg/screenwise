package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkAmoledColorScheme = darkColorScheme(
    primary = AmoledPrimary,
    secondary = AmoledSecondary,
    tertiary = AmoledTertiary,
    background = AmoledBackground,
    surface = AmoledSurface,
    onBackground = AmoledOnSurface,
    onSurface = AmoledOnSurface
)

private val EmeraldColorScheme = darkColorScheme(
    primary = EmeraldPrimary,
    secondary = EmeraldSecondary,
    tertiary = EmeraldTertiary,
    background = EmeraldBackground,
    surface = EmeraldSurface,
    onBackground = EmeraldOnSurface,
    onSurface = EmeraldOnSurface
)

private val OceanColorScheme = darkColorScheme(
    primary = OceanPrimary,
    secondary = OceanSecondary,
    tertiary = OceanTertiary,
    background = OceanBackground,
    surface = OceanSurface,
    onBackground = OceanOnSurface,
    onSurface = OceanOnSurface
)

private val SunsetColorScheme = darkColorScheme(
    primary = SunsetPrimary,
    secondary = SunsetSecondary,
    tertiary = SunsetTertiary,
    background = SunsetBackground,
    surface = SunsetSurface,
    onBackground = SunsetOnSurface,
    onSurface = SunsetOnSurface
)

private val SleekColorScheme = lightColorScheme(
    primary = SleekPrimary,
    onPrimary = SleekOnPrimary,
    primaryContainer = SleekPrimaryContainer,
    onPrimaryContainer = SleekOnPrimaryContainer,
    secondary = SleekSecondary,
    onSecondary = SleekOnSecondary,
    secondaryContainer = SleekSecondaryContainer,
    onSecondaryContainer = SleekOnSecondaryContainer,
    tertiary = SleekTertiary,
    onTertiary = SleekOnTertiary,
    tertiaryContainer = SleekTertiaryContainer,
    onTertiaryContainer = SleekOnTertiaryContainer,
    background = SleekBackground,
    onBackground = SleekOnBackground,
    surface = SleekSurface,
    onSurface = SleekOnSurface,
    surfaceVariant = SleekSurfaceVariant,
    onSurfaceVariant = SleekOnSurfaceVariant,
    outline = SleekOutline,
    outlineVariant = SleekOutlineVariant,
    error = SleekError,
    onError = SleekOnError
)

@Composable
fun ScreenWiseTheme(
    themeMode: String = "Sleek Interface",
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when (themeMode) {
        "Sleek Interface" -> SleekColorScheme
        "AMOLED Dark" -> DarkAmoledColorScheme
        "Emerald Focus" -> EmeraldColorScheme
        "Ocean Serene" -> OceanColorScheme
        "Sunset Focus" -> SunsetColorScheme
        "Material You" -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                dynamicDarkColorScheme(context)
            } else {
                SleekColorScheme
            }
        }
        else -> SleekColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
