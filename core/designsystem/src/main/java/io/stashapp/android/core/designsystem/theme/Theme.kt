package io.stashapp.android.core.designsystem.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// ---- Shape tokens (Spine design system) -------------------------------------

val ShapeSmall = RoundedCornerShape(6.dp)
val ShapeMedium = RoundedCornerShape(10.dp)
val ShapeLarge = RoundedCornerShape(16.dp)
val ShapeCircle = CircleShape

// ---- Accent palette data structures (D-05) ----------------------------------

data class AccentColors(
    val primary: Color,
    val dim: Color,
    val onPrimary: Color,
)

val SageAccent = AccentColors(Color(0xFF9DC83C), Color(0xFF6E9028), Color(0xFF0B1402))
val EmberAccent = AccentColors(Color(0xFFE5A742), Color(0xFFB07B25), Color(0xFF1A0F00))
val SignalAccent = AccentColors(Color(0xFF4FD0E6), Color(0xFF2A9DB0), Color(0xFF001218))

val LocalAccentColors = compositionLocalOf { SageAccent }

fun accentForName(name: String): AccentColors =
    when (name) {
        "ember" -> EmberAccent
        "signal" -> SignalAccent
        else -> SageAccent
    }

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
fun StashTheme(
    accentName: String = "sage",
    content: @Composable () -> Unit,
) {
    val accent = remember(accentName) { accentForName(accentName) }
    val colorScheme =
        remember(accentName) {
            StashDarkColorScheme.copy(
                primary = accent.primary,
                onPrimary = accent.onPrimary,
                primaryContainer = accent.dim,
                onPrimaryContainer = accent.onPrimary,
            )
        }
    CompositionLocalProvider(LocalAccentColors provides accent) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = StashTypography,
            content = content,
        )
    }
}
