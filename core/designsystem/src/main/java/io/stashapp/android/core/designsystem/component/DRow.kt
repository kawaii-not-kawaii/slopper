package io.stashapp.android.core.designsystem.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.stashapp.android.core.designsystem.theme.SpineColors

/**
 * Detail-page row primitive (two modes).
 *
 * Inline mode ([DRow]): label + optional hint flex-1, trailing widget right.
 * Stacked mode ([DRowStacked]): label + hint on top; full-width body below.
 *
 * Each DRow inside a DetailGroup is separated from its neighbours by a 1dp
 * Border divider — the DetailGroup container is responsible for the dividers.
 */
@Composable
fun DRow(
    label: String,
    hint: String? = null,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = SpineColors.OnSurface,
            )
            if (hint != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = hint,
                    style = MaterialTheme.typography.labelMedium,
                    color = SpineColors.OnSurfaceMuted,
                )
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(12.dp))
            trailing()
        }
    }
}

@Composable
fun DRowStacked(
    label: String,
    hint: String? = null,
    modifier: Modifier = Modifier,
    body: @Composable () -> Unit,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(14.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = SpineColors.OnSurface,
        )
        if (hint != null) {
            Spacer(Modifier.height(2.dp))
            Text(
                text = hint,
                style = MaterialTheme.typography.labelMedium,
                color = SpineColors.OnSurfaceMuted,
            )
        }
        Spacer(Modifier.height(10.dp))
        body()
    }
}
