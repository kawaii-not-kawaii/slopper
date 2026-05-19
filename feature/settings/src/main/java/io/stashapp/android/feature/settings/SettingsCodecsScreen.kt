package io.stashapp.android.feature.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.widget.Toast
import io.stashapp.android.core.designsystem.component.DRow
import io.stashapp.android.core.designsystem.component.DRowStacked
import io.stashapp.android.core.designsystem.theme.LocalAccentColors
import io.stashapp.android.core.designsystem.theme.MetaMono
import io.stashapp.android.core.designsystem.theme.ShapeSmall
import io.stashapp.android.core.designsystem.theme.SpaceGrotesk
import io.stashapp.android.core.designsystem.theme.SpineColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsCodecsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val decoderPref by viewModel.playerPrefs.decoderPreference.collectAsStateWithLifecycle("auto")
    val bufferPreset by viewModel.playerPrefs.videoBufferPreset.collectAsStateWithLifecycle("medium")
    val fallback by viewModel.playerPrefs.fallbackOnDecoderError.collectAsStateWithLifecycle(true)
    val tunneling by viewModel.playerPrefs.tunneling.collectAsStateWithLifecycle(false)
    val preBuffer by viewModel.playerPrefs.preBufferOnHover.collectAsStateWithLifecycle(false)
    val hdr by viewModel.playerPrefs.hdrPassthrough.collectAsStateWithLifecycle(false)
    val matchRefresh by viewModel.playerPrefs.matchRefreshRate.collectAsStateWithLifecycle(false)
    val matchRes by viewModel.playerPrefs.matchResolution.collectAsStateWithLifecycle(false)

    val context = LocalContext.current

    LazyColumn(contentPadding = PaddingValues(bottom = 100.dp)) {
        item {
            TopAppBar(
                title = {
                    Text(
                        "Quality & Codecs",
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

        // Capability banner (standalone Surface item)
        item {
            val accent = LocalAccentColors.current
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                color = accent.primary.copy(alpha = 0.06f),
                shape = ShapeSmall,
                border = BorderStroke(1.dp, accent.primary.copy(alpha = 0.25f)),
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Tune,
                        contentDescription = null,
                        tint = accent.primary,
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Full codec support",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = SpineColors.OnSurface,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "HW · HEVC · AV1 · H264 · FFmpeg extension loaded",
                            style = MetaMono,
                            color = SpineColors.OnSurfaceMuted,
                        )
                    }
                }
            }
        }

        // Group: Decoder
        item {
            CodecsDetailGroup(
                title = "Decoder",
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            ) {
                DRowStacked(label = "Decoder preference") {
                    ChipRow(
                        options = listOf(
                            "Auto" to "auto",
                            "Prefer HW" to "prefer_hw",
                            "Prefer SW" to "prefer_sw",
                        ),
                        selected = decoderPref,
                        onSelect = { v -> viewModel.setPlayer { setDecoderPreference(v) } },
                    )
                }
                HorizontalDivider(color = SpineColors.Border, thickness = 1.dp)
                DRow(
                    label = "Fallback on failure",
                    hint = "Try software decoder if hardware fails",
                    trailing = {
                        SpineSwitch(checked = fallback) {
                            viewModel.setPlayer { setFallbackOnDecoderError(it) }
                        }
                    },
                )
                HorizontalDivider(color = SpineColors.Border, thickness = 1.dp)
                DRow(
                    label = "Tunneling",
                    hint = "Use tunneled video rendering (experimental)",
                    trailing = {
                        SpineSwitch(checked = tunneling) {
                            viewModel.setPlayer { setTunneling(it) }
                        }
                    },
                )
            }
        }

        // Group: Buffer
        item {
            CodecsDetailGroup(
                title = "Buffer",
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            ) {
                DRowStacked(label = "Buffer size") {
                    ChipRow(
                        options = listOf(
                            "Small · 15s" to "small",
                            "Medium · 50s" to "medium",
                            "Large · 2min" to "large",
                        ),
                        selected = bufferPreset,
                        onSelect = { v -> viewModel.setPlayer { setVideoBufferPreset(v) } },
                    )
                }
                HorizontalDivider(color = SpineColors.Border, thickness = 1.dp)
                DRow(
                    label = "Pre-buffer on hover",
                    hint = "Begin loading on long-press before play",
                    trailing = {
                        SpineSwitch(checked = preBuffer) {
                            viewModel.setPlayer { setPreBufferOnHover(it) }
                        }
                    },
                )
            }
        }

        // Group: Display & HDR
        item {
            CodecsDetailGroup(
                title = "Display & HDR",
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            ) {
                DRow(
                    label = "HDR passthrough",
                    hint = "Pass HDR signal to display (requires HDR panel)",
                    trailing = {
                        SpineSwitch(checked = hdr) {
                            viewModel.setPlayer { setHdrPassthrough(it) }
                        }
                    },
                )
                HorizontalDivider(color = SpineColors.Border, thickness = 1.dp)
                DRow(
                    label = "Match refresh rate",
                    hint = "Switch display to video frame rate when playing",
                    trailing = {
                        SpineSwitch(checked = matchRefresh) {
                            viewModel.setPlayer { setMatchRefreshRate(it) }
                        }
                    },
                )
                HorizontalDivider(color = SpineColors.Border, thickness = 1.dp)
                DRow(
                    label = "Match resolution",
                    hint = "Adjust display resolution to match video (premium panels)",
                    trailing = {
                        SpineSwitch(checked = matchRes) {
                            viewModel.setPlayer { setMatchResolution(it) }
                        }
                    },
                )
            }
        }

        // Footer: Run codec test button
        item {
            OutlinedButton(
                onClick = {
                    Toast.makeText(context, "Codec test — coming soon", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                border = BorderStroke(1.dp, SpineColors.Border),
                shape = ShapeSmall,
            ) {
                Text(
                    "Run codec test",
                    style = MaterialTheme.typography.labelMedium,
                    fontFamily = SpaceGrotesk,
                    color = SpineColors.OnSurface,
                )
            }
        }
    }
}

// Local DetailGroup for Codecs screen — same structure as in SettingsPlaybackScreen
// but private to avoid duplicate private fun collision (will be deduplicated via internal when merged)
@Composable
private fun CodecsDetailGroup(
    title: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = modifier) {
        if (title != null) {
            Text(
                text = title.uppercase(),
                style = MetaMono.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp,
                ),
                color = SpineColors.OnSurfaceMuted,
                modifier = Modifier.padding(bottom = 8.dp, start = 2.dp),
            )
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
