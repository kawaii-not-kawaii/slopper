package io.stashapp.android.core.designsystem.theme

import androidx.compose.ui.graphics.Color

/**
 * Spine design system color tokens.
 *
 * Design goals (v5 Spine direction):
 *  - Near-black navy base (Bg) — softer than pure black for OLED readability.
 *  - 4-tier surface stack: Bg → Surface → SurfaceHigh → SurfaceTop.
 *  - Sage green accent (AccentPrimary) replaces warm amber — matches the
 *    design handoff direction in design_handoff_slopper_spine/.
 *  - Cool blue secondary (AccentCool) replaces teal.
 *  - Border/BorderStrong for subtle rule lines instead of a full-opacity Divider.
 */
object SpineColors {
    // Surfaces — 4-tier graduated elevation stack
    val Bg = Color(0xFF0A0D12) // app scaffold / page background
    val Surface = Color(0xFF11151C) // cards, rails, sheets
    val SurfaceHigh = Color(0xFF1A2030) // elevated cards, menus
    val SurfaceTop = Color(0xFF232B3D) // dialogs, toasts, top-layer chips

    // Text
    val OnSurface = Color(0xFFEAEEF6)
    val OnSurfaceVariant = Color(0xFF8C95A8)
    val OnSurfaceMuted = Color(0xFF525B6E)
    val OnSurfaceFaint = Color(0xFF2F3645)

    // Brand / accent — sage green
    val AccentPrimary = Color(0xFF9DC83C)
    val AccentPrimaryDim = Color(0xFF6E9028)
    val AccentOnPrimary = Color(0xFF0B1402)

    // Secondary — cool blue
    val AccentCool = Color(0xFF7FB6FF)
    val AccentCoolDim = Color(0xFF4A75B6)

    // Semantic
    val Warning = Color(0xFFFFCC44)
    val Error = Color(0xFFFF5860)
    val Success = Color(0xFF5DBB63)

    // Borders (alpha-based)
    val Border = Color(0xFFA0B4DC).copy(alpha = 0.10f)
    val BorderStrong = Color(0xFFA0B4DC).copy(alpha = 0.22f)
}
