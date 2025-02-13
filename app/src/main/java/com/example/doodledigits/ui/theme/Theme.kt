package com.example.doodledigits.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// Пастельні кольори
val PastelBlue = Color(0xFFA7C7E7)
val PastelPink = Color(0xFFF8C8DC)
val PastelGreen = Color(0xFFB6E2D3)
val PastelYellow = Color(0xFFFBE7A1)
val PastelPurple = Color(0xFFD5B4E2)
val SoftWhite = Color(0xFFFFF8F0)
val SoftGray = Color(0xFFD9D9D9)

// Темна тема
private val DarkColorScheme = darkColorScheme(
    primary = PastelPurple,
    secondary = PastelPink,
    background = Color.Black,
    surface = Color.DarkGray,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White
)

// Світла тема
private val LightColorScheme = lightColorScheme(
    primary = PastelBlue,
    secondary = PastelPink,
    background = SoftWhite,
    surface = SoftGray,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = Color.Black,
    onSurface = Color.Black
)



@Composable
fun DoodleDigitsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
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
