package io.stashapp.android.feature.player

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import io.stashapp.android.core.designsystem.theme.MetaMono
import io.stashapp.android.core.designsystem.theme.MonoSmall
import io.stashapp.android.core.designsystem.theme.ShapeSmall
import io.stashapp.android.core.designsystem.theme.SpineColors
import io.stashapp.android.core.model.Marker
import kotlinx.collections.immutable.ImmutableList

/**
 * Spine marker editor bottom sheet — SPINE-12.
 *
 * 84% height, mini-timeline at top, scrollable marker rows, "+ Add marker" footer.
 * COMPLY-01: contentWindowInsets = { WindowInsets.navigationBars } preserved.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MarkerEditorSheet(
    sheetState: SheetState,
    markers: ImmutableList<Marker>,
    positionMs: Long,
    durationMs: Long,
    onAddMarker: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SpineColors.Bg,
        contentWindowInsets = { WindowInsets.navigationBars }, // COMPLY-01 — do not remove
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.84f)
                    .padding(vertical = 8.dp),
        ) {
            // Mini timeline — Canvas with 3dp track, AccentPrimary fill, Warning marker dots
            MarkerMiniTimeline(
                markers = markers,
                positionMs = positionMs,
                durationMs = durationMs,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                        .padding(horizontal = 18.dp),
            )

            Spacer(Modifier.height(12.dp))

            // Marker list
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding =
                    androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 18.dp,
                        vertical = 4.dp,
                    ),
            ) {
                items(markers, key = { it.id }) { marker ->
                    MarkerRow(marker = marker)
                }
            }

            Spacer(Modifier.height(8.dp))

            // Footer: full-width outlined "Add marker" button
            OutlinedButton(
                onClick = onAddMarker,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp),
                shape = ShapeSmall,
                border = BorderStroke(1.dp, SpineColors.Border),
            ) {
                Text(
                    "+ Add marker at current time · ${formatDuration(positionMs)}",
                    style = MonoSmall,
                    color = SpineColors.OnSurface,
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun MarkerMiniTimeline(
    markers: ImmutableList<Marker>,
    positionMs: Long,
    durationMs: Long,
    modifier: Modifier = Modifier,
) {
    val accentPrimary = SpineColors.AccentPrimary
    val warning = SpineColors.Warning
    val bg = SpineColors.Bg

    Canvas(modifier = modifier) {
        val trackH = 3.dp.toPx()
        val centerY = size.height / 2f

        // Track background
        drawRect(
            color = Color.White.copy(alpha = 0.10f),
            topLeft = Offset(0f, centerY - trackH / 2f),
            size = Size(size.width, trackH),
        )

        // AccentPrimary fill for played portion
        val playedFraction =
            if (durationMs > 0L) {
                (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
            } else {
                0f
            }
        if (playedFraction > 0f) {
            drawRect(
                color = accentPrimary,
                topLeft = Offset(0f, centerY - trackH / 2f),
                size = Size(size.width * playedFraction, trackH),
            )
        }

        // Warning marker dots (10dp diameter, 2dp Bg border)
        val dotRadius = 5.dp.toPx()
        markers.forEach { marker ->
            val frac =
                if (durationMs > 0L) {
                    (marker.seconds * 1000.0 / durationMs.toDouble()).toFloat().coerceIn(0f, 1f)
                } else {
                    0f
                }
            val x = size.width * frac
            drawCircle(color = warning, radius = dotRadius, center = Offset(x, centerY))
            drawCircle(
                color = bg,
                radius = dotRadius - 2.dp.toPx(),
                center = Offset(x, centerY),
                style = Stroke(width = 2.dp.toPx()),
            )
        }
    }
}

@Composable
private fun MarkerRow(marker: Marker) {
    Surface(
        color = SpineColors.Surface,
        shape = ShapeSmall,
        border = BorderStroke(1.dp, SpineColors.Border),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Thumbnail placeholder: 64×36dp (URL not yet wired — future enhancement)
            Box(
                modifier =
                    Modifier
                        .size(width = 64.dp, height = 36.dp)
                        .clip(ShapeSmall)
                        .background(SpineColors.SurfaceHigh),
            )

            Spacer(Modifier.width(10.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    text = marker.title.ifBlank { "Marker" },
                    style = androidx.compose.material3.MaterialTheme.typography.titleSmall,
                    color = SpineColors.OnSurface,
                    maxLines = 1,
                )
                if (marker.primaryTagName.isNotBlank()) {
                    Text(
                        text = marker.primaryTagName,
                        style = MetaMono,
                        color = SpineColors.AccentPrimary,
                    )
                }
            }

            Spacer(Modifier.width(8.dp))

            Text(
                text = formatDuration((marker.seconds * 1000.0).toLong()),
                style = MonoSmall,
                color = SpineColors.OnSurfaceMuted,
            )

            IconButton(onClick = { /* overflow menu placeholder */ }, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Filled.MoreVert,
                    contentDescription = "More",
                    tint = SpineColors.OnSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

// formatDuration(ms: Long) is defined in PlayerTimeline.kt (same package, internal) — no need to redefine here.
