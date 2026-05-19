package io.stashapp.android.core.designsystem.component

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import io.stashapp.android.core.designsystem.theme.MetaMono
import io.stashapp.android.core.designsystem.theme.ShapeMedium
import io.stashapp.android.core.designsystem.theme.SpineColors

/**
 * Spine resume card — 130×88dp thumbnail (left) + metadata (right).
 *
 * Shows "RESUME" label, title, studio/remaining text, and a 3dp progress bar.
 * Used on the Home screen and Library for scenes with partial watch progress.
 */
@Composable
fun SpineResumeCard(
    thumbnailUrl: String,
    title: String,
    studio: String?,
    remainingLabel: String,
    progress: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = SpineColors.Surface,
        shape = ShapeMedium,
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, SpineColors.Border, ShapeMedium)
            .clip(ShapeMedium)
            .clickable(onClick = onClick),
    ) {
        Row(
            verticalAlignment = Alignment.Top,
        ) {
            // Thumbnail — 130×88dp
            Box(
                modifier = Modifier
                    .width(130.dp)
                    .height(88.dp),
                contentAlignment = Alignment.Center,
            ) {
                AsyncImage(
                    model = thumbnailUrl,
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(topStart = 10.dp, bottomStart = 10.dp)),
                )
                // Frosted glass play button overlay
                val playBgModifier = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Modifier.graphicsLayer {
                        renderEffect = BlurEffect(12f, 12f, TileMode.Clamp)
                    }
                } else {
                    Modifier
                }
                Box(
                    modifier = playBgModifier
                        .size(32.dp)
                        .background(
                            color = Color.Black.copy(alpha = 0.60f),
                            shape = RoundedCornerShape(50),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = "Resume",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            // Metadata column
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                // "RESUME" label
                Text(
                    text = "RESUME",
                    style = MetaMono.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                    ),
                    color = SpineColors.AccentPrimary,
                )

                // Title
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = SpineColors.OnSurface,
                )

                // Studio · remaining
                val metaText = buildString {
                    studio?.let { append(it) }
                    if (!studio.isNullOrBlank()) append(" · ")
                    append(remainingLabel)
                }
                Text(
                    text = metaText,
                    style = MetaMono,
                    color = SpineColors.OnSurfaceMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                // Progress bar — 3dp height, 1.5dp corner radius
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(1.5.dp))
                        .background(Color.White.copy(alpha = 0.08f)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress.coerceIn(0f, 1f))
                            .fillMaxHeight()
                            .background(SpineColors.AccentPrimary),
                    )
                }
            }
        }
    }
}

// Bring `sp` in scope as a local extension (avoids top-level import conflict with
// the MetaMono copy parameter, since MetaMono already carries a TextStyle).
private val Int.sp get() = androidx.compose.ui.unit.TextUnit(this.toFloat(), androidx.compose.ui.unit.TextUnitType.Sp)
