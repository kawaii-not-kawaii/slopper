package io.stashapp.android.feature.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.stashapp.android.core.designsystem.theme.MetaMono
import io.stashapp.android.core.designsystem.theme.ShapeSmall
import io.stashapp.android.core.designsystem.theme.SpineColors
import io.stashapp.android.core.designsystem.theme.LocalAccentColors

/**
 * Spine right-anchored player settings panel — SPINE-12 (D-11).
 *
 * Slides in from the right edge (40% landscape width) with AnimatedVisibility.
 * Uses drawBehind for the left-edge-only border (NOT .border() which adds all 4 sides).
 * Speed chips, audio, subtitle placeholders, and up-next queue stub.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun PlayerSettingsPanel(
    visible: Boolean,
    playbackSpeed: Float,
    onSpeedChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
        exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
        modifier = modifier,
    ) {
        val borderStrong = SpineColors.BorderStrong
        val accent = LocalAccentColors.current

        Box(
            modifier =
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.40f)
                    .background(Color(0xF20B0F16))
                    .drawBehind {
                        // Left border only — drawBehind draws a single edge (NOT .border() which adds all 4 sides)
                        drawLine(
                            color = borderStrong,
                            start = Offset(0f, 0f),
                            end = Offset(0f, size.height),
                            strokeWidth = 1.dp.toPx(),
                        )
                    }.padding(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Speed section header
                androidx.compose.material3.Text(
                    "Speed",
                    style = MetaMono.copy(fontWeight = FontWeight.SemiBold),
                    color = SpineColors.OnSurfaceMuted,
                )

                // Speed chip row
                val speeds = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    speeds.forEach { speed ->
                        val isSelected = kotlin.math.abs(speed - playbackSpeed) < 0.01f
                        Box(
                            modifier =
                                Modifier
                                    .clip(ShapeSmall)
                                    .background(
                                        if (isSelected) {
                                            accent.primary
                                        } else {
                                            SpineColors.SurfaceTop
                                        },
                                    ).clickable { onSpeedChange(speed) }
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            androidx.compose.material3.Text(
                                text = PlayerViewModel.formatSpeed(speed),
                                style = MetaMono,
                                color =
                                    if (isSelected) {
                                        accent.onPrimary
                                    } else {
                                        SpineColors.OnSurface
                                    },
                            )
                        }
                    }
                }

                // Audio section
                androidx.compose.material3.Text(
                    "Audio",
                    style = MetaMono.copy(fontWeight = FontWeight.SemiBold),
                    color = SpineColors.OnSurfaceMuted,
                )
                androidx.compose.material3.Text(
                    "No audio tracks",
                    style = MetaMono,
                    color = SpineColors.OnSurfaceFaint,
                )

                // Subtitles section
                androidx.compose.material3.Text(
                    "Subtitles",
                    style = MetaMono.copy(fontWeight = FontWeight.SemiBold),
                    color = SpineColors.OnSurfaceMuted,
                )
                androidx.compose.material3.Text(
                    "No subtitles",
                    style = MetaMono,
                    color = SpineColors.OnSurfaceFaint,
                )
            }
        }
    }
}
