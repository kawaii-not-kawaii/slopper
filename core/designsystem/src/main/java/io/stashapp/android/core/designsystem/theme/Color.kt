package io.stashapp.android.core.designsystem.theme

import androidx.compose.ui.graphics.Color

/**
 * Plex-inspired dark palette tailored for Stash.
 *
 * Design goals:
 *  - Deep, slightly-blue charcoal as the primary surface (not pure black — better
 *    for OLED burn-in avoidance and less harsh over long viewing sessions).
 *  - Warm amber accent that evokes classic film / projector vibe and reads as
 *    Stash-brand-adjacent without copying Plex orange.
 *  - High-contrast text with subtle layering via 3 elevation tiers.
 */
object StashColors {
    // Surfaces — graduated from deepest (scaffold) to highest (dialogs/modals)
    val SurfaceBase = Color(0xFF0B0E13) // app background
    val SurfaceLow = Color(0xFF121722) // cards, rails
    val SurfaceMed = Color(0xFF1A2030) // elevated cards, sheets
    val SurfaceHigh = Color(0xFF232B3D) // dialogs, menus
    val SurfaceHighest = Color(0xFF2E3750)

    // Text
    val OnSurface = Color(0xFFE8EBF2)
    val OnSurfaceVariant = Color(0xFFA8B0C0)
    val OnSurfaceMuted = Color(0xFF6C7488)
    val OnSurfaceFaint = Color(0xFF3F4656)

    // Brand / accent — warm amber-orange
    val AccentPrimary = Color(0xFFF0A037)
    val AccentPrimaryDim = Color(0xFFC68129)
    val AccentOnPrimary = Color(0xFF1A0F00)

    // Secondary — cool teal for interactive elements like progress + links
    val AccentSecondary = Color(0xFF4FC8D9)
    val AccentSecondaryDim = Color(0xFF338996)

    // Semantic
    val Success = Color(0xFF5DBB63)
    val Warning = Color(0xFFE8C547)
    val Error = Color(0xFFE85757)

    // Overlays & dividers
    val Divider = Color(0x1FFFFFFF)
    val ScrimStrong = Color(0xD9000000)
    val ScrimMedium = Color(0x99000000)
    val ScrimSoft = Color(0x66000000)
}
