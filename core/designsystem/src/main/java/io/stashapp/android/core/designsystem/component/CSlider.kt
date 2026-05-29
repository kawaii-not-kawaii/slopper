package io.stashapp.android.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.stashapp.android.core.designsystem.theme.LocalAccentColors
import io.stashapp.android.core.designsystem.theme.MonoSmall
import io.stashapp.android.core.designsystem.theme.SpineColors

/**
 * Compact slider — full-width track (flex 1) + 60dp value bubble (right).
 * Replaces dot-row SliderPref from old SettingsScreen.
 *
 * Usage:
 * ```
 * CSlider(
 *     value = speed,
 *     onValueChange = { setSpeed(it) },
 *     valueRange = 0.5f..2f,
 *     valueLabel = "%.2f×".format(speed),
 * )
 * ```
 */
@Composable
fun CSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    valueLabel: String,
    modifier: Modifier = Modifier,
    steps: Int = 0,
) {
    val accent = LocalAccentColors.current
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            modifier = Modifier.weight(1f),
            colors =
                SliderDefaults.colors(
                    thumbColor = accent.primary,
                    activeTrackColor = accent.primary,
                    inactiveTrackColor = SpineColors.SurfaceHigh,
                    activeTickColor = SpineColors.OnSurfaceFaint,
                    inactiveTickColor = SpineColors.OnSurfaceFaint,
                ),
        )
        Spacer(Modifier.width(8.dp))
        // Value bubble: accent-8% bg, accent-25% border, 4dp radius
        Box(
            modifier =
                Modifier
                    .width(60.dp)
                    .background(
                        accent.primary.copy(alpha = 0.08f),
                        RoundedCornerShape(4.dp),
                    ).border(
                        1.dp,
                        accent.primary.copy(alpha = 0.25f),
                        RoundedCornerShape(4.dp),
                    ).padding(horizontal = 8.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = valueLabel,
                style = MonoSmall.copy(fontWeight = FontWeight.SemiBold),
                color = accent.primary,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
    }
}
