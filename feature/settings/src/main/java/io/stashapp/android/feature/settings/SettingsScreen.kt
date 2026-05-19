package io.stashapp.android.feature.settings

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.stashapp.android.core.data.prefs.PlayerPreferences
import io.stashapp.android.core.data.prefs.UiPreferences
import io.stashapp.android.core.designsystem.theme.JetBrainsMono
import io.stashapp.android.core.designsystem.theme.MetaMono
import io.stashapp.android.core.designsystem.theme.ShapeSmall
import io.stashapp.android.core.designsystem.theme.SpaceGrotesk
import io.stashapp.android.core.designsystem.theme.SpineColors
import io.stashapp.android.feature.player.CodecCapabilities
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onDisconnected: () -> Unit,
    onBrowsePerformers: () -> Unit = {},
    onBrowseStudios: () -> Unit = {},
    onBrowseTags: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val pp = viewModel.playerPrefs
    val up = viewModel.uiPrefs

    // Player prefs
    val seekMsPerPx by pp.seekMsPerPx.collectAsStateWithLifecycle(initialValue = PlayerPreferences.DEFAULT_SEEK_MS_PER_PX)
    val doubleTapSec by pp.doubleTapSeekSeconds.collectAsStateWithLifecycle(initialValue = PlayerPreferences.DEFAULT_DOUBLE_TAP_SEEK_SEC)
    val defaultSpeed by pp.defaultPlaybackSpeed.collectAsStateWithLifecycle(initialValue = PlayerPreferences.DEFAULT_SPEED)
    val autoPlayNext by pp.autoPlayNext.collectAsStateWithLifecycle(initialValue = PlayerPreferences.DEFAULT_AUTO_PLAY_NEXT)
    val resumeThreshold by pp.resumeThresholdSeconds.collectAsStateWithLifecycle(initialValue = PlayerPreferences.DEFAULT_RESUME_THRESHOLD)
    val completionThreshold by pp.completionThresholdPercent.collectAsStateWithLifecycle(
        initialValue = PlayerPreferences.DEFAULT_COMPLETION_THRESHOLD,
    )
    val skipIntro by pp.skipIntroSeconds.collectAsStateWithLifecycle(initialValue = PlayerPreferences.DEFAULT_SKIP_INTRO)
    val bufferPreset by pp.videoBufferPreset.collectAsStateWithLifecycle(initialValue = PlayerPreferences.DEFAULT_BUFFER_PRESET)
    val aspectRatio by pp.defaultAspectRatio.collectAsStateWithLifecycle(initialValue = PlayerPreferences.DEFAULT_ASPECT_RATIO)
    val decoderPref by pp.decoderPreference.collectAsStateWithLifecycle(initialValue = PlayerPreferences.DEFAULT_DECODER_PREF)

    // UI prefs
    val imageCacheMb by up.imageCacheSizeMb.collectAsStateWithLifecycle(initialValue = UiPreferences.DEFAULT_IMAGE_CACHE_MB)
    val gridColumns by up.gridColumns.collectAsStateWithLifecycle(initialValue = UiPreferences.DEFAULT_GRID_COLUMNS)
    val amoled by up.amoledBlackMode.collectAsStateWithLifecycle(initialValue = UiPreferences.DEFAULT_AMOLED)
    val showRating by up.showRatingOnCards.collectAsStateWithLifecycle(initialValue = true)
    val showPlayCount by up.showPlayCountOnCards.collectAsStateWithLifecycle(initialValue = true)
    val showResolution by up.showResolutionOnCards.collectAsStateWithLifecycle(initialValue = true)
    val activityTracking by up.activityTracking.collectAsStateWithLifecycle(initialValue = true)
    val autoRotate by up.autoRotatePlayer.collectAsStateWithLifecycle(initialValue = true)

    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Scaffold(
        topBar = { TopAppBar(title = { Text("Settings") }) },
    ) { inner ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(inner)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            // ── APP ─────────────────────────────────────────────────────
            SectionHeader("App")
            // COMPLY-06: Language row — preserved from Phase 2
            SectionContainer {
                LanguageRow()
            }

            Spacer(Modifier.size(8.dp))

            // ── PLAYER ──────────────────────────────────────────────────
            SectionHeader("Player")

            SectionContainer {
                SliderPref(
                    "Scrub sensitivity",
                    "${seekMsPerPx.toInt()} ms/px",
                    seekMsPerPx,
                    PlayerPreferences.SEEK_MS_PER_PX_MIN..PlayerPreferences.SEEK_MS_PER_PX_MAX,
                ) { scope.launch { pp.setSeekMsPerPx(it) } }
                HorizontalDivider(color = SpineColors.Border, thickness = 1.dp)
                SliderPref(
                    "Double-tap seek",
                    "${doubleTapSec}s",
                    doubleTapSec.toFloat(),
                    PlayerPreferences.DOUBLE_TAP_SEEK_MIN.toFloat()..PlayerPreferences.DOUBLE_TAP_SEEK_MAX.toFloat(),
                    steps = PlayerPreferences.DOUBLE_TAP_SEEK_MAX - PlayerPreferences.DOUBLE_TAP_SEEK_MIN - 1,
                ) { scope.launch { pp.setDoubleTapSeekSeconds(it.toInt()) } }
                HorizontalDivider(color = SpineColors.Border, thickness = 1.dp)
                ChipRowPref(
                    "Default speed",
                    options = listOf("0.5x" to 0.5f, "0.75x" to 0.75f, "1x" to 1.0f, "1.25x" to 1.25f, "1.5x" to 1.5f, "2x" to 2.0f),
                    selected = defaultSpeed,
                    compareBy = { kotlin.math.abs(it - defaultSpeed) < 0.01f },
                ) { scope.launch { pp.setDefaultPlaybackSpeed(it) } }
                HorizontalDivider(color = SpineColors.Border, thickness = 1.dp)
                SwitchPref("Auto-play next", "Advance to the next item in queue", autoPlayNext) {
                    scope.launch { pp.setAutoPlayNext(it) }
                }
                HorizontalDivider(color = SpineColors.Border, thickness = 1.dp)
                SliderPref(
                    "Resume threshold",
                    "${resumeThreshold}s",
                    resumeThreshold.toFloat(),
                    0f..30f,
                    steps = 29,
                ) { scope.launch { pp.setResumeThresholdSeconds(it.toInt()) } }
                HorizontalDivider(color = SpineColors.Border, thickness = 1.dp)
                SliderPref(
                    "Completion threshold",
                    "$completionThreshold%",
                    completionThreshold.toFloat(),
                    50f..100f,
                    steps = 49,
                ) { scope.launch { pp.setCompletionThresholdPercent(it.toInt()) } }
                HorizontalDivider(color = SpineColors.Border, thickness = 1.dp)
                SliderPref(
                    "Skip intro",
                    if (skipIntro == 0) "Off" else "${skipIntro}s",
                    skipIntro.toFloat(),
                    0f..120f,
                    steps = 23,
                ) { scope.launch { pp.setSkipIntroSeconds(it.toInt()) } }
                HorizontalDivider(color = SpineColors.Border, thickness = 1.dp)
                ChipRowPref(
                    "Video buffer",
                    options = listOf("Small (15s)" to "small", "Medium (50s)" to "medium", "Large (2min)" to "large"),
                    selected = bufferPreset,
                    compareBy = { it == bufferPreset },
                ) { scope.launch { pp.setVideoBufferPreset(it) } }
                HorizontalDivider(color = SpineColors.Border, thickness = 1.dp)
                ChipRowPref(
                    "Default aspect ratio",
                    options = listOf("Fit" to "fit", "Crop" to "crop", "Stretch" to "stretch"),
                    selected = aspectRatio,
                    compareBy = { it == aspectRatio },
                ) { scope.launch { pp.setDefaultAspectRatio(it) } }
                HorizontalDivider(color = SpineColors.Border, thickness = 1.dp)
                ChipRowPref(
                    "Decoder preference",
                    options = listOf("Auto" to "auto", "Prefer HW" to "prefer_hw", "Prefer SW" to "prefer_sw"),
                    selected = decoderPref,
                    compareBy = { it == decoderPref },
                ) { scope.launch { pp.setDecoderPreference(it) } }
            }

            CodecStatusCard()

            Spacer(Modifier.size(8.dp))

            // ── CACHE ───────────────────────────────────────────────────
            SectionHeader("Cache")

            SectionContainer {
                SliderPref(
                    "Image cache size",
                    "$imageCacheMb MB",
                    imageCacheMb.toFloat(),
                    64f..512f,
                    steps = 7,
                ) { scope.launch { up.setImageCacheSizeMb(it.toInt()) } }
            }

            OutlinedButton(
                onClick = {
                    val cacheDir = context.cacheDir.resolve("image_cache")
                    cacheDir.deleteRecursively()
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp),
            ) {
                Icon(Icons.Filled.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text("Clear image cache")
            }

            Spacer(Modifier.size(8.dp))

            // ── DISPLAY ─────────────────────────────────────────────────
            SectionHeader("Display")

            SectionContainer {
                ChipRowPref(
                    "Grid columns",
                    options = listOf("Auto" to "auto", "2" to "2", "3" to "3", "4" to "4"),
                    selected = gridColumns,
                    compareBy = { it == gridColumns },
                ) { scope.launch { up.setGridColumns(it) } }
                HorizontalDivider(color = SpineColors.Border, thickness = 1.dp)
                SwitchPref("AMOLED black mode", "Pure black backgrounds for OLED screens", amoled) {
                    scope.launch { up.setAmoledBlackMode(it) }
                }
                HorizontalDivider(color = SpineColors.Border, thickness = 1.dp)
                SwitchPref("Rating on cards", null, showRating) {
                    scope.launch { up.setShowRatingOnCards(it) }
                }
                HorizontalDivider(color = SpineColors.Border, thickness = 1.dp)
                SwitchPref("Play count on cards", null, showPlayCount) {
                    scope.launch { up.setShowPlayCountOnCards(it) }
                }
                HorizontalDivider(color = SpineColors.Border, thickness = 1.dp)
                SwitchPref("Resolution on cards", null, showResolution) {
                    scope.launch { up.setShowResolutionOnCards(it) }
                }
            }

            Spacer(Modifier.size(8.dp))

            // ── APP BEHAVIOR ────────────────────────────────────────────
            SectionHeader("App behavior")

            SectionContainer {
                SwitchPref("Activity tracking", "Sync resume position + play count to Stash", activityTracking) {
                    scope.launch { up.setActivityTracking(it) }
                }
                HorizontalDivider(color = SpineColors.Border, thickness = 1.dp)
                SwitchPref("Auto-rotate player", "Sensor landscape when playing", autoRotate) {
                    scope.launch { up.setAutoRotatePlayer(it) }
                }
            }

            Spacer(Modifier.size(8.dp))

            // ── ABOUT / ACCOUNT ─────────────────────────────────────────
            SectionHeader("Account")

            SectionContainer {
                SettingRow(
                    key = "Disconnect server",
                    onClick = { viewModel.disconnect(onDisconnected) },
                )
            }

            Spacer(Modifier.size(8.dp))

            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp)) {
                Text("Back")
            }

            Spacer(Modifier.size(24.dp))
        }
    }
}

// ---- Section primitives ─────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MetaMono.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 1.5.sp),
        color = SpineColors.OnSurfaceMuted,
        modifier = Modifier.padding(top = 8.dp, start = 18.dp, end = 18.dp, bottom = 4.dp),
    )
}

/** Spine section container: Surface with ShapeSmall clip and overflow hidden. */
@Composable
private fun SectionContainer(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp),
        color = SpineColors.Surface,
        shape = ShapeSmall,
    ) {
        Column {
            content()
        }
    }
}

/** Plain tappable setting row with a key label (no value, just action). */
@Composable
private fun SettingRow(key: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            key,
            style = TextStyle(fontFamily = SpaceGrotesk, fontSize = 13.sp, fontWeight = FontWeight.Medium),
            color = SpineColors.OnSurface,
            modifier = Modifier.weight(1f),
        )
    }
}

// ---- Reusable settings primitives -------------------------------------------

/**
 * COMPLY-06 (per D-06): system per-app language picker entry point.
 * Renders nothing below API 33 (the ACTION_APP_LOCALE_SETTINGS Intent only
 * resolves on Tiramisu+). With Slopper's English-only resources today, the
 * dialog opens but shows "App default" only — expected behavior per Pitfall E4;
 * translation work is out of scope for this milestone.
 *
 * Strings live in app/src/main/res/values/strings.xml per CONTEXT.md Claude's
 * Discretion (system-Intent action belongs to app scope, not feature scope) —
 * R.string references resolve via the app-scope R import above.
 */
@Composable
private fun LanguageRow() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                val intent =
                    Intent(Settings.ACTION_APP_LOCALE_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                context.startActivity(intent)
            }
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.Language,
            contentDescription = null,
            tint = SpineColors.OnSurface,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.size(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(R.string.settings_language),
                style = TextStyle(fontFamily = SpaceGrotesk, fontSize = 13.sp, fontWeight = FontWeight.Medium),
                color = SpineColors.OnSurface,
            )
            Text(
                stringResource(R.string.settings_language_description),
                style = MetaMono,
                color = SpineColors.OnSurfaceMuted,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

@Composable
private fun SwitchPref(
    title: String,
    subtitle: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = TextStyle(fontFamily = SpaceGrotesk, fontSize = 13.sp, fontWeight = FontWeight.Medium),
                color = SpineColors.OnSurface,
            )
            subtitle?.let {
                Text(
                    it,
                    style = MetaMono,
                    color = SpineColors.OnSurfaceMuted,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        // SpineSwitch — M3 Switch with Spine custom SwitchColors (per SPINE-09)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = SpineColors.AccentOnPrimary,
                checkedTrackColor = SpineColors.AccentPrimary,
                checkedBorderColor = SpineColors.AccentPrimary,
                uncheckedThumbColor = SpineColors.OnSurfaceVariant,
                uncheckedTrackColor = SpineColors.SurfaceHigh,
                uncheckedBorderColor = SpineColors.Border,
            ),
        )
    }
}

@Composable
private fun SliderPref(
    title: String,
    valueLabel: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    onValueChange: (Float) -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                title,
                style = TextStyle(fontFamily = SpaceGrotesk, fontSize = 13.sp, fontWeight = FontWeight.Medium),
                color = SpineColors.OnSurface,
                modifier = Modifier.weight(1f),
            )
            Text(
                valueLabel,
                style = TextStyle(fontFamily = JetBrainsMono, fontSize = 11.sp),
                color = SpineColors.AccentPrimary,
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            steps = steps,
            colors = SliderDefaults.colors(
                thumbColor = SpineColors.AccentPrimary,
                activeTrackColor = SpineColors.AccentPrimary,
                inactiveTrackColor = SpineColors.SurfaceHigh,
            ),
        )
    }
}

@Composable
private fun <T> ChipRowPref(
    title: String,
    options: List<Pair<String, T>>,
    selected: T,
    compareBy: (T) -> Boolean,
    onSelect: (T) -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp)) {
        Text(
            title,
            style = TextStyle(fontFamily = SpaceGrotesk, fontSize = 13.sp, fontWeight = FontWeight.Medium),
            color = SpineColors.OnSurface,
        )
        Spacer(Modifier.size(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            options.forEach { (label, value) ->
                val isSelected = compareBy(value)
                Box(
                    modifier = Modifier
                        .clip(ShapeSmall)
                        .background(if (isSelected) SpineColors.AccentPrimary else SpineColors.SurfaceHigh)
                        .clickable { onSelect(value) }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        label,
                        style = MetaMono,
                        color = if (isSelected) SpineColors.AccentOnPrimary else SpineColors.OnSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun CodecStatusCard() {
    val usable = CodecCapabilities.ffmpegExtensionUsable
    val present = CodecCapabilities.ffmpegExtensionPresent
    val (icon, tint, headline) =
        when {
            usable -> Triple(Icons.Filled.CheckCircle, SpineColors.AccentPrimary, "Full codec support")
            present -> Triple(Icons.Filled.Warning, MaterialTheme.colorScheme.error, "FFmpeg detected but not loaded")
            else -> Triple(Icons.Filled.Warning, MaterialTheme.colorScheme.error, "Limited codec support")
        }
    Surface(
        shape = ShapeSmall,
        color = SpineColors.Surface,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 4.dp)
            .border(1.dp, SpineColors.Border, ShapeSmall),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(22.dp))
            Spacer(Modifier.size(12.dp))
            Column {
                Text(
                    headline,
                    style = TextStyle(fontFamily = SpaceGrotesk, fontSize = 13.sp, fontWeight = FontWeight.Medium),
                    color = SpineColors.OnSurface,
                )
                Spacer(Modifier.size(4.dp))
                Text(
                    CodecCapabilities.statusLabel,
                    style = MetaMono,
                    color = SpineColors.OnSurfaceMuted,
                )
            }
        }
    }
}
