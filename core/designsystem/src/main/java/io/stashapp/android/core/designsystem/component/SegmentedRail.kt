package io.stashapp.android.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.stashapp.android.core.designsystem.theme.LocalAccentColors
import io.stashapp.android.core.designsystem.theme.MetaMono
import io.stashapp.android.core.designsystem.theme.ShapeSmall
import io.stashapp.android.core.designsystem.theme.SpineColors

/**
 * One-row selector for small, ordered sets of values.
 *
 * Usage:
 * ```
 * SegmentedRail(listOf("auto" to null, "2" to 2), selected, onSelect = { selected = it })
 * ```
 */
@Composable
fun <T> SegmentedRail(
    options: List<Pair<String, T>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = LocalAccentColors.current
    Surface(
        modifier = modifier.fillMaxWidth().heightIn(min = 44.dp).clip(ShapeSmall),
        color = SpineColors.Surface,
        shape = ShapeSmall,
        border = BorderStroke(1.dp, SpineColors.Border),
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, (label, value) ->
                val isSelected = selected == value
                val treatment = spineSelectionTreatment(isSelected)
                Box(
                    modifier =
                        Modifier
                            .weight(1f)
                            .heightIn(min = 42.dp)
                            .background(treatment.background)
                            .clickable { onSelect(value) }
                            .semantics {
                                this.selected = isSelected
                                role = Role.Button
                            },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label,
                        style = MetaMono.copy(fontSize = 10.5.sp, fontWeight = treatment.weight),
                        color = treatment.content,
                        maxLines = 1,
                    )
                    if (isSelected && index > 0) {
                        Box(
                            Modifier
                                .align(Alignment.CenterStart)
                                .height(42.dp)
                                .padding(vertical = 1.dp)
                                .background(accent.primary.copy(alpha = 0.35f))
                                .fillMaxWidth(0.003f),
                        )
                    }
                    if (isSelected && index < options.lastIndex) {
                        Box(
                            Modifier
                                .align(Alignment.CenterEnd)
                                .height(42.dp)
                                .padding(vertical = 1.dp)
                                .background(accent.primary.copy(alpha = 0.35f))
                                .fillMaxWidth(0.003f),
                        )
                    }
                }
                if (index < options.lastIndex && !isSelected && selected != options[index + 1].second) {
                    Box(Modifier.height(42.dp).fillMaxWidth(0.003f).background(SpineColors.Border))
                }
            }
        }
    }
}
