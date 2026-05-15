package com.example.pract3.ui.theme

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

private val DarkColorScheme = darkColorScheme(
    primary = Sage80,
    secondary = Mist80,
    tertiary = Sand80,
    background = Color(0xFF18211F),
    surface = Color(0xFF21302B),
    onPrimary = Ink,
    onSecondary = Ink,
    onTertiary = Ink,
    onBackground = Color(0xFFF4F1EA),
    onSurface = Color(0xFFF4F1EA)
)

private val LightColorScheme = lightColorScheme(
    primary = Sage40,
    secondary = Mist40,
    tertiary = Sand40,
    background = Fog,
    surface = Cloud,
    surfaceVariant = Moss,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Ink,
    onSurface = Ink,
    onSurfaceVariant = Color(0xFF5F6E67),
    outline = Color(0xFF9CAC9F),
    outlineVariant = Color(0xFFD2D9D3)
)

@Composable
fun Pract3Theme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
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
