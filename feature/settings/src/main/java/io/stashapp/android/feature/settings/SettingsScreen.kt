package io.stashapp.android.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.stashapp.android.core.data.prefs.PlayerPreferences
import io.stashapp.android.core.data.prefs.UiPreferences
import io.stashapp.android.core.designsystem.theme.StashColors
import io.stashapp.android.core.domain.ConnectionRepository
import io.stashapp.android.feature.player.CodecCapabilities
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel
    @Inject
    constructor(
        private val connectionRepository: ConnectionRepository,
        val playerPrefs: PlayerPreferences,
        val uiPrefs: UiPreferences,
    ) : ViewModel() {
        fun disconnect(onDone: () -> Unit) {
            viewModelScope.launch {
                connectionRepository.disconnect()
                onDone()
            }
        }

        fun <T> setPlayer(setter: suspend PlayerPreferences.() -> Unit) {
            viewModelScope.launch { playerPrefs.setter() }
        }

        fun <T> setUi(setter: suspend UiPreferences.() -> Unit) {
            viewModelScope.launch { uiPrefs.setter() }
        }
    }

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
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            // ── PLAYER ──────────────────────────────────────────────────
            SectionHeader("Player")

            SliderPref(
                "Scrub sensitivity",
                "${seekMsPerPx.toInt()} ms/px",
                seekMsPerPx,
                PlayerPreferences.SEEK_MS_PER_PX_MIN..PlayerPreferences.SEEK_MS_PER_PX_MAX,
            ) { scope.launch { pp.setSeekMsPerPx(it) } }

            SliderPref(
                "Double-tap seek",
                "${doubleTapSec}s",
                doubleTapSec.toFloat(),
                PlayerPreferences.DOUBLE_TAP_SEEK_MIN.toFloat()..PlayerPreferences.DOUBLE_TAP_SEEK_MAX.toFloat(),
                steps = PlayerPreferences.DOUBLE_TAP_SEEK_MAX - PlayerPreferences.DOUBLE_TAP_SEEK_MIN - 1,
            ) { scope.launch { pp.setDoubleTapSeekSeconds(it.toInt()) } }

            ChipRowPref(
                "Default speed",
                options = listOf("0.5x" to 0.5f, "0.75x" to 0.75f, "1x" to 1.0f, "1.25x" to 1.25f, "1.5x" to 1.5f, "2x" to 2.0f),
                selected = defaultSpeed,
                compareBy = { kotlin.math.abs(it - defaultSpeed) < 0.01f },
            ) { scope.launch { pp.setDefaultPlaybackSpeed(it) } }

            SwitchPref("Auto-play next", "Advance to the next item in queue", autoPlayNext) {
                scope.launch { pp.setAutoPlayNext(it) }
            }

            SliderPref(
                "Resume threshold",
                "${resumeThreshold}s",
                resumeThreshold.toFloat(),
                0f..30f,
                steps = 29,
            ) { scope.launch { pp.setResumeThresholdSeconds(it.toInt()) } }

            SliderPref(
                "Completion threshold",
                "$completionThreshold%",
                completionThreshold.toFloat(),
                50f..100f,
                steps = 49,
            ) { scope.launch { pp.setCompletionThresholdPercent(it.toInt()) } }

            SliderPref(
                "Skip intro",
                if (skipIntro == 0) "Off" else "${skipIntro}s",
                skipIntro.toFloat(),
                0f..120f,
                steps = 23,
            ) { scope.launch { pp.setSkipIntroSeconds(it.toInt()) } }

            ChipRowPref(
                "Video buffer",
                options = listOf("Small (15s)" to "small", "Medium (50s)" to "medium", "Large (2min)" to "large"),
                selected = bufferPreset,
                compareBy = { it == bufferPreset },
            ) { scope.launch { pp.setVideoBufferPreset(it) } }

            ChipRowPref(
                "Default aspect ratio",
                options = listOf("Fit" to "fit", "Crop" to "crop", "Stretch" to "stretch"),
                selected = aspectRatio,
                compareBy = { it == aspectRatio },
            ) { scope.launch { pp.setDefaultAspectRatio(it) } }

            ChipRowPref(
                "Decoder preference",
                options = listOf("Auto" to "auto", "Prefer HW" to "prefer_hw", "Prefer SW" to "prefer_sw"),
                selected = decoderPref,
                compareBy = { it == decoderPref },
            ) { scope.launch { pp.setDecoderPreference(it) } }

            CodecStatusCard()

            SectionDivider()

            // ── CACHE ───────────────────────────────────────────────────
            SectionHeader("Cache")

            SliderPref(
                "Image cache size",
                "$imageCacheMb MB",
                imageCacheMb.toFloat(),
                64f..512f,
                steps = 7,
            ) { scope.launch { up.setImageCacheSizeMb(it.toInt()) } }

            OutlinedButton(
                onClick = {
                    val cacheDir = context.cacheDir.resolve("image_cache")
                    cacheDir.deleteRecursively()
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text("Clear image cache")
            }

            SectionDivider()

            // ── DISPLAY ─────────────────────────────────────────────────
            SectionHeader("Display")

            ChipRowPref(
                "Grid columns",
                options = listOf("Auto" to "auto", "2" to "2", "3" to "3", "4" to "4"),
                selected = gridColumns,
                compareBy = { it == gridColumns },
            ) { scope.launch { up.setGridColumns(it) } }

            SwitchPref("AMOLED black mode", "Pure black backgrounds for OLED screens", amoled) {
                scope.launch { up.setAmoledBlackMode(it) }
            }

            SwitchPref("Rating on cards", null, showRating) {
                scope.launch { up.setShowRatingOnCards(it) }
            }
            SwitchPref("Play count on cards", null, showPlayCount) {
                scope.launch { up.setShowPlayCountOnCards(it) }
            }
            SwitchPref("Resolution on cards", null, showResolution) {
                scope.launch { up.setShowResolutionOnCards(it) }
            }

            SectionDivider()

            // ── APP BEHAVIOR ────────────────────────────────────────────
            SectionHeader("App behavior")

            SwitchPref("Activity tracking", "Sync resume position + play count to Stash", activityTracking) {
                scope.launch { up.setActivityTracking(it) }
            }
            SwitchPref("Auto-rotate player", "Sensor landscape when playing", autoRotate) {
                scope.launch { up.setAutoRotatePlayer(it) }
            }

            SectionDivider()

            // ── ABOUT / ACCOUNT ─────────────────────────────────────────
            SectionHeader("Account")

            OutlinedButton(
                onClick = { viewModel.disconnect(onDisconnected) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Disconnect server") }

            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                Text("Back")
            }

            Spacer(Modifier.size(24.dp))
        }
    }
}

// ---- Reusable settings primitives -------------------------------------------

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
        color = StashColors.AccentPrimary,
        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
    )
}

@Composable
private fun SectionDivider() {
    HorizontalDivider(
        color = StashColors.Divider,
        modifier = Modifier.padding(vertical = 8.dp),
    )
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
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            subtitle?.let {
                Text(it, style = MaterialTheme.typography.labelSmall, color = StashColors.OnSurfaceVariant)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedTrackColor = StashColors.AccentPrimary),
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
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Text(valueLabel, style = MaterialTheme.typography.labelMedium, color = StashColors.OnSurfaceVariant)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            steps = steps,
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
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(title, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.size(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            options.forEach { (label, value) ->
                val isSelected = compareBy(value)
                Surface(
                    color = if (isSelected) StashColors.AccentPrimary else MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = if (isSelected) StashColors.AccentOnPrimary else MaterialTheme.colorScheme.onSurface,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.clickable { onSelect(value) },
                ) {
                    Text(
                        label,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
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
            usable -> Triple(Icons.Filled.CheckCircle, StashColors.AccentPrimary, "Full codec support")
            present -> Triple(Icons.Filled.Warning, MaterialTheme.colorScheme.error, "FFmpeg detected but not loaded")
            else -> Triple(Icons.Filled.Warning, MaterialTheme.colorScheme.error, "Limited codec support")
        }
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(22.dp))
            Spacer(Modifier.size(12.dp))
            Column {
                Text(headline, style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.size(4.dp))
                Text(CodecCapabilities.statusLabel, style = MaterialTheme.typography.bodySmall, color = StashColors.OnSurfaceVariant)
            }
        }
    }
}
