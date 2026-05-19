package io.stashapp.android.core.designsystem.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// ---- Shape tokens (Spine design system) -------------------------------------

val ShapeSmall  = RoundedCornerShape(6.dp)
val ShapeMedium = RoundedCornerShape(10.dp)
val ShapeLarge  = RoundedCornerShape(16.dp)
val ShapeCircle = CircleShape

// ---- M3 color scheme wired to SpineColors -----------------------------------

private val StashDarkColorScheme =
    darkColorScheme(
        primary = SpineColors.AccentPrimary,
        onPrimary = SpineColors.AccentOnPrimary,
        primaryContainer = SpineColors.AccentPrimaryDim,
        onPrimaryContainer = SpineColors.AccentOnPrimary,
        secondary = SpineColors.AccentCool,
        onSecondary = Color.Black,
        secondaryContainer = SpineColors.AccentCoolDim,
        onSecondaryContainer = Color.White,
        tertiary = SpineColors.AccentCool,
        onTertiary = Color.Black,
        background = SpineColors.Bg,
        onBackground = SpineColors.OnSurface,
        surface = SpineColors.Bg,
        onSurface = SpineColors.OnSurface,
        surfaceVariant = SpineColors.Surface,
        onSurfaceVariant = SpineColors.OnSurfaceVariant,
        surfaceContainerLowest = SpineColors.Bg,
        surfaceContainerLow = SpineColors.Surface,
        surfaceContainer = SpineColors.Surface,
        surfaceContainerHigh = SpineColors.SurfaceHigh,
        surfaceContainerHighest = SpineColors.SurfaceTop,
        error = SpineColors.Error,
        onError = Color.White,
        outline = SpineColors.OnSurfaceFaint,
        outlineVariant = SpineColors.Border,
        scrim = Color.Black,
    )

@Composable
fun StashTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = StashDarkColorScheme,
        typography = StashTypography,
        content = content,
    )
}
