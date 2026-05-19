package io.stashapp.android.core.ui.nav

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.stashapp.android.core.designsystem.theme.SpineColors

/**
 * A minimal customization sheet — tap to include an item in the bottom bar.
 *
 * MVP scope: toggle visibility only. Reordering (drag-and-drop) is a natural
 * follow-up once the core set stabilizes; for now the on-screen order follows
 * the canonical [MainNavItems.All] order.
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
        containerColor = SpineColors.Surface,
        contentWindowInsets = { WindowInsets.navigationBars },
    ) {
        Column(
            Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                "Bottom bar items",
                style = MaterialTheme.typography.titleSmall,
                color = SpineColors.OnSurface,
            )
            Text(
                "Pick up to $maxVisible — the rest live in the More menu.",
                style = MaterialTheme.typography.labelSmall,
                color = SpineColors.OnSurfaceMuted,
            )
            Spacer(Modifier.size(8.dp))

            MainNavItems.All.forEach { item ->
                val isSelected = item.id in selected
                val atCap = selected.size >= maxVisible && !isSelected
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
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
                            ),
                    )
                    Icon(
                        item.iconOutlined,
                        contentDescription = null,
                        tint = if (atCap) SpineColors.OnSurfaceFaint else SpineColors.OnSurface,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.size(12.dp))
                    Text(
                        item.label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (atCap) SpineColors.OnSurfaceFaint else SpineColors.OnSurface,
                    )
                }
            }

            Spacer(Modifier.size(8.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onDismiss) { Text("Cancel") }
                TextButton(onClick = {
                    // Preserve canonical ordering of [All] so the bar looks
                    // stable regardless of toggle order.
                    val ordered = MainNavItems.All.map { it.id }.filter { it in selected }
                    onApply(ordered.ifEmpty { MainNavItems.DefaultVisibleIds })
                    onDismiss()
                }) {
                    Text("Apply", color = SpineColors.AccentPrimary)
                }
            }
            Spacer(Modifier.size(8.dp))
        }
    }
}
