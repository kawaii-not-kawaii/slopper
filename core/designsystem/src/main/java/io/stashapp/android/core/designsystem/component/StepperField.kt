package io.stashapp.android.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.stashapp.android.core.designsystem.theme.MetaMono
import io.stashapp.android.core.designsystem.theme.ShapeSmall
import io.stashapp.android.core.designsystem.theme.SpineColors

/**
 * Compact integer stepper with an explicit unset state.
 *
 * Usage:
 * ```
 * StepperField(count, onChange = { count = it }, max = 50)
 * ```
 */
@Composable
fun StepperField(
    value: Int?,
    onChange: (Int?) -> Unit,
    modifier: Modifier = Modifier,
    min: Int = 1,
    max: Int = 99,
    valueLabel: (Int) -> String = { "≥ $it" },
    anyLabel: String = "any",
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = SpineColors.Surface,
        shape = ShapeSmall,
        border = BorderStroke(1.dp, SpineColors.Border),
    ) {
        Row(modifier = Modifier.height(44.dp), verticalAlignment = Alignment.CenterVertically) {
            StepperButton(
                description = "Decrease",
                enabled = value != null,
                onClick = {
                    value?.let { current -> onChange(if (current <= min) null else current - 1) }
                },
            ) {
                Icon(
                    imageVector = Icons.Outlined.Remove,
                    contentDescription = null,
                    tint = if (value == null) SpineColors.OnSurfaceFaint else SpineColors.OnSurfaceVariant,
                    modifier = Modifier.width(12.dp),
                )
            }
            Box(Modifier.width(1.dp).height(44.dp), contentAlignment = Alignment.Center) {
                Surface(Modifier.fillMaxWidth(), color = SpineColors.Border) {}
            }
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text(
                    text = value?.let(valueLabel) ?: anyLabel,
                    style =
                        MetaMono.copy(
                            fontSize = 11.sp,
                            fontWeight = if (value == null) FontWeight.Normal else FontWeight.SemiBold,
                        ),
                    color = if (value == null) SpineColors.OnSurfaceMuted else SpineColors.OnSurface,
                )
            }
            Box(Modifier.width(1.dp).height(44.dp), contentAlignment = Alignment.Center) {
                Surface(Modifier.fillMaxWidth(), color = SpineColors.Border) {}
            }
            StepperButton(
                description = "Increase",
                enabled = value == null || value < max,
                onClick = { onChange(if (value == null) min else (value + 1).coerceAtMost(max)) },
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = null,
                    tint = SpineColors.OnSurfaceVariant,
                    modifier = Modifier.width(12.dp),
                )
            }
        }
    }
}

@Composable
private fun StepperButton(
    description: String,
    enabled: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .width(34.dp)
                .height(44.dp)
                .clickable(enabled = enabled, onClick = onClick)
                .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}
