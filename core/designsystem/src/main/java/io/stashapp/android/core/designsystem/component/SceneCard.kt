package io.stashapp.android.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import io.stashapp.android.core.designsystem.theme.SpineColors

/**
 * Plex-style media card with 16:9 poster area, play overlay on hover/press,
 * metadata row beneath. Designed to sit in a LazyVerticalGrid of 2-3 columns
 * on phones.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SceneCard(
    title: String,
    screenshotUrl: String?,
    durationSeconds: Double?,
    resolution: String?,
    rating100: Int?,
    playCount: Int,
    resumeFraction: Float?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
) {
    var pressed by remember { mutableStateOf(false) }
    val overlayAlpha by animateColorAsState(
        if (pressed) Color.Black.copy(alpha = 0.85f) else Color.Transparent,
        label = "overlay",
    )

    Column(modifier = modifier) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainer,
            shape = RoundedCornerShape(10.dp),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .combinedClickable(
                        onClick = onClick,
                        onLongClick = onLongClick,
                    ),
        ) {
            Box {
                AsyncImage(
                    model = screenshotUrl,
                    contentDescription = title,
                    // Crop (not Fit) so the card fully covers — source
                    // screenshots occasionally have slightly off aspect
                    // ratios and letterboxing makes the grid feel broken.
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )

                // Bottom gradient scrim for legibility of overlaid chips
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    0.45f to Color.Transparent,
                                    1.0f to Color(0xEB0A0D12),
                                ),
                            ),
                )

                // Press overlay with play icon
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .background(overlayAlpha),
                    contentAlignment = Alignment.Center,
                ) {
                    if (pressed) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(56.dp),
                        )
                    }
                }

                // Top-right: rating
                rating100?.let { rating ->
                    RatingPill(
                        rating100 = rating,
                        modifier =
                            Modifier
                                .align(Alignment.TopEnd)
                                .padding(6.dp),
                    )
                }

                // Bottom-left: duration + resolution
                Row(
                    modifier =
                        Modifier
                            .align(Alignment.BottomStart)
                            .padding(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    durationSeconds?.takeIf { it > 0 }?.let {
                        Chip(text = formatDuration(it))
                    }
                    resolution?.let { Chip(text = it) }
                }

                // Bottom-right: play count
                if (playCount > 0) {
                    Box(
                        modifier =
                            Modifier
                                .align(Alignment.BottomEnd)
                                .padding(6.dp),
                    ) {
                        Chip(text = "▶ $playCount", accent = true)
                    }
                }

                // Resume progress bar along bottom edge (fix: fillMaxWidth(fraction) not size(width=0))
                if ((resumeFraction ?: 0f) > 0f) {
                    Box(
                        modifier =
                            Modifier
                                .align(Alignment.BottomStart)
                                .fillMaxWidth()
                                .height(2.dp)
                                .background(Color.White.copy(alpha = 0.10f)),
                    ) {
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxWidth(resumeFraction!!.coerceIn(0f, 1f))
                                    .fillMaxHeight()
                                    .background(SpineColors.AccentPrimary),
                        )
                    }
                }
            }
        }

        Spacer(Modifier.size(8.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun Chip(
    text: String,
    accent: Boolean = false,
) {
    Surface(
        color = if (accent) SpineColors.AccentPrimary else SpineColors.SurfaceTop,
        contentColor = if (accent) SpineColors.AccentOnPrimary else Color.White,
        shape = RoundedCornerShape(4.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

@Composable
private fun RatingPill(
    rating100: Int,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = SpineColors.SurfaceTop,
        contentColor = Color.White,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = null,
                tint = SpineColors.Warning,
                modifier = Modifier.size(12.dp),
            )
            Text(
                text = "%.1f".format(rating100 / 20.0),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            )
        }
    }
}

private fun formatDuration(seconds: Double): String {
    val s = seconds.toInt()
    val h = s / 3600
    val m = (s % 3600) / 60
    val sec = s % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, sec) else "%d:%02d".format(m, sec)
}

fun resolutionLabel(
    width: Int?,
    height: Int?,
): String? {
    if (width == null || height == null) return null
    val shortest = minOf(width, height)
    return when {
        shortest >= 2160 -> "4K"
        shortest >= 1440 -> "1440p"
        shortest >= 1080 -> "1080p"
        shortest >= 720 -> "720p"
        shortest >= 480 -> "480p"
        else -> "${height}p"
    }
}
