package io.stashapp.android.feature.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.getValue
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.ScreenLockRotation
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.stashapp.android.core.designsystem.theme.StashColors
import io.stashapp.android.core.model.Marker
import io.stashapp.android.core.model.RepeatMode
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.StateFlow

/** Height/corner shared by all three top-right chips so they read as a row. */
private val TopChipHeight = 36.dp
private val TopChipShape = RoundedCornerShape(10.dp)
private val TopChipMinWidth = 52.dp

// ---- MX-Player-style controls ----------------------------------------------

@Composable
internal fun PlayerControls(
    title: String,
    subtitle: String?,
    queuePosition: String?,
    isPlaying: Boolean,
    shuffled: Boolean,
    repeatMode: RepeatMode,
    positionFlow: StateFlow<PlayerPositionState>,
    markers: ImmutableList<Marker>,
    playbackSpeed: Float,
    canSkipPrev: Boolean,
    canSkipNext: Boolean,
    codecLabel: String,
    rotationLocked: Boolean,
    resizeMode: Int,
    doubleTapSeekSec: Int,
    showRemaining: Boolean,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onShuffle: () -> Unit,
    onRepeat: () -> Unit,
    onSeek: (Long) -> Unit,
    onSeekBy: (Long) -> Unit,
    onPip: () -> Unit,
    onClose: () -> Unit,
    onLock: () -> Unit,
    onToggleRotationLock: () -> Unit,
    onCycleResize: () -> Unit,
    onCycleSpeed: () -> Unit,
    onToggleRemaining: () -> Unit,
    onScreenshot: () -> Unit,
) {
    // Note: outer wrap in PlayerScreen Box(safeDrawingPadding()) already
    // handles system-bar inset for the entire control overlay layer per
    // COMPLY-01 (RESEARCH §F2). Internal Column padding(sysBarsPadding) lines
    // were removed to avoid double-padding (see 02.1-DEVIATIONS.md DEV-02).

    Box(
        Modifier
            .fillMaxSize()
            .drawBehind {
                val topH = 260f
                val bottomH = 320f
                // F2 = 95% opacity — heavier scrim so controls stay legible
                // over bright scenes without a full blackout.
                drawRect(
                    brush =
                        Brush.verticalGradient(
                            0f to Color(0xF2000000),
                            0.7f to Color(0x80000000),
                            1f to Color.Transparent,
                            endY = topH,
                        ),
                    size = Size(size.width, topH),
                )
                drawRect(
                    brush =
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            0.3f to Color(0x80000000),
                            1f to Color(0xF2000000),
                            startY = 0f,
                            endY = bottomH,
                        ),
                    topLeft = Offset(0f, size.height - bottomH),
                    size = Size(size.width, bottomH),
                )
            },
    ) {
        // Top block: main bar + secondary toggles row
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 2.dp)
                .align(Alignment.TopStart),
        ) {
            // Main top row
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose, modifier = Modifier.size(44.dp)) {
                    Icon(
                        Icons.Filled.KeyboardArrowDown,
                        contentDescription = "Close",
                        tint = Color.White,
                        modifier = Modifier.size(30.dp),
                    )
                }
                Column(
                    Modifier.weight(1f).padding(start = 4.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            title.ifBlank { "Playing" },
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White,
                            maxLines = 1,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        queuePosition?.let {
                            Spacer(Modifier.size(6.dp))
                            Text(
                                it,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                color = StashColors.AccentSecondary,
                            )
                        }
                    }
                    subtitle?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.labelSmall,
                            color = StashColors.OnSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                }
                // Right-side status chips — uniform height + corner radius so
                // they read as a single chip row regardless of content width.
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CodecBadge(codecLabel)
                    SpeedPill(playbackSpeed, onCycleSpeed)
                    PipChip(onClick = onPip)
                }
            }

            // Secondary toggles row (MX-Player style dense utility strip)
            Row(
                Modifier.padding(start = 4.dp, top = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                UtilityIconButton(
                    icon = if (rotationLocked) Icons.Filled.ScreenLockRotation else Icons.Filled.ScreenRotation,
                    active = rotationLocked,
                    contentDescription = "Rotation lock",
                    onClick = onToggleRotationLock,
                )
                UtilityIconButton(
                    icon = Icons.Filled.AspectRatio,
                    active = false,
                    contentDescription = "Aspect: ${resizeLabel(resizeMode)}",
                    onClick = onCycleResize,
                )
                UtilityIconButton(
                    icon = Icons.Filled.PhotoCamera,
                    active = false,
                    contentDescription = "Screenshot",
                    onClick = onScreenshot,
                )
                UtilityIconButton(
                    icon = Icons.Filled.Shuffle,
                    active = shuffled,
                    contentDescription = "Shuffle",
                    onClick = onShuffle,
                )
                UtilityIconButton(
                    icon =
                        when (repeatMode) {
                            RepeatMode.ONE -> Icons.Filled.RepeatOne
                            else -> Icons.Filled.Repeat
                        },
                    active = repeatMode != RepeatMode.OFF,
                    contentDescription = "Repeat",
                    onClick = onRepeat,
                )
            }
        }

        // Bottom block: progress + transport row — compact, tight vertical spacing
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .align(Alignment.BottomStart),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            TimelineBar(
                positionFlow = positionFlow,
                markers = markers,
                showRemaining = showRemaining,
                onSeek = onSeek,
                onToggleRightLabel = onToggleRemaining,
            )

            // Transport row — zero gap above, sits tight against the progress bar
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                UtilityIconButton(
                    icon = Icons.Filled.LockOpen,
                    active = false,
                    contentDescription = "Lock screen",
                    onClick = onLock,
                )
                UtilityIconButton(
                    icon = Icons.Filled.Replay10,
                    active = false,
                    contentDescription = "Back ${doubleTapSeekSec}s",
                    onClick = { onSeekBy(-doubleTapSeekSec * 1000L) },
                )
                TransportIcon(
                    icon = Icons.Filled.SkipPrevious,
                    size = 36.dp,
                    enabled = canSkipPrev,
                    onClick = onPrevious,
                    contentDescription = "Previous",
                )
                PlayPauseFlat(isPlaying = isPlaying, onClick = onPlayPause)
                TransportIcon(
                    icon = Icons.Filled.SkipNext,
                    size = 36.dp,
                    enabled = canSkipNext,
                    onClick = onNext,
                    contentDescription = "Next",
                )
                UtilityIconButton(
                    icon = Icons.Filled.Forward10,
                    active = false,
                    contentDescription = "Forward ${doubleTapSeekSec}s",
                    onClick = { onSeekBy(doubleTapSeekSec * 1000L) },
                )
                UtilityIconButton(
                    icon = Icons.Filled.AspectRatio,
                    active = false,
                    contentDescription = "Aspect",
                    onClick = onCycleResize,
                )
            }
        }
    }
}

@Composable
private fun PlayPauseFlat(
    isPlaying: Boolean,
    onClick: () -> Unit,
) {
    // Flat, no circle wrapper — matches MX Player. AnimatedContent gives the
    // icon swap a bit of motion so it doesn't feel too austere.
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(64.dp),
    ) {
        AnimatedContent(
            targetState = isPlaying,
            transitionSpec = {
                (scaleIn(tween(160), initialScale = 0.7f) + fadeIn(tween(160))) togetherWith
                    (scaleOut(tween(160), targetScale = 1.2f) + fadeOut(tween(160)))
            },
            label = "play-pause-icon",
        ) { playing ->
            Icon(
                if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (playing) "Pause" else "Play",
                tint = Color.White,
                modifier = Modifier.size(52.dp),
            )
        }
    }
}

@Composable
private fun TransportIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    size: androidx.compose.ui.unit.Dp,
    enabled: Boolean,
    onClick: () -> Unit,
    contentDescription: String,
) {
    IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(48.dp)) {
        Icon(
            icon,
            contentDescription = contentDescription,
            tint = if (enabled) Color.White else Color.White.copy(alpha = 0.3f),
            modifier = Modifier.size(size),
        )
    }
}

@Composable
private fun UtilityIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    active: Boolean,
    contentDescription: String,
    onClick: () -> Unit,
    iconSize: androidx.compose.ui.unit.Dp = 22.dp,
) {
    IconButton(onClick = onClick, modifier = Modifier.size(36.dp)) {
        Icon(
            icon,
            contentDescription = contentDescription,
            tint = if (active) StashColors.AccentPrimary else Color.White,
            modifier = Modifier.size(iconSize),
        )
    }
}

@Composable
private fun CodecBadge(label: String) {
    Surface(
        color = StashColors.AccentSecondary.copy(alpha = 0.18f),
        contentColor = StashColors.AccentSecondary,
        shape = TopChipShape,
        border =
            BorderStroke(
                width = 1.dp,
                color = StashColors.AccentSecondary.copy(alpha = 0.6f),
            ),
        modifier =
            Modifier
                .height(TopChipHeight)
                .widthIn(min = TopChipMinWidth),
    ) {
        Box(
            Modifier.padding(horizontal = 10.dp).fillMaxHeight(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            )
        }
    }
}

@Composable
private fun SpeedPill(
    speed: Float,
    onClick: () -> Unit,
) {
    val active = speed != 1f
    Surface(
        color = if (active) StashColors.AccentPrimary else StashColors.SurfaceHigh.copy(alpha = 0.85f),
        contentColor = if (active) StashColors.AccentOnPrimary else Color.White,
        shape = TopChipShape,
        border =
            if (!active) {
                BorderStroke(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.25f),
                )
            } else {
                null
            },
        modifier =
            Modifier
                .height(TopChipHeight)
                .widthIn(min = TopChipMinWidth)
                .clickable(onClick = onClick),
    ) {
        Row(
            Modifier.padding(horizontal = 10.dp).fillMaxHeight(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterHorizontally),
        ) {
            Icon(
                Icons.Filled.Speed,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
            Text(
                PlayerViewModel.formatSpeed(speed),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            )
        }
    }
}

@Composable
private fun PipChip(onClick: () -> Unit) {
    Surface(
        color = StashColors.SurfaceHigh.copy(alpha = 0.85f),
        contentColor = Color.White,
        shape = TopChipShape,
        border =
            BorderStroke(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.25f),
            ),
        modifier =
            Modifier
                .height(TopChipHeight)
                .widthIn(min = TopChipMinWidth)
                .clickable(onClick = onClick),
    ) {
        Box(Modifier.fillMaxHeight().padding(horizontal = 10.dp), contentAlignment = Alignment.Center) {
            Icon(
                Icons.Filled.PictureInPicture,
                contentDescription = "Picture in Picture",
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

// ---- Transient pieces -------------------------------------------------------

internal data class ScrubPreview(
    val targetMs: Long,
    val deltaMs: Long,
)

internal data class StepSeek(
    val totalSeconds: Int,
    val generation: Int,
)

@Composable
internal fun StepSeekCallout(
    step: StepSeek,
    alignment: Alignment,
) {
    val progress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(380, easing = FastOutLinearInEasing),
        label = "arc-progress-${step.generation}",
    )
    Box(
        Modifier.fillMaxSize().padding(horizontal = 24.dp),
        contentAlignment = alignment,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(160.dp)) {
                val baseRadius = size.minDimension * 0.25f
                val maxExpansion = size.minDimension * 0.12f
                val alpha = (1f - progress) * 0.30f
                val strokeColor = Color.White.copy(alpha = alpha)
                listOf(0f, 0.4f).forEach { offsetFrac ->
                    val r = baseRadius + (progress + offsetFrac) * maxExpansion
                    drawArc(
                        color = strokeColor,
                        startAngle = -130f,
                        sweepAngle = 260f,
                        useCenter = false,
                        topLeft =
                            Offset(
                                (size.width - 2 * r) / 2f,
                                (size.height - 2 * r) / 2f,
                            ),
                        size = Size(2 * r, 2 * r),
                        style = Stroke(width = 2f),
                    )
                }
            }
            val label = if (step.totalSeconds >= 0) "+${step.totalSeconds}s" else "${step.totalSeconds}s"
            Text(
                label,
                color = Color.White,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
            )
        }
    }
}

// Helper used only within PlayerControls.kt (mirrors the one in PlayerTimeline.kt
// but scoped here to avoid cross-file calls from the resize label in secondary row)
private fun resizeLabel(mode: Int): String =
    when (mode) {
        androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT -> "Fit"
        androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> "Crop"
        androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FILL -> "Stretch"
        else -> "Fit"
    }
