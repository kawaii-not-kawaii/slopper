package io.stashapp.android.feature.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.PowerOff
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.VideoLibrary
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.stashapp.android.core.data.prefs.PlayerPreferences
import io.stashapp.android.core.data.prefs.UiPreferences
import io.stashapp.android.core.designsystem.component.CSlider
import io.stashapp.android.core.designsystem.component.DRow
import io.stashapp.android.core.designsystem.component.DRowStacked
import io.stashapp.android.core.designsystem.theme.EmberAccent
import io.stashapp.android.core.designsystem.theme.JetBrainsMono
import io.stashapp.android.core.designsystem.theme.LocalAccentColors
import io.stashapp.android.core.designsystem.theme.MetaMono
import io.stashapp.android.core.designsystem.theme.SageAccent
import io.stashapp.android.core.designsystem.theme.ShapeSmall
import io.stashapp.android.core.designsystem.theme.SignalAccent
import io.stashapp.android.core.designsystem.theme.SpaceGrotesk
import io.stashapp.android.core.designsystem.theme.SpineColors
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onDisconnected: () -> Unit,
    onServerClick: () -> Unit,
    onAboutClick: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val pp = viewModel.playerPrefs
    val up = viewModel.uiPrefs

    val activeServer by viewModel.activeServer.collectAsStateWithLifecycle()
    val serverInfo by viewModel.serverInfo.collectAsStateWithLifecycle()

    // Playback prefs
    val doubleTap by pp.doubleTapSeekSeconds.collectAsStateWithLifecycle(10)
    val seekMs by pp.seekMsPerPx.collectAsStateWithLifecycle(120f)

    // Display prefs
    val accentPalette by up.accentPalette.collectAsStateWithLifecycle("sage")
    val gridColumns by up.gridColumns.collectAsStateWithLifecycle("auto")

    // Library prefs
    val cacheMb by up.imageCacheSizeMb.collectAsStateWithLifecycle(256)

    LazyColumn(contentPadding = PaddingValues(bottom = 100.dp)) {
        // ---- TopAppBar ----
        item {
            TopAppBar(
                title = {
                    Text(
                        "Settings",
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
                actions = {
                    val synced = serverInfo != null
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = SpineColors.Surface,
                        border = BorderStroke(1.dp, SpineColors.Border),
                        modifier = Modifier.padding(end = 12.dp),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier =
                                    Modifier
                                        .size(6.dp)
                                        .background(
                                            if (synced) SpineColors.Success else SpineColors.OnSurfaceMuted,
                                            RoundedCornerShape(50),
                                        ),
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(
                                if (synced) "Synced" else "Offline",
                                style = MetaMono,
                                color = if (synced) SpineColors.Success else SpineColors.OnSurfaceMuted,
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SpineColors.Bg),
            )
        }

        // ---- Server status card ----
        item {
            Surface(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                        .then(
                            if (activeServer != null) {
                                Modifier.clickable { onServerClick() }
                            } else {
                                Modifier
                            },
                        ),
                color = SpineColors.Surface,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, SpineColors.Border),
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val accentColors = LocalAccentColors.current
                    Box(
                        modifier =
                            Modifier
                                .size(40.dp)
                                .background(
                                    accentColors.primary.copy(alpha = 0.12f),
                                    RoundedCornerShape(10.dp),
                                ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = null,
                            tint = accentColors.primary,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = activeServer?.displayName ?: "Not connected",
                                style =
                                    TextStyle(
                                        fontFamily = SpaceGrotesk,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                    ),
                                color = SpineColors.OnSurface,
                            )
                            if (serverInfo != null) {
                                Spacer(Modifier.width(6.dp))
                                Box(
                                    Modifier
                                        .size(8.dp)
                                        .background(SpineColors.Success, RoundedCornerShape(50)),
                                )
                            }
                        }
                        if (activeServer != null) {
                            val subText =
                                serverInfo?.let { "Stash v${it.version} · ${it.sceneCount} scenes" }
                                    ?: "Connected · tap to refresh"
                            Text(
                                text = subText,
                                style = MetaMono,
                                color = SpineColors.OnSurfaceMuted,
                                modifier = Modifier.padding(top = 2.dp),
                            )
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowForwardIos,
                        contentDescription = null,
                        tint = SpineColors.OnSurfaceMuted,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        }

        // ---- Theme ----
        item {
            DetailGroup(
                title = "Theme",
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            ) {
                val palettes =
                    listOf(
                        Triple("sage", "Sage", SageAccent.primary),
                        Triple("ember", "Ember", EmberAccent.primary),
                        Triple("signal", "Signal", SignalAccent.primary),
                    )
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    palettes.forEach { (id, name, color) ->
                        val isActive = accentPalette == id
                        Box(
                            modifier =
                                Modifier
                                    .weight(1f)
                                    .clip(ShapeSmall)
                                    .background(
                                        if (isActive) color.copy(alpha = 0.08f) else SpineColors.Bg,
                                    ).border(1.dp, if (isActive) color else SpineColors.Border, ShapeSmall)
                                    .clickable { viewModel.setAccentPalette(id) }
                                    .padding(10.dp),
                        ) {
                            Column {
                                Box(
                                    modifier =
                                        Modifier
                                            .size(28.dp)
                                            .background(color, RoundedCornerShape(6.dp)),
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = name,
                                    style =
                                        TextStyle(
                                            fontFamily = SpaceGrotesk,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                        ),
                                    color = SpineColors.OnSurface,
                                )
                            }
                            if (isActive) {
                                Icon(
                                    imageVector = Icons.Outlined.Check,
                                    contentDescription = null,
                                    tint = color,
                                    modifier =
                                        Modifier
                                            .size(14.dp)
                                            .align(Alignment.TopEnd),
                                )
                            }
                        }
                    }
                }
            }
        }

        // ---- Library layout ----
        item {
            DetailGroup(
                title = "Library layout",
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            ) {
                DRowStacked(label = "Grid columns") {
                    ChipRow(
                        options =
                            listOf(
                                "Auto" to "auto",
                                "2" to "2",
                                "3" to "3",
                                "4" to "4",
                            ),
                        selected = gridColumns,
                        onSelect = { v -> viewModel.setUi { setGridColumns(v) } },
                    )
                }
            }
        }

        // ---- Playback ----
        item {
            DetailGroup(
                title = "Playback",
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
            }
        }

        // ---- Image cache ----
        item {
            DetailGroup(
                title = "Image cache",
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            ) {
                DRowStacked(
                    label = "Disk cache",
                    hint = "Size of the thumbnail disk cache",
                ) {
                    CSlider(
                        value = cacheMb.toFloat(),
                        onValueChange = { viewModel.setUi { setImageCacheSizeMb(it.roundToInt()) } },
                        valueRange = 64f..512f,
                        valueLabel = "$cacheMb MB",
                    )
                }
            }
        }

        // ---- Nav rows: Server, About ----
        item {
            SectionLabel("App", Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
            NavRow(
                icon = Icons.Outlined.Settings,
                label = "Server",
                onClick = onServerClick,
            )
            NavRow(
                icon = Icons.Outlined.Info,
                label = "About & diagnostics",
                onClick = onAboutClick,
            )
        }

        // ---- Danger zone ----
        item {
            SectionLabel("Danger zone", Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
            NavRow(
                icon = Icons.Outlined.PowerOff,
                label = "Disconnect server",
                onClick = { viewModel.disconnect(onDisconnected) },
                danger = true,
            )
        }
    }
}

// ---- Nav rows ---------------------------------------------------------------

@Composable
private fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text.uppercase(),
        style =
            MetaMono.copy(
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp,
            ),
        color = SpineColors.OnSurfaceMuted,
        modifier = modifier.padding(bottom = 8.dp, start = 2.dp),
    )
}

@Composable
private fun NavRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    danger: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val iconBg = if (danger) SpineColors.Error.copy(alpha = 0.08f) else SpineColors.SurfaceHigh
    val iconTint = if (danger) SpineColors.Error else SpineColors.OnSurfaceVariant
    val labelColor = if (danger) SpineColors.Error else SpineColors.OnSurface
    Surface(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp)
                .clickable(onClick = onClick),
        color = SpineColors.Surface,
        shape = ShapeSmall,
        border = BorderStroke(1.dp, SpineColors.Border),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(32.dp)
                        .background(iconBg, ShapeSmall),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = label,
                style =
                    TextStyle(
                        fontFamily = SpaceGrotesk,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                color = labelColor,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowForwardIos,
                contentDescription = null,
                tint = SpineColors.OnSurfaceMuted,
                modifier = Modifier.size(16.dp),
            )
        }
    }
    Spacer(Modifier.height(6.dp))
}

// ---- Shared primitives (used by Server + About sub-screens) -----------------

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
 */
@Composable
internal fun <T> ChipRow(
    options: List<Pair<String, T>>,
    selected: T,
    onSelect: (T) -> Unit,
) {
    val accent = LocalAccentColors.current
    androidx.compose.foundation.lazy.LazyRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
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
            Row(
                modifier = Modifier.padding(bottom = 8.dp, start = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
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
                    Spacer(Modifier.padding(start = 8.dp))
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
