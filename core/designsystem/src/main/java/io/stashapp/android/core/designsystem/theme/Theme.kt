package io.stashapp.android.core.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

private val StashDarkColorScheme =
    darkColorScheme(
        primary = StashColors.AccentPrimary,
        onPrimary = StashColors.AccentOnPrimary,
        primaryContainer = StashColors.AccentPrimaryDim,
        onPrimaryContainer = StashColors.AccentOnPrimary,
        secondary = StashColors.AccentSecondary,
        onSecondary = Color.Black,
        secondaryContainer = StashColors.AccentSecondaryDim,
        onSecondaryContainer = Color.White,
        tertiary = StashColors.AccentSecondary,
        onTertiary = Color.Black,
        background = StashColors.SurfaceBase,
        onBackground = StashColors.OnSurface,
        surface = StashColors.SurfaceBase,
        onSurface = StashColors.OnSurface,
        surfaceVariant = StashColors.SurfaceLow,
        onSurfaceVariant = StashColors.OnSurfaceVariant,
        surfaceContainerLowest = StashColors.SurfaceBase,
        surfaceContainerLow = StashColors.SurfaceLow,
        surfaceContainer = StashColors.SurfaceLow,
        surfaceContainerHigh = StashColors.SurfaceMed,
        surfaceContainerHighest = StashColors.SurfaceHigh,
        error = StashColors.Error,
        onError = Color.White,
        outline = StashColors.OnSurfaceFaint,
        outlineVariant = StashColors.Divider,
        scrim = Color.Black,
    )

/**
 * Stash extra semantic colors not covered by Material 3.
 * Access via [LocalStashColors].current.
 */
data class StashExtraColors(
    val success: Color = StashColors.Success,
    val warning: Color = StashColors.Warning,
    val scrimStrong: Color = StashColors.ScrimStrong,
    val scrimMedium: Color = StashColors.ScrimMedium,
    val scrimSoft: Color = StashColors.ScrimSoft,
    val surfaceHighest: Color = StashColors.SurfaceHighest,
    val onSurfaceMuted: Color = StashColors.OnSurfaceMuted,
)

val LocalStashColors = staticCompositionLocalOf { StashExtraColors() }

@Composable
fun StashTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalStashColors provides StashExtraColors()) {
        MaterialTheme(
            colorScheme = StashDarkColorScheme,
            typography = StashTypography,
            content = content,
        )
    }
}
