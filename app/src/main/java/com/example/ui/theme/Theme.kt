package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = SafetyOrange,
    secondary = SafetyYellow,
    tertiary = CyberBlue,
    background = CyberDark,
    surface = CyberCard,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onTertiary = Color.White,
    onBackground = CustomLavText,
    onSurface = CustomLavText,
    outline = BorderColor
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false, // Use light theme by default for Clean Minimalism
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}
