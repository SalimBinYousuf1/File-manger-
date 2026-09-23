package com.example.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = SalimAccentDark,
    onPrimary = SalimOledBlack,
    primaryContainer = SalimSurfaceHighlightDark,
    onPrimaryContainer = SalimTextPrimaryDark,
    background = SalimOledBlack,
    onBackground = SalimTextPrimaryDark,
    surface = SalimSurfaceDark,
    onSurface = SalimTextPrimaryDark,
    surfaceVariant = SalimCardDark,
    onSurfaceVariant = SalimTextSecondaryDark,
    outline = SalimSubtleBorderDark,
    error = SalimDestructiveDark,
    onError = SalimOledBlack
)

private val LightColorScheme = lightColorScheme(
    primary = SalimAccent,
    onPrimary = SalimWhite,
    primaryContainer = SalimSurfaceHighlightLight,
    onPrimaryContainer = SalimTextPrimaryLight,
    background = SalimOffWhite,
    onBackground = SalimTextPrimaryLight,
    surface = SalimWhite,
    onSurface = SalimTextPrimaryLight,
    surfaceVariant = SalimOffWhite,
    onSurfaceVariant = SalimTextSecondaryLight,
    outline = SalimSubtleBorderLight,
    error = SalimDestructive,
    onError = SalimWhite
)

// Continuous squircle-like curvature tokens
val SalimShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun SalimTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = SalimTypography,
        shapes = SalimShapes,
        content = content
    )
}
