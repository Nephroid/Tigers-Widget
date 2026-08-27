package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = TigersOrange,
    secondary = TigersLightNavy,
    tertiary = TigersGold,
    background = TigersDarkBg,
    surface = TigersCardBg,
    onPrimary = OffWhiteText,
    onSecondary = OffWhiteText,
    onTertiary = TigersDarkBg,
    onBackground = OffWhiteText,
    onSurface = OffWhiteText,
    primaryContainer = TigersLightNavy,
    onPrimaryContainer = OffWhiteText,
    surfaceVariant = TigersLightNavy,
    onSurfaceVariant = OffWhiteText
)

private val LightColorScheme = lightColorScheme(
    primary = TigersNavy,
    secondary = TigersOrange,
    tertiary = TigersGold,
    background = LightBg,
    surface = LightCard,
    onPrimary = OffWhiteText,
    onSecondary = OffWhiteText,
    onTertiary = DarkText,
    onBackground = DarkText,
    onSurface = DarkText,
    primaryContainer = TigersLightNavy,
    onPrimaryContainer = OffWhiteText,
    surfaceVariant = LightBg,
    onSurfaceVariant = DarkText
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is disabled by default to maintain the Tigers branding
    dynamicColor: Boolean = false,
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
