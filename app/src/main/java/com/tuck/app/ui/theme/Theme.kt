package com.tuck.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.tuck.app.domain.repository.AppTheme
import com.tuck.app.domain.repository.TuckThemeFlavor

object TuckTheme {
    val colors: TuckColors
        @Composable
        @ReadOnlyComposable
        get() = LocalTuckColors.current

    val spacing: TuckSpacing
        @Composable
        @ReadOnlyComposable
        get() = LocalTuckSpacing.current

    val shapes: TuckShapes
        @Composable
        @ReadOnlyComposable
        get() = LocalTuckShapes.current

    val typography: TuckTypographyExtension
        @Composable
        @ReadOnlyComposable
        get() = LocalTuckTypographyExtension.current
}

@Composable
fun TuckTheme(
    themeSetting: AppTheme = AppTheme.SYSTEM,
    themeFlavor: TuckThemeFlavor = TuckThemeFlavor.LINEN,
    darkTheme: Boolean = when (themeSetting) {
        AppTheme.SYSTEM -> isSystemInDarkTheme()
        AppTheme.LIGHT -> false
        AppTheme.DARK -> true
    },
    content: @Composable () -> Unit
) {
    val tuckColors = getTuckColors(themeFlavor, darkTheme)
    val tuckSpacing = TuckSpacing()
    val tuckShapes = TuckShapes()
    val tuckTypography = TuckTypographyExtension()

    val materialColorScheme = if (darkTheme) {
        darkColorScheme(
            primary = tuckColors.accent,
            onPrimary = tuckColors.textOnAccent,
            primaryContainer = tuckColors.accentContainer,
            onPrimaryContainer = tuckColors.accentLight,
            secondary = tuckColors.accentLight,
            onSecondary = tuckColors.textOnAccent,
            background = tuckColors.background,
            onBackground = tuckColors.textPrimary,
            surface = tuckColors.surface,
            onSurface = tuckColors.textPrimary,
            surfaceVariant = tuckColors.surfaceVariant,
            onSurfaceVariant = tuckColors.textSecondary,
            outline = tuckColors.border
        )
    } else {
        lightColorScheme(
            primary = tuckColors.accent,
            onPrimary = tuckColors.textOnAccent,
            primaryContainer = tuckColors.accentContainer,
            onPrimaryContainer = tuckColors.accent,
            secondary = tuckColors.accentLight,
            onSecondary = tuckColors.textOnAccent,
            background = tuckColors.background,
            onBackground = tuckColors.textPrimary,
            surface = tuckColors.surface,
            onSurface = tuckColors.textPrimary,
            surfaceVariant = tuckColors.surfaceVariant,
            onSurfaceVariant = tuckColors.textSecondary,
            outline = tuckColors.border
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = tuckColors.background.toArgb()
            window.navigationBarColor = tuckColors.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    CompositionLocalProvider(
        LocalTuckColors provides tuckColors,
        LocalTuckSpacing provides tuckSpacing,
        LocalTuckShapes provides tuckShapes,
        LocalTuckTypographyExtension provides tuckTypography
    ) {
        MaterialTheme(
            colorScheme = materialColorScheme,
            typography = TuckTypography,
            content = content
        )
    }
}
