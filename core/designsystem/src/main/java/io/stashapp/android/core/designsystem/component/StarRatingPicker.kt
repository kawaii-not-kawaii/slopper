package io.stashapp.android.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.stashapp.android.core.designsystem.theme.LocalAccentColors
import io.stashapp.android.core.designsystem.theme.MonoSmall
import io.stashapp.android.core.designsystem.theme.ShapeSmall
import io.stashapp.android.core.designsystem.theme.SpineColors

/**
 * Five-star minimum-rating selector backed by the domain's 0..100 scale.
 *
 * Usage:
 * ```
 * StarRatingPicker(filter.minRating100, onChange = { rating = it })
 * ```
 */
@Composable
fun StarRatingPicker(
    rating100: Int?,
    onChange: (Int?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = LocalAccentColors.current
    Surface(
        modifier = modifier,
        color = SpineColors.Surface,
        shape = ShapeSmall,
        border = BorderStroke(1.dp, SpineColors.Border),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(5) { index ->
                val fullValue = (index + 1) * 20
                val filled = rating100 != null && rating100 >= fullValue
                Icon(
                    imageVector = if (filled) Icons.Filled.Star else Icons.Outlined.StarBorder,
                    contentDescription = "${index + 1} stars minimum",
                    tint = if (filled) accent.primary else SpineColors.OnSurfaceFaint,
                    modifier =
                        Modifier
                            .size(24.dp)
                            .semantics {
                                selected = rating100 == fullValue
                                role = Role.Button
                            }.pointerInput(rating100) {
                                detectTapGestures { offset ->
                                    val selectedValue = fullValue - if (offset.x < size.width / 2f) 10 else 0
                                    onChange(if (rating100 == selectedValue) null else selectedValue)
                                }
                            }.padding(3.5.dp),
                )
            }
            Spacer(Modifier.weight(1f))
            val isSet = rating100 != null
            Box(
                modifier =
                    Modifier
                        .background(
                            if (isSet) accent.primary.copy(alpha = 0.08f) else SpineColors.Surface,
                            RoundedCornerShape(4.dp),
                        ).border(
                            1.dp,
                            if (isSet) accent.primary.copy(alpha = 0.25f) else SpineColors.Border,
                            RoundedCornerShape(4.dp),
                        ).padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Text(
                    text = rating100?.let { "★ %.1f+".format(it / 20f) } ?: "any",
                    style = MonoSmall.copy(fontWeight = if (isSet) FontWeight.SemiBold else FontWeight.Normal),
                    color = if (isSet) accent.primary else SpineColors.OnSurfaceVariant,
                )
            }
        }
    }
}
