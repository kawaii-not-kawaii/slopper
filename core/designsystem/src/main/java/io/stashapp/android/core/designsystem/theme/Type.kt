package io.stashapp.android.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import io.stashapp.android.core.designsystem.R
import androidx.compose.ui.text.googlefonts.Font as GoogleFontLoader

// ---- Google Fonts provider --------------------------------------------------

private val provider =
    GoogleFont.Provider(
        providerAuthority = "com.google.android.gms.fonts",
        providerPackage = "com.google.android.gms",
        certificates = R.array.com_google_android_gms_fonts_certs,
    )

// ---- Font families ----------------------------------------------------------

val SpaceGrotesk =
    FontFamily(
        GoogleFontLoader(GoogleFont("Space Grotesk"), provider, weight = FontWeight.Light),
        GoogleFontLoader(GoogleFont("Space Grotesk"), provider, weight = FontWeight.Normal),
        GoogleFontLoader(GoogleFont("Space Grotesk"), provider, weight = FontWeight.Medium),
        GoogleFontLoader(GoogleFont("Space Grotesk"), provider, weight = FontWeight.SemiBold),
        GoogleFontLoader(GoogleFont("Space Grotesk"), provider, weight = FontWeight.Bold),
    )

val JetBrainsMono =
    FontFamily(
        GoogleFontLoader(GoogleFont("JetBrains Mono"), provider, weight = FontWeight.Normal),
        GoogleFontLoader(GoogleFont("JetBrains Mono"), provider, weight = FontWeight.Medium),
        GoogleFontLoader(GoogleFont("JetBrains Mono"), provider, weight = FontWeight.SemiBold),
    )

// ---- Named mono styles (used directly in Spine components) ------------------

/** 10sp monospace — metadata labels, codec badges, timestamps */
val MetaMono =
    TextStyle(
        fontFamily = JetBrainsMono,
        fontSize = 10.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.6.sp,
    )

/** 9sp monospace — smallest annotations, progress sub-labels */
val MonoSmall =
    TextStyle(
        fontFamily = JetBrainsMono,
        fontSize = 9.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.5.sp,
    )

// ---- M3 Typography slots wired to Space Grotesk / JetBrains Mono ------------

val StashTypography =
    Typography(
        displayLarge =
            TextStyle(
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.Bold,
                fontSize = 40.sp,
                lineHeight = 48.sp,
                letterSpacing = (-0.5).sp,
            ),
        displayMedium =
            TextStyle(
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp,
                lineHeight = 40.sp,
            ),
        headlineLarge =
            TextStyle(
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.SemiBold,
                fontSize = 28.sp,
                lineHeight = 36.sp,
                letterSpacing = (-0.8).sp,
            ),
        headlineMedium =
            TextStyle(
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.SemiBold,
                fontSize = 22.sp,
                lineHeight = 28.sp,
                letterSpacing = (-0.5).sp,
            ),
        headlineSmall =
            TextStyle(
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp,
                lineHeight = 24.sp,
            ),
        titleLarge =
            TextStyle(
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp,
                lineHeight = 24.sp,
                letterSpacing = (-0.4).sp,
            ),
        titleMedium =
            TextStyle(
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                letterSpacing = (-0.2).sp,
            ),
        titleSmall =
            TextStyle(
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                letterSpacing = (-0.1).sp,
            ),
        bodyLarge =
            TextStyle(
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.Normal,
                fontSize = 15.sp,
                lineHeight = 22.sp,
            ),
        bodyMedium =
            TextStyle(
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.Normal,
                fontSize = 13.sp,
                lineHeight = 19.sp,
            ),
        bodySmall =
            TextStyle(
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.Normal,
                fontSize = 12.sp,
                lineHeight = 16.sp,
            ),
        labelLarge =
            TextStyle(
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                letterSpacing = (-0.1).sp,
            ),
        labelMedium =
            TextStyle(
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                letterSpacing = (-0.1).sp,
            ),
        // labelSmall uses JetBrains Mono — fills the MetaMono role in the M3 slot system
        labelSmall =
            TextStyle(
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.Normal,
                fontSize = 10.sp,
                lineHeight = 13.sp,
                letterSpacing = 0.5.sp,
            ),
    )
