package io.stashapp.android.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.stashapp.android.core.designsystem.theme.LocalAccentColors
import io.stashapp.android.core.designsystem.theme.MetaMono
import io.stashapp.android.core.designsystem.theme.ShapeSmall
import io.stashapp.android.core.designsystem.theme.SpineColors

/**
 * Compact, accessible Spine selection chip.
 *
 * Usage:
 * ```
 * SpineChip("Newest", selected = true, onClick = {})
 * ```
 */
@Composable
fun SpineChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leading: @Composable (() -> Unit)? = null,
    dashed: Boolean = false,
) {
    val treatment = spineSelectionTreatment(selected)
    val borderColor = if (dashed) SpineColors.BorderStrong else treatment.border
    val background = if (dashed) Color.Transparent else treatment.background
    val contentColor = if (dashed) SpineColors.OnSurfaceMuted else treatment.content
    val fontWeight = if (dashed) FontWeight.Normal else treatment.weight

    Box(
        modifier =
            modifier
                .heightIn(min = 44.dp)
                .semantics {
                    this.selected = selected
                    role = Role.Button
                },
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier.clickable(onClick = onClick),
            color = background,
            shape = ShapeSmall,
            border = BorderStroke(1.dp, borderColor),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                leading?.invoke()
                Text(
                    text = label,
                    style = MetaMono.copy(fontSize = 11.sp, fontWeight = fontWeight),
                    color = contentColor,
                )
            }
        }
    }
}

/**
 * Three-state Spine chip cycling any, yes and no.
 *
 * Usage:
 * ```
 * SpineTriStateChip("Organized", state, onChange = { state = it })
 * ```
 */
@Composable
fun SpineTriStateChip(
    label: String,
    state: Boolean?,
    onChange: (Boolean?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = LocalAccentColors.current
    val treatment = spineSelectionTreatment(state == true)
    val next =
        when (state) {
            null -> true
            true -> false
            false -> null
        }
    val stateDescription =
        when (state) {
            true -> "yes"
            false -> "no"
            null -> "any"
        }
    val background = if (state == false) SpineColors.Error.copy(alpha = 0.10f) else treatment.background
    val border = if (state == false) SpineColors.Error.copy(alpha = 0.35f) else treatment.border
    val content = if (state == false) SpineColors.Error else treatment.content
    val weight = if (state == null) FontWeight.Normal else FontWeight.SemiBold

    Box(
        modifier =
            modifier
                .heightIn(min = 44.dp)
                .semantics {
                    selected = state != null
                    contentDescription = "$label, $stateDescription"
                    role = Role.Button
                },
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier.clickable { onChange(next) },
            color = background,
            shape = ShapeSmall,
            border = BorderStroke(1.dp, border),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                when (state) {
                    true -> Box(Modifier.width(5.dp).height(5.dp).background(accent.primary, RoundedCornerShape(50)))
                    false -> Box(Modifier.width(8.dp).height(1.5.dp).background(SpineColors.Error))
                    null -> Unit
                }
                Text(
                    text = label,
                    style = MetaMono.copy(fontSize = 11.sp, fontWeight = weight),
                    color = content,
                )
            }
        }
    }
}
