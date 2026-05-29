package io.stashapp.android.feature.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.stashapp.android.core.data.prefs.PlayerPreferences
import io.stashapp.android.core.designsystem.component.CSlider
import io.stashapp.android.core.designsystem.component.DRow
import io.stashapp.android.core.designsystem.component.DRowStacked
import io.stashapp.android.core.designsystem.theme.LocalAccentColors
import io.stashapp.android.core.designsystem.theme.MetaMono
import io.stashapp.android.core.designsystem.theme.ShapeSmall
import io.stashapp.android.core.designsystem.theme.SpaceGrotesk
import io.stashapp.android.core.designsystem.theme.SpineColors
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsPlaybackScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val speed by viewModel.playerPrefs.defaultPlaybackSpeed.collectAsStateWithLifecycle(1.0f)
    val aspect by viewModel.playerPrefs.defaultAspectRatio.collectAsStateWithLifecycle("fit")
    val autoPlay by viewModel.playerPrefs.autoPlayNext.collectAsStateWithLifecycle(true)
    val autoRotate by viewModel.uiPrefs.autoRotatePlayer.collectAsStateWithLifecycle(true)
    val doubleTap by viewModel.playerPrefs.doubleTapSeekSeconds.collectAsStateWithLifecycle(10)
    val seekMs by viewModel.playerPrefs.seekMsPerPx.collectAsStateWithLifecycle(120f)
    val chapterThumb by viewModel.playerPrefs.showChapterThumbnails.collectAsStateWithLifecycle(false)
    val resumeThreshold by viewModel.playerPrefs.resumeThresholdSeconds.collectAsStateWithLifecycle(2)
    val completionThreshold by viewModel.playerPrefs.completionThresholdPercent.collectAsStateWithLifecycle(85)
    val skipIntro by viewModel.playerPrefs.skipIntroSeconds.collectAsStateWithLifecycle(0)
    val lockControls by viewModel.playerPrefs.lockControlsOnIdle.collectAsStateWithLifecycle(false)
    val showCodecBadge by viewModel.playerPrefs.showCodecBadge.collectAsStateWithLifecycle(true)
    val showQueuePos by viewModel.playerPrefs.showQueuePosition.collectAsStateWithLifecycle(false)
    val haptics by viewModel.playerPrefs.hapticsOnSeek.collectAsStateWithLifecycle(false)

    LazyColumn(contentPadding = PaddingValues(bottom = 100.dp)) {
        item {
            TopAppBar(
                title = {
                    Text(
                        "Playback",
                        style =
                            TextStyle(
                                fontFamily = SpaceGrotesk,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                            ),
                        color = SpineColors.OnSurface,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back",
                            tint = SpineColors.OnSurface,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SpineColors.Bg),
            )
        }

        // Group 1 — Defaults
        item {
            DetailGroup(
                title = "Defaults",
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            ) {
                DRowStacked(label = "Default speed", hint = "Speed applied when a video starts") {
                    ChipRow(
                        options =
                            listOf(
                                "0.5×" to 0.5f,
                                "0.75×" to 0.75f,
                                "1×" to 1.0f,
                                "1.25×" to 1.25f,
                                "1.5×" to 1.5f,
                                "2×" to 2.0f,
                            ),
                        selected = speed,
                        onSelect = { v -> viewModel.setPlayer { setDefaultPlaybackSpeed(v) } },
                    )
                }
                HorizontalDivider(color = SpineColors.Border, thickness = 1.dp)
                DRowStacked(label = "Aspect ratio") {
                    ChipRow(
                        options =
                            listOf(
                                "Fit" to "fit",
                                "Crop" to "crop",
                                "Stretch" to "stretch",
                            ),
                        selected = aspect,
                        onSelect = { v -> viewModel.setPlayer { setDefaultAspectRatio(v) } },
                    )
                }
                HorizontalDivider(color = SpineColors.Border, thickness = 1.dp)
                DRow(
                    label = "Auto-play next",
                    hint = "Automatically play the next scene",
                    trailing = {
                        SpineSwitch(checked = autoPlay) {
                            viewModel.setPlayer { setAutoPlayNext(it) }
                        }
                    },
                )
                HorizontalDivider(color = SpineColors.Border, thickness = 1.dp)
                DRow(
                    label = "Auto-rotate on play",
                    hint = "Lock landscape when video starts",
                    trailing = {
                        SpineSwitch(checked = autoRotate) {
                            viewModel.setUi { setAutoRotatePlayer(it) }
                        }
                    },
                )
            }
        }

        // Group 2 — Seeking
        item {
            DetailGroup(
                title = "Seeking",
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            ) {
                DRowStacked(label = "Double-tap seek", hint = "Seconds to skip on double-tap") {
                    CSlider(
                        value = doubleTap.toFloat(),
                        onValueChange = { viewModel.setPlayer { setDoubleTapSeekSeconds(it.roundToInt()) } },
                        valueRange = 5f..60f,
                        valueLabel = "${doubleTap}s",
                    )
                }
                HorizontalDivider(color = SpineColors.Border, thickness = 1.dp)
                DRowStacked(label = "Scrub sensitivity", hint = "Milliseconds of video per pixel dragged") {
                    CSlider(
                        value = seekMs,
                        onValueChange = { viewModel.setPlayer { setSeekMsPerPx(it) } },
                        valueRange = PlayerPreferences.SEEK_MS_PER_PX_MIN..PlayerPreferences.SEEK_MS_PER_PX_MAX,
                        valueLabel = "${seekMs.roundToInt()} ms/px",
                    )
                }
                HorizontalDivider(color = SpineColors.Border, thickness = 1.dp)
                DRow(
                    label = "Chapter thumbnails",
                    hint = "Show chapter thumbnail on timeline scrub",
                    trailing = {
                        SpineSwitch(checked = chapterThumb) {
                            viewModel.setPlayer { setShowChapterThumbnails(it) }
                        }
                    },
                )
            }
        }

        // Group 3 — Resume & skip
        item {
            DetailGroup(
                title = "Resume & skip",
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            ) {
                DRowStacked(
                    label = "Resume threshold",
                    hint = "Minimum watch time before position is saved",
                ) {
                    CSlider(
                        value = resumeThreshold.toFloat(),
                        onValueChange = { viewModel.setPlayer { setResumeThresholdSeconds(it.roundToInt()) } },
                        valueRange = 0f..30f,
                        valueLabel = "${resumeThreshold}s",
                    )
                }
                HorizontalDivider(color = SpineColors.Border, thickness = 1.dp)
                DRowStacked(
                    label = "Completion threshold",
                    hint = "Watch% to mark as complete",
                ) {
                    CSlider(
                        value = completionThreshold.toFloat(),
                        onValueChange = { viewModel.setPlayer { setCompletionThresholdPercent(it.roundToInt()) } },
                        valueRange = 50f..100f,
                        valueLabel = "$completionThreshold%",
                    )
                }
                HorizontalDivider(color = SpineColors.Border, thickness = 1.dp)
                DRowStacked(
                    label = "Skip intro",
                    hint = "Seconds to skip at video start (0 = off)",
                ) {
                    CSlider(
                        value = skipIntro.toFloat(),
                        onValueChange = { viewModel.setPlayer { setSkipIntroSeconds(it.roundToInt()) } },
                        valueRange = 0f..120f,
                        valueLabel = if (skipIntro == 0) "Off" else "${skipIntro}s",
                    )
                }
            }
        }

        // Group 4 — Player chrome (Power user)
        item {
            DetailGroup(
                title = "Player chrome",
                badge = "Power user",
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            ) {
                DRow(
                    label = "Lock controls on idle",
                    hint = "Hide controls after N seconds of inactivity",
                    trailing = {
                        SpineSwitch(checked = lockControls) {
                            viewModel.setPlayer { setLockControlsOnIdle(it) }
                        }
                    },
                )
                HorizontalDivider(color = SpineColors.Border, thickness = 1.dp)
                DRow(
                    label = "Show codec badge",
                    hint = "Display HW/SW codec indicator in player chips",
                    trailing = {
                        SpineSwitch(checked = showCodecBadge) {
                            viewModel.setPlayer { setShowCodecBadge(it) }
                        }
                    },
                )
                HorizontalDivider(color = SpineColors.Border, thickness = 1.dp)
                DRow(
                    label = "Show queue position",
                    hint = "Show current / total queue in top bar",
                    trailing = {
                        SpineSwitch(checked = showQueuePos) {
                            viewModel.setPlayer { setShowQueuePosition(it) }
                        }
                    },
                )
                HorizontalDivider(color = SpineColors.Border, thickness = 1.dp)
                DRow(
                    label = "Haptics on seek",
                    hint = "Vibrate on chapter marks and seek taps",
                    trailing = {
                        SpineSwitch(checked = haptics) {
                            viewModel.setPlayer { setHapticsOnSeek(it) }
                        }
                    },
                )
            }
        }
    }
}

// ---- Shared detail-page primitives ------------------------------------------

/**
 * Themed Switch using accent palette colors.
 */
@Composable
internal fun SpineSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val accent = LocalAccentColors.current
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        colors =
            SwitchDefaults.colors(
                checkedThumbColor = accent.onPrimary,
                checkedTrackColor = accent.primary,
                uncheckedThumbColor = SpineColors.OnSurfaceMuted,
                uncheckedTrackColor = SpineColors.SurfaceHigh,
            ),
    )
}

/**
 * Horizontal scrollable chip row.
 * Works with any value type via [Any] equality.
 */
@Composable
internal fun <T> ChipRow(
    options: List<Pair<String, T>>,
    selected: T,
    onSelect: (T) -> Unit,
) {
    val accent = LocalAccentColors.current
    androidx.compose.foundation.lazy.LazyRow(
        horizontalArrangement =
            androidx.compose.foundation.layout.Arrangement
                .spacedBy(6.dp),
    ) {
        items(options.size) { i ->
            val (label, value) = options[i]
            val isActive = value == selected
            Surface(
                onClick = { onSelect(value) },
                shape = ShapeSmall,
                color = if (isActive) accent.primary.copy(alpha = 0.12f) else SpineColors.SurfaceHigh,
                border =
                    BorderStroke(
                        1.dp,
                        if (isActive) accent.primary else SpineColors.Border,
                    ),
            ) {
                Text(
                    text = label,
                    style =
                        MetaMono.copy(
                            fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                        ),
                    color = if (isActive) accent.primary else SpineColors.OnSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                )
            }
        }
    }
}

/**
 * Container for a group of settings rows on a detail page.
 * Shows an optional title above and an optional badge next to the title.
 */
@Composable
internal fun DetailGroup(
    title: String? = null,
    badge: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val accent = LocalAccentColors.current
    Column(modifier = modifier) {
        if (title != null || badge != null) {
            androidx.compose.foundation.layout.Row(
                modifier = Modifier.padding(bottom = 8.dp, start = 2.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            ) {
                if (title != null) {
                    Text(
                        text = title.uppercase(),
                        style =
                            MetaMono.copy(
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 1.sp,
                            ),
                        color = SpineColors.OnSurfaceMuted,
                    )
                }
                if (badge != null) {
                    androidx.compose.foundation.layout.Spacer(
                        modifier = Modifier.padding(start = 8.dp),
                    )
                    Text(
                        text = badge,
                        style = MetaMono,
                        color = accent.primary,
                        modifier =
                            Modifier
                                .background(
                                    color = accent.primary.copy(alpha = 0.10f),
                                    shape = ShapeSmall,
                                ).padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }
        }
        Surface(
            color = SpineColors.Surface,
            shape = ShapeSmall,
            border = BorderStroke(1.dp, SpineColors.Border),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                content()
            }
        }
    }
}
