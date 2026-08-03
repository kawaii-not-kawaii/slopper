package io.stashapp.android.core.designsystem.component

import android.os.Build
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.unit.dp
import io.stashapp.android.core.designsystem.theme.SpineColors

val LocalPrivacyBlurEnabled = compositionLocalOf { true }

/**
 * Obscures remote imagery, with an opaque fallback where render-effect blur is unavailable.
 *
 * Usage:
 * ```
 * AsyncImage(..., modifier = Modifier.privacyBlur(enabled))
 * ```
 */
fun Modifier.privacyBlur(enabled: Boolean): Modifier {
    if (!enabled) return this
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        blur(20.dp)
    } else {
        val scrim = SpineColors.SurfaceHigh
        drawWithContent { drawRect(color = scrim) }
    }
}
