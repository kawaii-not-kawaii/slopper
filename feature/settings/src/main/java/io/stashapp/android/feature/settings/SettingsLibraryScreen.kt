package io.stashapp.android.feature.settings

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.stashapp.android.core.designsystem.component.CSlider
import io.stashapp.android.core.designsystem.component.DRow
import io.stashapp.android.core.designsystem.component.DRowStacked
import io.stashapp.android.core.designsystem.theme.MetaMono
import io.stashapp.android.core.designsystem.theme.ShapeSmall
import io.stashapp.android.core.designsystem.theme.SpaceGrotesk
import io.stashapp.android.core.designsystem.theme.SpineColors
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsLibraryScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val activityTracking by viewModel.uiPrefs.activityTracking.collectAsStateWithLifecycle(true)
    val syncRatings by viewModel.uiPrefs.syncRatings.collectAsStateWithLifecycle(true)
    val syncOCounter by viewModel.uiPrefs.syncOCounter.collectAsStateWithLifecycle(true)
    val syncMarkers by viewModel.uiPrefs.syncMarkers.collectAsStateWithLifecycle(true)
    val cacheMb by viewModel.uiPrefs.imageCacheSizeMb.collectAsStateWithLifecycle(256)
    val cacheDuration by viewModel.uiPrefs.cacheDuration.collectAsStateWithLifecycle("1week")
    val keepHistory by viewModel.uiPrefs.keepWatchHistory.collectAsStateWithLifecycle(true)
    val historyOnHome by viewModel.uiPrefs.historyOnHome.collectAsStateWithLifecycle(false)
    val smartRails by viewModel.uiPrefs.smartRails.collectAsStateWithLifecycle(false)
    val context = LocalContext.current

    LazyColumn(contentPadding = PaddingValues(bottom = 100.dp)) {
        item {
            TopAppBar(
                title = {
                    Text(
                        "Library",
                        style = TextStyle(
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

        // Group: Sync with Stash
        item {
            DetailGroup(
                title = "Sync with Stash",
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            ) {
                DRow(
                    label = "Activity tracking",
                    hint = "Send play/resume/finish events to Stash",
                    trailing = {
                        SpineSwitch(checked = activityTracking) {
                            viewModel.setUi { setActivityTracking(it) }
                        }
                    },
                )
                HorizontalDivider(color = SpineColors.Border, thickness = 1.dp)
                DRow(
                    label = "Sync ratings",
                    hint = "Upload star ratings to Stash on change",
                    trailing = {
                        SpineSwitch(checked = syncRatings) {
                            viewModel.setUi { setSyncRatings(it) }
                        }
                    },
                )
                HorizontalDivider(color = SpineColors.Border, thickness = 1.dp)
                DRow(
                    label = "Sync O-counter",
                    hint = "Upload O-counter increments to Stash",
                    trailing = {
                        SpineSwitch(checked = syncOCounter) {
                            viewModel.setUi { setSyncOCounter(it) }
                        }
                    },
                )
                HorizontalDivider(color = SpineColors.Border, thickness = 1.dp)
                DRow(
                    label = "Sync markers",
                    hint = "Upload chapter/marker edits to Stash",
                    trailing = {
                        SpineSwitch(checked = syncMarkers) {
                            viewModel.setUi { setSyncMarkers(it) }
                        }
                    },
                )
            }
        }

        // Group: Cache (with footer button)
        item {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                Text(
                    text = "CACHE",
                    style = MetaMono.copy(
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp,
                    ),
                    color = SpineColors.OnSurfaceMuted,
                    modifier = Modifier.padding(bottom = 8.dp, start = 2.dp),
                )
                Surface(
                    color = SpineColors.Surface,
                    shape = ShapeSmall,
                    border = BorderStroke(1.dp, SpineColors.Border),
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        DRowStacked(
                            label = "Image cache",
                            hint = "Size of the thumbnail disk cache",
                        ) {
                            CSlider(
                                value = cacheMb.toFloat(),
                                onValueChange = { viewModel.setUi { setImageCacheSizeMb(it.roundToInt()) } },
                                valueRange = 64f..512f,
                                valueLabel = "$cacheMb MB",
                            )
                        }
                        HorizontalDivider(color = SpineColors.Border, thickness = 1.dp)
                        DRowStacked(
                            label = "Cache duration",
                            hint = "How long cached data is kept",
                        ) {
                            ChipRow(
                                options = listOf(
                                    "1 day" to "1day",
                                    "1 week" to "1week",
                                    "30 days" to "30days",
                                    "Forever" to "forever",
                                ),
                                selected = cacheDuration,
                                onSelect = { v -> viewModel.setUi { setCacheDuration(v) } },
                            )
                        }
                        HorizontalDivider(color = SpineColors.Border, thickness = 1.dp)
                        TextButton(
                            onClick = {
                                Toast.makeText(context, "Cache clear — coming soon", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                        ) {
                            Text(
                                text = "Clear image cache · up to $cacheMb MB",
                                style = MetaMono,
                                color = SpineColors.AccentCool,
                            )
                        }
                    }
                }
            }
        }

        // Group: History
        item {
            DetailGroup(
                title = "History",
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            ) {
                DRow(
                    label = "Keep watch history",
                    hint = "Record what you've watched",
                    trailing = {
                        SpineSwitch(checked = keepHistory) {
                            viewModel.setUi { setKeepWatchHistory(it) }
                        }
                    },
                )
                HorizontalDivider(color = SpineColors.Border, thickness = 1.dp)
                DRow(
                    label = "History on Home",
                    hint = "Show Recently Watched rail on the Home screen",
                    trailing = {
                        SpineSwitch(checked = historyOnHome) {
                            viewModel.setUi { setHistoryOnHome(it) }
                        }
                    },
                )
                HorizontalDivider(color = SpineColors.Border, thickness = 1.dp)
                DRow(
                    label = "Smart rails",
                    hint = "AI-curated suggestions based on watch history",
                    trailing = {
                        SpineSwitch(checked = smartRails) {
                            viewModel.setUi { setSmartRails(it) }
                        }
                    },
                )
            }
        }

        // Downloads: hidden behind feature flag — not implemented in v1
        @Suppress("KotlinConstantConditions")
        if (false) {
            // DetailGroup("Downloads · offline", badge = "Beta") { ... }
        }
    }
}
