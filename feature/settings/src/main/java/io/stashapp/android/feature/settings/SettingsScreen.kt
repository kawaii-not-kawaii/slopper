package io.stashapp.android.feature.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.PowerOff
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.stashapp.android.core.data.prefs.PlayerPreferences
import io.stashapp.android.core.data.prefs.UiPreferences
import io.stashapp.android.core.designsystem.theme.JetBrainsMono
import io.stashapp.android.core.designsystem.theme.LocalAccentColors
import io.stashapp.android.core.designsystem.theme.MetaMono
import io.stashapp.android.core.designsystem.theme.ShapeSmall
import io.stashapp.android.core.designsystem.theme.SpaceGrotesk
import io.stashapp.android.core.designsystem.theme.SpineColors
import io.stashapp.android.core.ui.nav.Routes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onDisconnected: () -> Unit,
    onPlaybackClick: () -> Unit,
    onCodecsClick: () -> Unit,
    onDisplayClick: () -> Unit,
    onLibraryClick: () -> Unit,
    onServerClick: () -> Unit,
    onAboutClick: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val pp = viewModel.playerPrefs
    val up = viewModel.uiPrefs

    val activeServer by viewModel.activeServer.collectAsStateWithLifecycle()
    val serverInfo by viewModel.serverInfo.collectAsStateWithLifecycle()

    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()

    var showSearch by remember { mutableStateOf(false) }

    // Playback summary prefs
    val speed by pp.defaultPlaybackSpeed.collectAsStateWithLifecycle(initialValue = PlayerPreferences.DEFAULT_SPEED)
    val doubleTapSec by pp.doubleTapSeekSeconds.collectAsStateWithLifecycle(initialValue = PlayerPreferences.DEFAULT_DOUBLE_TAP_SEEK_SEC)
    val bufferPreset by pp.videoBufferPreset.collectAsStateWithLifecycle(initialValue = PlayerPreferences.DEFAULT_BUFFER_PRESET)
    val decoderPref by pp.decoderPreference.collectAsStateWithLifecycle(initialValue = PlayerPreferences.DEFAULT_DECODER_PREF)

    // Display summary prefs
    val accentPaletteVal by up.accentPalette.collectAsStateWithLifecycle(initialValue = UiPreferences.DEFAULT_ACCENT_PALETTE)
    val gridColumns by up.gridColumns.collectAsStateWithLifecycle(initialValue = UiPreferences.DEFAULT_GRID_COLUMNS)

    // Library summary prefs
    val imageCacheMb by up.imageCacheSizeMb.collectAsStateWithLifecycle(initialValue = UiPreferences.DEFAULT_IMAGE_CACHE_MB)

    // Summary strings
    val playbackSummary = "${speed}× · ${doubleTapSec}s seek"
    val codecSummary = "${decoderPref} · $bufferPreset"
    val displaySummary = "$accentPaletteVal · $gridColumns cols"
    val librarySummary = "$imageCacheMb MB cache"

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(bottom = 100.dp),
        ) {
            item {
                TopAppBar(
                    title = {
                        Text(
                            "Settings",
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
                    actions = {
                        // Sync-status pill
                        val synced = serverInfo != null
                        Surface(
                            shape = CircleShape,
                            color = SpineColors.Surface,
                            border = BorderStroke(1.dp, SpineColors.Border),
                            modifier = Modifier.padding(end = 12.dp),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(
                                            if (synced) SpineColors.Success else SpineColors.OnSurfaceMuted,
                                            CircleShape,
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
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = SpineColors.Bg,
                    ),
                )
            }

            // Server status card
            item {
                Surface(
                    modifier = Modifier
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
                        // Icon container
                        val accentColors = LocalAccentColors.current
                        Box(
                            modifier = Modifier
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
                                    style = TextStyle(
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
                                            .background(SpineColors.Success, CircleShape),
                                    )
                                }
                            }
                            if (activeServer != null) {
                                val subText = serverInfo?.let {
                                    "Stash v${it.version} · ${it.sceneCount} scenes"
                                } ?: "Connected · tap to refresh"
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

            // Quick search field — tapping opens search overlay
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 4.dp)
                        .clickable { showSearch = true },
                    color = SpineColors.Surface,
                    shape = ShapeSmall,
                    border = BorderStroke(1.dp, SpineColors.Border),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = null,
                            tint = SpineColors.OnSurfaceMuted,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "search settings…",
                            style = MetaMono,
                            color = SpineColors.OnSurfaceMuted,
                        )
                    }
                }
            }

            // HubGroup: Playback, Quality & Codecs, Display, Library
            item {
                HubGroup(
                    title = null,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                ) {
                    HubRow(
                        icon = Icons.Outlined.PlayArrow,
                        label = "Playback",
                        value = playbackSummary,
                        onClick = onPlaybackClick,
                    )
                    HorizontalDivider(color = SpineColors.Border, thickness = 1.dp)
                    HubRow(
                        icon = Icons.Outlined.Tune,
                        label = "Quality & Codecs",
                        value = codecSummary,
                        onClick = onCodecsClick,
                    )
                    HorizontalDivider(color = SpineColors.Border, thickness = 1.dp)
                    HubRow(
                        icon = Icons.Outlined.Palette,
                        label = "Display",
                        value = displaySummary,
                        onClick = onDisplayClick,
                    )
                    HorizontalDivider(color = SpineColors.Border, thickness = 1.dp)
                    HubRow(
                        icon = Icons.Outlined.VideoLibrary,
                        label = "Library",
                        value = librarySummary,
                        onClick = onLibraryClick,
                    )
                }
            }

            // HubGroup: App
            item {
                HubGroup(
                    title = "App",
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                ) {
                    HubRow(
                        icon = Icons.Outlined.Info,
                        label = "About & diagnostics",
                        value = "",
                        onClick = onAboutClick,
                    )
                }
            }

            // HubGroup: Danger zone
            item {
                HubGroup(
                    title = "Danger zone",
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                ) {
                    HubRow(
                        icon = Icons.Outlined.PowerOff,
                        label = "Disconnect server",
                        value = "",
                        onClick = { viewModel.disconnect(onDisconnected) },
                        danger = true,
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = showSearch,
            enter = fadeIn(tween(150)),
            exit = fadeOut(tween(100)),
        ) {
            SettingsSearchOverlay(
                query = searchQuery,
                results = searchResults,
                onQueryChange = viewModel::updateSearchQuery,
                onResultClick = { entry ->
                    showSearch = false
                    viewModel.updateSearchQuery("")
                    when (entry.route) {
                        Routes.SettingsPlayback -> onPlaybackClick()
                        Routes.SettingsCodecs   -> onCodecsClick()
                        Routes.SettingsDisplay  -> onDisplayClick()
                        Routes.SettingsLibrary  -> onLibraryClick()
                        Routes.SettingsServer   -> onServerClick()
                        Routes.SettingsAbout    -> onAboutClick()
                    }
                },
                onClose = {
                    showSearch = false
                    viewModel.updateSearchQuery("")
                },
            )
        }
    }
}

// ---- Hub primitives ----------------------------------------------------------

@Composable
private fun HubGroup(
    title: String?,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
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
            Column {
                content()
            }
        }
    }
}

@Composable
private fun HubRow(
    icon: ImageVector,
    label: String,
    value: String,
    onClick: () -> Unit,
    danger: Boolean = false,
) {
    val iconBg = if (danger) {
        SpineColors.Error.copy(alpha = 0.08f)
    } else {
        SpineColors.SurfaceHigh
    }
    val iconTint = if (danger) SpineColors.Error else SpineColors.OnSurfaceVariant
    val labelColor = if (danger) SpineColors.Error else SpineColors.OnSurface

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Leading icon container
        Box(
            modifier = Modifier
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
        // Center label + value
        Column(Modifier.weight(1f)) {
            Text(
                text = label,
                style = TextStyle(
                    fontFamily = SpaceGrotesk,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Medium,
                ),
                color = labelColor,
            )
            if (value.isNotEmpty()) {
                Text(
                    text = value,
                    style = TextStyle(
                        fontFamily = JetBrainsMono,
                        fontSize = 11.sp,
                    ),
                    color = LocalAccentColors.current.primary,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        // Trailing chevron
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.ArrowForwardIos,
            contentDescription = null,
            tint = SpineColors.OnSurfaceMuted,
            modifier = Modifier.size(16.dp),
        )
    }
}

// ---- Search overlay ----------------------------------------------------------

@Composable
private fun SettingsSearchOverlay(
    query: String,
    results: List<SettingsSearchEntry>,
    onQueryChange: (String) -> Unit,
    onResultClick: (SettingsSearchEntry) -> Unit,
    onClose: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = SpineColors.Bg,
    ) {
        Column {
            // Top bar: back arrow + active TextField
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Back",
                        tint = SpineColors.OnSurface,
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier
                        .weight(1f)
                        .background(SpineColors.Surface, ShapeSmall)
                        .border(
                            1.dp,
                            if (query.isNotEmpty()) LocalAccentColors.current.primary
                            else SpineColors.Border,
                            ShapeSmall,
                        )
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    textStyle = MonoSmall.copy(color = SpineColors.OnSurface),
                    cursorBrush = SolidColor(LocalAccentColors.current.primary),
                    singleLine = true,
                    decorationBox = { innerTextField ->
                        if (query.isEmpty()) {
                            Text(
                                "search settings…",
                                style = MonoSmall,
                                color = SpineColors.OnSurfaceMuted,
                            )
                        }
                        innerTextField()
                    },
                )
            }

            if (results.isNotEmpty()) {
                Text(
                    "${results.size} MATCHES",
                    style = MetaMono,
                    color = SpineColors.OnSurfaceMuted,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
                )
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp),
                    shape = ShapeSmall,
                    border = BorderStroke(1.dp, SpineColors.Border),
                ) {
                    LazyColumn {
                        itemsIndexed(results) { index, entry ->
                            if (index > 0) {
                                HorizontalDivider(color = SpineColors.Border, thickness = 1.dp)
                            }
                            SearchHit(
                                entry = entry,
                                query = query,
                                onClick = { onResultClick(entry) },
                            )
                        }
                    }
                }
            } else if (query.isNotBlank()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        "No results for “$query”",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SpineColors.OnSurfaceVariant,
                    )
                    Text(
                        "Try a different keyword",
                        style = MetaMono,
                        color = SpineColors.OnSurfaceMuted,
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchHit(
    entry: SettingsSearchEntry,
    query: String,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(12.dp),
    ) {
        Text(entry.breadcrumb, style = MetaMono, color = SpineColors.OnSurfaceMuted)
        Spacer(Modifier.height(2.dp))
        HighlightedText(
            text = entry.label,
            highlight = query,
            normalColor = SpineColors.OnSurface,
            highlightColor = LocalAccentColors.current.primary,
            highlightBg = LocalAccentColors.current.primary.copy(alpha = 0.25f),
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            entry.hint,
            style = MaterialTheme.typography.bodySmall,
            color = SpineColors.OnSurfaceMuted,
            maxLines = 2,
        )
    }
}

@Composable
private fun HighlightedText(
    text: String,
    highlight: String,
    normalColor: Color,
    highlightColor: Color,
    highlightBg: Color,
    style: TextStyle,
) {
    if (highlight.isBlank()) {
        Text(text, style = style, color = normalColor)
        return
    }
    val idx = text.indexOf(highlight, ignoreCase = true)
    if (idx < 0) {
        Text(text, style = style, color = normalColor)
        return
    }
    val annotated = buildAnnotatedString {
        withStyle(SpanStyle(color = normalColor)) {
            append(text.substring(0, idx))
        }
        withStyle(
            SpanStyle(
                color = highlightColor,
                background = highlightBg,
                fontWeight = FontWeight.SemiBold,
            ),
        ) {
            append(text.substring(idx, idx + highlight.length))
        }
        withStyle(SpanStyle(color = normalColor)) {
            append(text.substring(idx + highlight.length))
        }
    }
    Text(annotated, style = style)
}

// ---- MonoSmall alias (local — avoids extra import) ---------------------------

private val MonoSmall = io.stashapp.android.core.designsystem.theme.MonoSmall
