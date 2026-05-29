package io.stashapp.android.core.ui.nav

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import io.stashapp.android.core.designsystem.theme.MetaMono
import io.stashapp.android.core.designsystem.theme.ShapeSmall
import io.stashapp.android.core.designsystem.theme.SpineColors

/**
 * A Spine-styled customization sheet — toggle + reorder bottom bar items.
 *
 * D-12 (SPINE-12): 76% height, AccentPrimary checkboxes, info banner, Cancel/Apply footer.
 *
 * COMPLY-01 preserved: contentWindowInsets = { WindowInsets.navigationBars } must stay.
 * maxVisible = 4 cap preserved.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavCustomizeSheet(
    sheetState: SheetState,
    visibleIds: List<String>,
    onApply: (List<String>) -> Unit,
    onDismiss: () -> Unit,
) {
    var selected by remember { mutableStateOf(visibleIds.toSet()) }

    // Keep the bar usable: cap visible items at 4 (+ the always-on More tab = 5).
    val maxVisible = 4

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SpineColors.Bg,
        contentWindowInsets = { WindowInsets.navigationBars }, // COMPLY-01 — do not remove
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.76f)
                .padding(horizontal = 18.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            // Spine info banner
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(SpineColors.AccentPrimary.copy(alpha = 0.08f), ShapeSmall)
                        .border(1.dp, SpineColors.AccentPrimary.copy(alpha = 0.20f), ShapeSmall)
                        .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Outlined.Info,
                    contentDescription = null,
                    tint = SpineColors.AccentPrimary,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    "Select up to $maxVisible tabs. Drag to reorder.",
                    style = MetaMono,
                    color = SpineColors.OnSurfaceVariant,
                )
            }

            Spacer(Modifier.size(4.dp))

            MainNavItems.All.forEach { item ->
                val isSelected = item.id in selected
                val atCap = selected.size >= maxVisible && !isSelected

                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .then(if (atCap) Modifier.alpha(0.4f) else Modifier),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Checkbox — 4dp radius, AccentPrimary when checked
                    Checkbox(
                        checked = isSelected,
                        enabled = !atCap,
                        onCheckedChange = { checked ->
                            selected = if (checked) selected + item.id else selected - item.id
                        },
                        colors =
                            CheckboxDefaults.colors(
                                checkedColor = SpineColors.AccentPrimary,
                                checkmarkColor = SpineColors.AccentOnPrimary,
                                uncheckedColor = SpineColors.Border,
                            ),
                    )
                    // Leading icon — AccentPrimary when selected
                    Icon(
                        item.iconOutlined,
                        contentDescription = null,
                        tint = if (isSelected) SpineColors.AccentPrimary else SpineColors.OnSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.size(12.dp))
                    Text(
                        item.label,
                        style = androidx.compose.material3.MaterialTheme.typography.titleSmall,
                        color = if (isSelected) SpineColors.OnSurface else SpineColors.OnSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            // Footer: Cancel ghost + Apply AccentPrimary filled
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    shape = ShapeSmall,
                    border = androidx.compose.foundation.BorderStroke(1.dp, SpineColors.Border),
                ) {
                    Text("Cancel", color = SpineColors.OnSurface)
                }
                Button(
                    onClick = {
                        // Preserve canonical ordering of [All] so the bar looks
                        // stable regardless of toggle order.
                        val ordered = MainNavItems.All.map { it.id }.filter { it in selected }
                        onApply(ordered.ifEmpty { MainNavItems.DefaultVisibleIds })
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f),
                    shape = ShapeSmall,
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = SpineColors.AccentPrimary,
                            contentColor = SpineColors.AccentOnPrimary,
                        ),
                ) {
                    Text("Apply")
                }
            }

            Spacer(Modifier.size(8.dp))
        }
    }
}
