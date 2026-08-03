package io.stashapp.android.feature.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.PowerOff
import androidx.compose.material.icons.outlined.Remove
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.stashapp.android.core.data.prefs.PlayerPreferences
import io.stashapp.android.core.designsystem.component.CSlider
import io.stashapp.android.core.designsystem.component.SectionLabel
import io.stashapp.android.core.designsystem.component.SegmentedRail
import io.stashapp.android.core.designsystem.component.SpineChip
import io.stashapp.android.core.designsystem.theme.EmberAccent
import io.stashapp.android.core.designsystem.theme.LocalAccentColors
import io.stashapp.android.core.designsystem.theme.MetaMono
import io.stashapp.android.core.designsystem.theme.MonoSmall
import io.stashapp.android.core.designsystem.theme.SageAccent
import io.stashapp.android.core.designsystem.theme.ShapeSmall
import io.stashapp.android.core.designsystem.theme.SignalAccent
import io.stashapp.android.core.designsystem.theme.SpineColors
import io.stashapp.android.core.domain.SavedFilterPreset
import io.stashapp.android.core.model.ServerInfo
import io.stashapp.android.core.model.StashServer
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
    val serverInfoFetchedAt by viewModel.serverInfoFetchedAt.collectAsStateWithLifecycle()
    val serverLatencyMs by viewModel.serverLatencyMs.collectAsStateWithLifecycle()
    val doubleTap by pp.doubleTapSeekSeconds.collectAsStateWithLifecycle(10)
    val seekMs by pp.seekMsPerPx.collectAsStateWithLifecycle(120f)
    val accentPalette by up.accentPalette.collectAsStateWithLifecycle("sage")
    val gridColumns by up.gridColumns.collectAsStateWithLifecycle("auto")
    val cacheMb by up.imageCacheSizeMb.collectAsStateWithLifecycle(256)
    val blurThumbnails by up.blurThumbnails.collectAsStateWithLifecycle(true)
    val cacheSizeBytes by viewModel.cacheSizeBytes.collectAsStateWithLifecycle()
    val presets by viewModel.presets.collectAsStateWithLifecycle()
    var confirmDisconnect by remember { mutableStateOf(false) }
    var editingPreset by remember { mutableStateOf<SavedFilterPreset?>(null) }
    var managingPresets by remember { mutableStateOf(false) }

    LazyColumn(contentPadding = PaddingValues(bottom = 100.dp)) {
        item { SettingsHeader(serverInfo, serverInfoFetchedAt, onBack) }
        item { ServerSection(activeServer, serverInfo, serverLatencyMs, onServerClick) }
        item { AccentSection(accentPalette, viewModel::setAccentPalette) }
        item { GridSection(gridColumns) { viewModel.setUi { setGridColumns(it) } } }
        item {
            PresetsSection(
                presets = presets,
                onEdit = { editingPreset = it },
                onManage = { managingPresets = true },
            )
        }
        item { DoubleTapSection(doubleTap) { viewModel.setPlayer { setDoubleTapSeekSeconds(it) } } }
        item { ScrubSection(seekMs) { viewModel.setPlayer { setSeekMsPerPx(it) } } }
        item { CacheSection(cacheMb, cacheSizeBytes, viewModel::setImageCacheSizeMb, viewModel::clearImageCache) }
        item { BlurSection(blurThumbnails, viewModel::setBlurThumbnails) }
        item { AppSection(activeServer, onServerClick, onAboutClick) }
        item { DangerSection { confirmDisconnect = true } }
    }

    if (confirmDisconnect) {
        ConfirmationDialog(
            title = "Disconnect server?",
            body = "You will need the server URL and API key to reconnect.",
            confirmLabel = "Disconnect",
            danger = true,
            onDismiss = { confirmDisconnect = false },
            onConfirm = {
                confirmDisconnect = false
                viewModel.disconnect(onDisconnected)
            },
        )
    }
    editingPreset?.let { preset ->
        PresetEditDialog(
            preset = preset,
            onDismiss = { editingPreset = null },
            onRename = { name ->
                viewModel.renamePreset(preset.id, name)
                editingPreset = null
            },
            onDelete = {
                viewModel.deletePreset(preset.id)
                editingPreset = null
            },
        )
    }
    if (managingPresets) {
        ManagePresetsDialog(
            presets = presets,
            onDismiss = { managingPresets = false },
            onSelect = {
                managingPresets = false
                editingPreset = it
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsHeader(
    serverInfo: ServerInfo?,
    fetchedAt: Long?,
    onBack: () -> Unit,
) {
    Column {
        TopAppBar(
            title = {
                Text(
                    "SETTINGS",
                    style = MetaMono.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.2.sp),
                    color = SpineColors.OnSurface,
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back", Modifier.size(18.dp), SpineColors.OnSurface)
                }
            },
            actions = {
                Row(Modifier.padding(end = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(6.dp)
                            .background(
                                if (serverInfo == null) SpineColors.OnSurfaceMuted else SpineColors.Success,
                                RoundedCornerShape(50),
                            ),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (serverInfo == null) "offline" else "synced ${relativeTime(fetchedAt)}",
                        style = MetaMono,
                        color = if (serverInfo == null) SpineColors.OnSurfaceMuted else SpineColors.OnSurfaceVariant,
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = SpineColors.Bg),
        )
        HorizontalDivider(color = SpineColors.Border)
    }
}

@Composable
private fun ServerSection(
    server: StashServer?,
    info: ServerInfo?,
    latencyMs: Long?,
    onClick: () -> Unit,
) {
    SettingsSection("Server") {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            onClick = onClick,
            enabled = server != null,
            color = SpineColors.Surface,
            shape = ShapeSmall,
            border = BorderStroke(1.dp, SpineColors.Border),
        ) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        server?.displayName ?: "Not connected",
                        style = MetaMono.copy(fontSize = 12.5.sp, fontWeight = FontWeight.Medium),
                        color = SpineColors.OnSurface,
                    )
                    server?.let {
                        Spacer(Modifier.height(5.dp))
                        val meta =
                            info?.let {
                                "stash v${it.version} · ${it.sceneCount} scenes" +
                                    (latencyMs?.let { latency -> " · ${latency}ms" } ?: "")
                            } ?: "connected · checking server"
                        Text(meta, style = MonoSmall, color = SpineColors.OnSurfaceVariant)
                    }
                }
                Icon(
                    Icons.AutoMirrored.Outlined.ArrowForwardIos,
                    contentDescription = null,
                    tint = SpineColors.OnSurfaceMuted,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}

@Composable
private fun AccentSection(
    selectedPalette: String,
    onSelect: (String) -> Unit,
) {
    SettingsSection("Accent") {
        val palettes =
            listOf(
                Triple("sage", "sage", SageAccent.primary),
                Triple("ember", "ember", EmberAccent.primary),
                Triple("signal", "signal", SignalAccent.primary),
            )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            palettes.forEach { (id, name, color) ->
                val selected = selectedPalette == id
                Surface(
                    modifier = Modifier.weight(1f),
                    onClick = { onSelect(id) },
                    color = if (selected) color.copy(alpha = 0.12f) else SpineColors.Surface,
                    shape = ShapeSmall,
                    border = BorderStroke(1.dp, if (selected) color.copy(alpha = 0.45f) else SpineColors.Border),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.size(16.dp).background(color, RoundedCornerShape(4.dp)))
                        Text(
                            name,
                            style = MetaMono.copy(fontSize = 11.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal),
                            color = if (selected) color else SpineColors.OnSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GridSection(
    value: String,
    onSelect: (String) -> Unit,
) {
    SettingsSection("Grid columns") {
        SegmentedRail(listOf("auto" to "auto", "2" to "2", "3" to "3", "4" to "4"), value, onSelect)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PresetsSection(
    presets: List<SavedFilterPreset>,
    onEdit: (SavedFilterPreset) -> Unit,
    onManage: () -> Unit,
) {
    SettingsSection("Saved filter presets") {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            presets.forEach { SpineChip(it.name, false, { onEdit(it) }) }
            SpineChip("manage…", false, onManage, dashed = true)
        }
    }
}

@Composable
private fun DoubleTapSection(
    value: Int,
    onSelect: (Int) -> Unit,
) {
    SettingsSection("Double-tap seek") {
        SegmentedRail(listOf(5, 10, 15, 30, 60).map { "${it}s" to it }, value, onSelect)
    }
}

@Composable
private fun ScrubSection(
    value: Float,
    onChange: (Float) -> Unit,
) {
    SettingsSection("Scrub sensitivity", trailing = { ValueBubble("${value.roundToInt()} ms/px") }) {
        StepSlider(value, PlayerPreferences.SEEK_MS_PER_PX_MIN..PlayerPreferences.SEEK_MS_PER_PX_MAX, onChange)
        Text(
            "drag the rail, or step by 10 with the ends",
            style = MonoSmall,
            color = SpineColors.OnSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

@Composable
private fun CacheSection(
    cacheMb: Int,
    cacheSizeBytes: Long,
    onResize: (Int) -> Unit,
    onClear: () -> Unit,
) {
    SettingsSection("Thumbnail cache", trailing = { ValueBubble("$cacheMb MB") }) {
        SegmentedRail(listOf(64, 128, 256, 384, 512).map { it.toString() to it }, cacheMb, onResize)
        Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("${cacheSizeBytes / (1024 * 1024)} MB in use · ", style = MonoSmall, color = SpineColors.OnSurfaceVariant)
            Text(
                "clear cache",
                style = MonoSmall.copy(fontWeight = FontWeight.Medium),
                color = SpineColors.OnSurfaceVariant,
                modifier = Modifier.clickable(onClick = onClear).padding(vertical = 6.dp),
            )
        }
    }
}

@Composable
private fun BlurSection(
    enabled: Boolean,
    onChange: (Boolean) -> Unit,
) {
    SettingsSection("Blur thumbnails") {
        Surface(color = SpineColors.Surface, shape = ShapeSmall, border = BorderStroke(1.dp, SpineColors.Border)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "Safe for screenshots",
                        style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                        color = SpineColors.OnSurface,
                    )
                    Text("Obscures all remote imagery", style = MonoSmall, color = SpineColors.OnSurfaceVariant)
                }
                SpineSwitch(checked = enabled, onCheckedChange = onChange)
            }
        }
    }
}

@Composable
private fun AppSection(
    server: StashServer?,
    onServerClick: () -> Unit,
    onAboutClick: () -> Unit,
) {
    SettingsSection("App") {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            AppRow("Server & connection", if (server?.apiKey.isNullOrBlank()) "no api key" else "api key set", onServerClick)
            AppRow("About & diagnostics", "v0.2.0-alpha", onAboutClick)
        }
    }
}

@Composable
private fun DangerSection(onDisconnect: () -> Unit) {
    SettingsSection("Danger zone", bottomPadding = 36.dp) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            onClick = onDisconnect,
            color = SpineColors.Error.copy(alpha = 0.06f),
            shape = ShapeSmall,
            border = BorderStroke(1.dp, SpineColors.Error.copy(alpha = 0.30f)),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.PowerOff, null, Modifier.size(16.dp), SpineColors.Error)
                Text(
                    "Disconnect server",
                    style =
                        androidx.compose.material3.MaterialTheme.typography.bodyMedium
                            .copy(fontWeight = FontWeight.Medium),
                    color = SpineColors.Error,
                )
            }
        }
    }
}

@Composable
private fun SettingsSection(
    label: String,
    bottomPadding: androidx.compose.ui.unit.Dp = 0.dp,
    trailing: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, top = 18.dp, bottom = bottomPadding),
    ) {
        SectionLabel(label) {
            if (trailing != null) {
                Spacer(Modifier.weight(1f))
                trailing()
            }
        }
        content()
    }
}

@Composable
private fun ValueBubble(label: String) {
    val accent = LocalAccentColors.current
    Text(
        text = label,
        style = MonoSmall.copy(fontWeight = FontWeight.SemiBold),
        color = accent.primary,
        modifier =
            Modifier
                .background(accent.primary.copy(alpha = 0.08f), RoundedCornerShape(4.dp))
                .border(1.dp, accent.primary.copy(alpha = 0.25f), RoundedCornerShape(4.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp),
    )
}

@Composable
private fun StepSlider(
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
) {
    Surface(
        color = SpineColors.Surface,
        shape = ShapeSmall,
        border = BorderStroke(1.dp, SpineColors.Border),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StepIcon(Icons.Outlined.Remove, "Decrease scrub sensitivity") {
                onValueChange((value - 10f).coerceAtLeast(valueRange.start))
            }
            Box(Modifier.width(1.dp).height(44.dp).background(SpineColors.Border))
            CSlider(
                value = value,
                onValueChange = onValueChange,
                valueRange = valueRange,
                valueLabel = "",
                showValue = false,
                modifier = Modifier.weight(1f),
            )
            Box(Modifier.width(1.dp).height(44.dp).background(SpineColors.Border))
            StepIcon(Icons.Outlined.Add, "Increase scrub sensitivity") {
                onValueChange((value + 10f).coerceAtMost(valueRange.endInclusive))
            }
        }
    }
}

@Composable
private fun StepIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier.size(width = 38.dp, height = 44.dp).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = description, tint = SpineColors.OnSurfaceVariant, modifier = Modifier.size(14.dp))
    }
}

@Composable
private fun AppRow(
    label: String,
    value: String,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        color = SpineColors.Surface,
        shape = ShapeSmall,
        border = BorderStroke(1.dp, SpineColors.Border),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                label,
                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                color = SpineColors.OnSurface,
                modifier = Modifier.weight(1f),
            )
            Text(value, style = MetaMono, color = SpineColors.OnSurfaceVariant)
            Spacer(Modifier.width(10.dp))
            Icon(
                Icons.AutoMirrored.Outlined.ArrowForwardIos,
                contentDescription = null,
                tint = SpineColors.OnSurfaceMuted,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

@Composable
private fun ConfirmationDialog(
    title: String,
    body: String,
    confirmLabel: String,
    danger: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = SpineColors.SurfaceTop,
            shape = ShapeSmall,
            border = BorderStroke(1.dp, SpineColors.BorderStrong),
        ) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(title, style = androidx.compose.material3.MaterialTheme.typography.titleMedium, color = SpineColors.OnSurface)
                Text(body, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium, color = SpineColors.OnSurfaceVariant)
                DialogActions(
                    confirmLabel = confirmLabel,
                    confirmColor = if (danger) SpineColors.Error else LocalAccentColors.current.primary,
                    onDismiss = onDismiss,
                    onConfirm = onConfirm,
                )
            }
        }
    }
}

@Composable
private fun DialogActions(
    confirmLabel: String,
    confirmColor: Color,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
        Text(
            "Cancel",
            style = MetaMono.copy(fontSize = 11.sp),
            color = SpineColors.OnSurfaceVariant,
            modifier = Modifier.clickable(onClick = onDismiss).padding(10.dp),
        )
        Surface(onClick = onConfirm, color = confirmColor, shape = ShapeSmall) {
            Text(
                confirmLabel,
                style = MetaMono.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                color = if (confirmColor == SpineColors.Error) SpineColors.OnSurface else LocalAccentColors.current.onPrimary,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            )
        }
    }
}

@Composable
private fun PresetEditDialog(
    preset: SavedFilterPreset,
    onDismiss: () -> Unit,
    onRename: (String) -> Unit,
    onDelete: () -> Unit,
) {
    var name by remember(preset.id) { mutableStateOf(preset.name) }
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = SpineColors.SurfaceTop,
            shape = ShapeSmall,
            border = BorderStroke(1.dp, SpineColors.BorderStrong),
        ) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionLabel("Edit saved preset")
                BasicTextField(
                    value = name,
                    onValueChange = { name = it.take(40) },
                    singleLine = true,
                    textStyle = MetaMono.copy(fontSize = 12.sp, color = SpineColors.OnSurface),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .background(SpineColors.Surface, ShapeSmall)
                            .border(1.dp, SpineColors.Border, ShapeSmall)
                            .padding(horizontal = 12.dp, vertical = 11.dp),
                )
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Delete",
                        style = MetaMono.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium),
                        color = SpineColors.Error,
                        modifier = Modifier.clickable(onClick = onDelete).padding(10.dp),
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        "Cancel",
                        style = MetaMono.copy(fontSize = 11.sp),
                        color = SpineColors.OnSurfaceVariant,
                        modifier = Modifier.clickable(onClick = onDismiss).padding(10.dp),
                    )
                    val accent = LocalAccentColors.current
                    Surface(
                        onClick = { onRename(name.trim()) },
                        enabled = name.trim().isNotEmpty(),
                        color = accent.primary,
                        contentColor = accent.onPrimary,
                        shape = ShapeSmall,
                    ) {
                        Text(
                            "Rename",
                            style = MetaMono.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ManagePresetsDialog(
    presets: List<SavedFilterPreset>,
    onDismiss: () -> Unit,
    onSelect: (SavedFilterPreset) -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = SpineColors.SurfaceTop,
            shape = ShapeSmall,
            border = BorderStroke(1.dp, SpineColors.BorderStrong),
        ) {
            Column(
                Modifier.padding(18.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                SectionLabel("Saved filter presets")
                if (presets.isEmpty()) {
                    Text("No saved presets yet", style = MetaMono, color = SpineColors.OnSurfaceMuted)
                } else {
                    presets.forEach { preset ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { onSelect(preset) },
                            color = SpineColors.Surface,
                            shape = ShapeSmall,
                            border = BorderStroke(1.dp, SpineColors.Border),
                        ) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    preset.name,
                                    style = MetaMono.copy(fontSize = 11.sp),
                                    color = SpineColors.OnSurface,
                                    modifier = Modifier.weight(1f),
                                )
                                Icon(
                                    Icons.AutoMirrored.Outlined.ArrowForwardIos,
                                    contentDescription = "Edit ${preset.name}",
                                    tint = SpineColors.OnSurfaceMuted,
                                    modifier = Modifier.size(14.dp),
                                )
                            }
                        }
                    }
                }
                Text(
                    "Close",
                    style = MetaMono.copy(fontSize = 11.sp),
                    color = SpineColors.OnSurfaceVariant,
                    modifier = Modifier.align(Alignment.End).clickable(onClick = onDismiss).padding(10.dp),
                )
            }
        }
    }
}

private fun relativeTime(fetchedAt: Long?): String {
    if (fetchedAt == null) return "now"
    val elapsedSeconds = ((System.currentTimeMillis() - fetchedAt).coerceAtLeast(0L) / 1_000)
    return when {
        elapsedSeconds < 60 -> "now"
        elapsedSeconds < 3_600 -> "${elapsedSeconds / 60}m ago"
        elapsedSeconds < 86_400 -> "${elapsedSeconds / 3_600}h ago"
        else -> "${elapsedSeconds / 86_400}d ago"
    }
}

/** Themed Switch used by this screen and the Server/About sub-screens. */
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

/** Horizontal scrollable chip row retained for settings sub-screens. */
@Composable
internal fun <T> ChipRow(
    options: List<Pair<String, T>>,
    selected: T,
    onSelect: (T) -> Unit,
) {
    val accent = LocalAccentColors.current
    androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items(options.size) { i ->
            val (label, value) = options[i]
            val isActive = value == selected
            Surface(
                onClick = { onSelect(value) },
                shape = ShapeSmall,
                color = if (isActive) accent.primary.copy(alpha = 0.12f) else SpineColors.SurfaceHigh,
                border = BorderStroke(1.dp, if (isActive) accent.primary else SpineColors.Border),
            ) {
                Text(
                    text = label,
                    style = MetaMono.copy(fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal),
                    color = if (isActive) accent.primary else SpineColors.OnSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                )
            }
        }
    }
}

/** Bordered settings group retained for the Server/About sub-screens. */
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
                title?.let {
                    Text(
                        text = it.uppercase(),
                        style = MetaMono.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp),
                        color = SpineColors.OnSurfaceMuted,
                    )
                }
                badge?.let {
                    Spacer(Modifier.padding(start = 8.dp))
                    Text(
                        text = it,
                        style = MetaMono,
                        color = accent.primary,
                        modifier =
                            Modifier
                                .background(accent.primary.copy(alpha = 0.10f), ShapeSmall)
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }
        }
        Surface(color = SpineColors.Surface, shape = ShapeSmall, border = BorderStroke(1.dp, SpineColors.Border)) {
            Column(modifier = Modifier.fillMaxWidth()) { content() }
        }
    }
}
