package io.stashapp.android.feature.player

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.stashapp.android.core.designsystem.theme.StashColors
import io.stashapp.android.core.model.Marker
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.StateFlow

// ---- Timeline ---------------------------------------------------------------

@Composable
internal fun TimelineBar(
    positionFlow: StateFlow<PlayerPositionState>,
    markers: ImmutableList<Marker>,
    showRemaining: Boolean,
    onSeek: (Long) -> Unit,
    onToggleRightLabel: () -> Unit,
) {
    // Subscribe to position here — this keeps the 250ms tick's recomposition
    // scoped to just this subtree.
    val pos by positionFlow.collectAsStateWithLifecycle()
    val positionMs = pos.positionMs
    val durationMs = pos.durationMs
    val bufferedMs = pos.bufferedMs

    val density = LocalDensity.current
    var dragFraction by remember { mutableStateOf<Float?>(null) }
    var dragging by remember { mutableStateOf(false) }

    val hasDuration = durationMs > 0
    val playedFraction = if (hasDuration) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
    val bufferedFraction = if (hasDuration) (bufferedMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
    val displayFraction = dragFraction ?: playedFraction

    val trackHeight by animateDpAsState(if (dragging) 10.dp else 7.dp, label = "track-h")

    Row(
        Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            formatDuration(if (dragFraction != null) (dragFraction!! * durationMs).toLong() else positionMs),
            style = MaterialTheme.typography.labelSmall,
            color = if (dragging) StashColors.AccentPrimary else StashColors.OnSurfaceVariant,
            modifier = Modifier.size(width = 44.dp, height = 14.dp),
        )
        Box(
            Modifier
                .weight(1f)
                .height(28.dp)
                .pointerInput(durationMs) {
                    if (!hasDuration) return@pointerInput
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            dragging = true
                            dragFraction = (offset.x / size.width).coerceIn(0f, 1f)
                        },
                        onDragEnd = {
                            dragFraction?.let { onSeek((it * durationMs).toLong()) }
                            dragFraction = null
                            dragging = false
                        },
                        onDragCancel = {
                            dragFraction = null
                            dragging = false
                        },
                        onHorizontalDrag = { change, _ ->
                            dragFraction = (change.position.x / size.width).coerceIn(0f, 1f)
                        },
                    )
                }.pointerInput(durationMs) {
                    if (!hasDuration) return@pointerInput
                    detectTapGestures(onTap = { offset ->
                        val frac = (offset.x / size.width).coerceIn(0f, 1f)
                        onSeek((frac * durationMs).toLong())
                    })
                },
            contentAlignment = Alignment.CenterStart,
        ) {
            Canvas(Modifier.fillMaxWidth().height(trackHeight)) {
                val th = size.height
                val corner = th / 2
                drawRoundRect(
                    color = StashColors.OnSurfaceFaint,
                    size = Size(size.width, th),
                    cornerRadius =
                        androidx.compose.ui.geometry
                            .CornerRadius(corner, corner),
                )
                if (bufferedFraction > 0f) {
                    drawRoundRect(
                        color = Color(0x59A8B0C0),
                        size = Size(size.width * bufferedFraction, th),
                        cornerRadius =
                            androidx.compose.ui.geometry
                                .CornerRadius(corner, corner),
                    )
                }
                if (displayFraction > 0f) {
                    drawRoundRect(
                        color = StashColors.AccentPrimary,
                        size = Size(size.width * displayFraction, th),
                        cornerRadius =
                            androidx.compose.ui.geometry
                                .CornerRadius(corner, corner),
                    )
                }
            }

            if (hasDuration && markers.isNotEmpty()) {
                Canvas(Modifier.fillMaxWidth().height(28.dp)) {
                    val dotRadius = with(density) { 2.5.dp.toPx() }
                    val ringRadius = with(density) { 4.dp.toPx() }
                    val centerY = size.height / 2f
                    markers.forEach { marker ->
                        val frac = (marker.seconds / (durationMs / 1000.0)).toFloat().coerceIn(0f, 1f)
                        val x = size.width * frac
                        drawCircle(
                            color = StashColors.AccentPrimary.copy(alpha = 0.6f),
                            radius = ringRadius,
                            center = Offset(x, centerY),
                            style = Stroke(width = 1.5f),
                        )
                        drawCircle(
                            color = StashColors.OnSurface,
                            radius = dotRadius,
                            center = Offset(x, centerY),
                        )
                    }
                }
            }

            if (hasDuration) {
                val thumbR by animateDpAsState(if (dragging) 10.dp else 7.dp, label = "thumb-r")
                Canvas(Modifier.fillMaxWidth().height(28.dp)) {
                    val r = with(density) { thumbR.toPx() }
                    drawCircle(
                        color = StashColors.AccentPrimary,
                        radius = r,
                        center = Offset(size.width * displayFraction, size.height / 2f),
                    )
                }
            }
        }
        // Right-side time label — tap to toggle total vs remaining
        val remainingMs =
            (durationMs - (dragFraction?.let { (it * durationMs).toLong() } ?: positionMs))
                .coerceAtLeast(0L)
        val rightText =
            when {
                !hasDuration -> "--:--"
                showRemaining -> "-${formatDuration(remainingMs)}"
                else -> formatDuration(durationMs)
            }
        Text(
            rightText,
            style = MaterialTheme.typography.labelSmall,
            color = StashColors.OnSurfaceVariant,
            modifier =
                Modifier
                    .size(width = 56.dp, height = 14.dp)
                    .clickable(onClick = onToggleRightLabel),
        )
    }
}

@Composable
internal fun ScrubPreviewCard(
    preview: ScrubPreview,
    durationMs: Long,
) {
    val target = formatDuration(preview.targetMs)
    val total = durationMs.takeIf { it > 0 }?.let { formatDuration(it) }
    val deltaSeconds = preview.deltaMs / 1000
    val delta = if (deltaSeconds >= 0) "+${deltaSeconds}s" else "${deltaSeconds}s"

    Surface(
        color = StashColors.SurfaceHigh,
        contentColor = Color.White,
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                target,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = Color.White,
            )
            if (total != null) {
                Text(
                    "of $total",
                    style = MaterialTheme.typography.labelSmall,
                    color = StashColors.OnSurfaceVariant,
                )
            }
            androidx.compose.foundation.layout.Spacer(Modifier.size(4.dp))
            Text(
                delta,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                color = StashColors.AccentPrimary,
            )
        }
    }
}

@Composable
internal fun BannerPill(text: String) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = StashColors.SurfaceHigh.copy(alpha = 0.92f),
        modifier =
            Modifier.drawBehind {
                drawRect(
                    color = StashColors.AccentPrimary,
                    topLeft = Offset.Zero,
                    size = Size(3.dp.toPx(), size.height),
                )
            },
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
            color = StashColors.AccentPrimary,
            modifier = Modifier.padding(start = 16.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
        )
    }
}

// ---- Helpers ----------------------------------------------------------------

internal fun formatDuration(ms: Long): String {
    val s = (ms / 1000).toInt().coerceAtLeast(0)
    val h = s / 3600
    val m = (s % 3600) / 60
    val sec = s % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, sec) else "%d:%02d".format(m, sec)
}
