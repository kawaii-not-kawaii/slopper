package io.stashapp.android.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.stashapp.android.core.designsystem.component.SceneCard
import io.stashapp.android.core.designsystem.component.resolutionLabel
import io.stashapp.android.core.model.SceneSummary
import kotlinx.collections.immutable.ImmutableList

/**
 * Home dashboard. Shows a fixed set of rails modeled after Stash's web UI
 * default front page (recently released, recently added, etc.) plus a
 * "continue watching" rail backed by the resume_time > 0 filter.
 *
 * Each rail is horizontally scrollable. Tapping a card opens scene detail;
 * long-pressing launches the player with that rail as the queue.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onSceneClick: (sceneId: String) -> Unit,
    onPlayQueue: (queueIds: List<String>, index: Int) -> Unit,
    onSettingsClick: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Home") },
                actions = {
                    IconButton(onClick = viewModel::load) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
            )
        },
    ) { inner ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding =
                PaddingValues(
                    top = inner.calculateTopPadding() + 8.dp,
                    bottom = inner.calculateBottomPadding() + 16.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            items(
                items = state.rails,
                key = { it.kind.name },
            ) { rail -> HomeRailRow(rail, onSceneClick, onPlayQueue) }
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
        Text(
            rail.kind.title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
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
        Modifier.fillMaxWidth().height(140.dp),
        contentAlignment = Alignment.Center,
    ) { CircularProgressIndicator(strokeWidth = 2.dp) }
}

@Composable
private fun RailError(message: String) {
    Box(
        Modifier.fillMaxWidth().height(80.dp).padding(horizontal = 16.dp),
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
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(count = scenes.size, key = { scenes[it].id }) { index ->
            val scene = scenes[index]
            val resumeFraction =
                run {
                    val dur = scene.durationSeconds
                    val pos = scene.resumeTimeSeconds
                    if (dur != null && dur > 0 && pos != null) (pos / dur).toFloat() else null
                }
            Box(Modifier.width(220.dp)) {
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
