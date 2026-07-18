package io.stashapp.android.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.stashapp.android.core.designsystem.component.SceneCard
import io.stashapp.android.core.designsystem.component.SpineResumeCard
import io.stashapp.android.core.designsystem.component.resolutionLabel
import io.stashapp.android.core.designsystem.theme.MetaMono
import io.stashapp.android.core.designsystem.theme.ShapeSmall
import io.stashapp.android.core.designsystem.theme.SpineColors
import io.stashapp.android.core.designsystem.theme.LocalAccentColors
import io.stashapp.android.core.model.SceneSummary
import kotlinx.collections.immutable.ImmutableList

/**
 * Home dashboard. Shows a fixed set of rails modeled after Stash's web UI
 * default front page (recently released, recently added, etc.) plus a
 * "continue watching" rail backed by the resume_time > 0 filter.
 *
 * Each rail is horizontally scrollable. Tapping a card opens scene detail;
 * long-pressing launches the player with that rail as the queue.
 *
 * Top bar is an inline Row (Spine design) — no TopAppBar/Scaffold topBar.
 */
@Composable
fun HomeScreen(
    onSceneClick: (sceneId: String) -> Unit,
    onPlayQueue: (queueIds: List<String>, index: Int) -> Unit,
    onSettingsClick: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
    ) { inner ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding =
                PaddingValues(
                    top = inner.calculateTopPadding(),
                    bottom = inner.calculateBottomPadding() + 16.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            // Inline Spine top bar — replaces TopAppBar
            item(key = "top_bar") {
                SpineTopBar(
                    onSearchClick = { /* TODO: wire to search */ },
                    onRefreshClick = viewModel::load,
                )
            }

            // SpineResumeCard slot — gated on resume state
            // TODO: wire to HomeUiState.resumeScene once that field is added
            if (false) {
                item(key = "resume_card") {
                    SpineResumeCard(
                        thumbnailUrl = "",
                        title = "",
                        studio = null,
                        remainingLabel = "",
                        progress = 0f,
                        onClick = {},
                        modifier = Modifier.padding(horizontal = 18.dp),
                    )
                }
            }

            items(
                items = state.rails,
                key = { it.kind.name },
            ) { rail ->
                HomeRailRow(rail, onSceneClick, onPlayQueue)
            }
        }
    }
}

@Composable
private fun SpineTopBar(
    onSearchClick: () -> Unit,
    onRefreshClick: () -> Unit,
) {
    val accent = LocalAccentColors.current
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, start = 18.dp, end = 18.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Logo: rotated-diamond accent shape
        Box(
            modifier =
                Modifier
                    .size(22.dp)
                    .rotate(45f)
                    .clip(ShapeSmall)
                    .background(accent.primary),
        )
        Spacer(Modifier.width(8.dp))
        Text("Slopper", style = MaterialTheme.typography.titleLarge)

        // Server name badge — no server field in HomeUiState yet; omit when blank
        val serverHost = "" // TODO: wire from HomeUiState once server field is added
        if (serverHost.isNotBlank()) {
            Spacer(Modifier.width(8.dp))
            Text(
                text = serverHost,
                style = MetaMono,
                color = SpineColors.OnSurfaceVariant,
                modifier =
                    Modifier
                        .background(SpineColors.Surface, RoundedCornerShape(4.dp))
                        .border(1.dp, SpineColors.Border, RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }

        Spacer(Modifier.weight(1f))

        IconButton(
            onClick = onSearchClick,
            modifier = Modifier.size(32.dp),
        ) {
            Icon(
                Icons.Outlined.Search,
                contentDescription = "Search",
                tint = SpineColors.OnSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
        IconButton(
            onClick = onRefreshClick,
            modifier = Modifier.size(32.dp),
        ) {
            Icon(
                Icons.Outlined.Refresh,
                contentDescription = "Refresh",
                tint = SpineColors.OnSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun HomeRailRow(
    rail: HomeRail,
    onSceneClick: (String) -> Unit,
    onPlayQueue: (List<String>, Int) -> Unit,
) {
    // Hide empty rails entirely — no point taking vertical real estate for
    // e.g. an empty "Continue watching" if the user has nothing in progress.
    if (!rail.loading && rail.scenes.isEmpty() && rail.error == null) return

    Column {
        // Spine-styled section header: title + count badge + chevron
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                rail.kind.title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            )
            Spacer(Modifier.width(6.dp))
            if (!rail.loading && rail.scenes.isNotEmpty()) {
                Text(
                    text = "${rail.scenes.size}",
                    style = MetaMono,
                    color = SpineColors.OnSurfaceMuted,
                    modifier =
                        Modifier
                            .background(SpineColors.Surface, RoundedCornerShape(3.dp))
                            .padding(horizontal = 5.dp, vertical = 1.dp),
                )
            }
            Spacer(Modifier.weight(1f))
            Icon(
                Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = SpineColors.OnSurfaceFaint,
                modifier = Modifier.size(16.dp),
            )
        }

        Spacer(Modifier.height(10.dp))

        when {
            rail.loading -> RailLoading()
            rail.error != null -> RailError(rail.error)
            else -> RailScenes(rail.scenes, onSceneClick, onPlayQueue)
        }
    }
}

@Composable
private fun RailLoading() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(140.dp),
        contentAlignment = Alignment.Center,
    ) { CircularProgressIndicator(strokeWidth = 2.dp) }
}

@Composable
private fun RailError(message: String) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(80.dp)
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            "Couldn't load: $message",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
    }
}

@Composable
private fun RailScenes(
    scenes: ImmutableList<SceneSummary>,
    onSceneClick: (String) -> Unit,
    onPlayQueue: (List<String>, Int) -> Unit,
) {
    val ids = scenes.map { it.id }
    LazyRow(
        contentPadding = PaddingValues(horizontal = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(count = scenes.size, key = { scenes[it].id }) { index ->
            val scene = scenes[index]
            val resumeFraction =
                run {
                    val dur = scene.durationSeconds
                    val pos = scene.resumeTimeSeconds
                    if (dur != null && dur > 0 && pos != null) (pos / dur).toFloat() else null
                }
            Box(Modifier.width(180.dp)) {
                SceneCard(
                    title = scene.displayTitle,
                    screenshotUrl = scene.screenshotUrl,
                    durationSeconds = scene.durationSeconds,
                    resolution = resolutionLabel(scene.width, scene.height),
                    rating100 = scene.rating100,
                    playCount = scene.playCount,
                    resumeFraction = resumeFraction,
                    onClick = { onSceneClick(scene.id) },
                    onLongClick = { onPlayQueue(ids, index) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
