package com.example.menza.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = MediumGreen,
    secondary = DarkGreen,
    tertiary = LightGreen,
    background = Color(0xFF121212),
    surface = DarkGreen,
    error = RejectRed,
    onPrimary = TextWhite,
    onSecondary = TextWhite,
    onTertiary = TextBlack,
    onBackground = TextWhite,
    onSurface = TextWhite,
    onError = TextWhite
)

private val LightColorScheme = lightColorScheme(
    primary = LightGreen,
    secondary = MediumGreen,
    tertiary = DarkGreen,
    background = BackgroundGreen,
    surface = LightGreen,
    error = RejectRed,
    onPrimary = TextBlack,
    onSecondary = TextWhite,
    onTertiary = TextWhite,
    onBackground = TextBlack,
    onSurface = TextBlack,
    onError = TextWhite
)

@Composable
fun MenzaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
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
