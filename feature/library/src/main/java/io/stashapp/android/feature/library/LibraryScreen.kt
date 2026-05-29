package io.stashapp.android.feature.library

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import io.stashapp.android.core.designsystem.component.SceneCard
import io.stashapp.android.core.designsystem.component.resolutionLabel
import io.stashapp.android.core.designsystem.theme.MetaMono
import io.stashapp.android.core.designsystem.theme.MonoSmall
import io.stashapp.android.core.designsystem.theme.ShapeSmall
import io.stashapp.android.core.designsystem.theme.SpaceGrotesk
import io.stashapp.android.core.designsystem.theme.SpineColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onSceneClick: (sceneId: String, queueIds: List<String>, index: Int) -> Unit,
    onPlayQueue: (queueIds: List<String>, index: Int) -> Unit = { ids, idx ->
        if (ids.isNotEmpty()) onSceneClick(ids[idx], ids, idx)
    },
    onSettingsClick: () -> Unit,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val ui by viewModel.state.collectAsStateWithLifecycle()
    val scenes = viewModel.scenes.collectAsLazyPagingItems()

    var showFilterSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
    ) { inner ->
        ScenesGrid(
            scenes = scenes,
            inner = inner,
            filterActive = ui.query.filter.isActive,
            onOpenFilter = { showFilterSheet = true },
            onSceneClick = onSceneClick,
            onPlayQueue = onPlayQueue,
        )
    }

    if (showFilterSheet) {
        FilterSheet(
            sheetState = sheetState,
            initialFilter = ui.query.filter,
            initialSort = ui.query.sort,
            hasSavedDefault = ui.hasSavedDefault,
            onDismiss = { showFilterSheet = false },
            onApply = { filter, sort ->
                viewModel.setFilter(filter)
                viewModel.setSort(sort)
            },
            onSaveAsDefault = { viewModel.saveAsDefault() },
            onClearDefault = { viewModel.clearDefault() },
        )
    }
}

@Composable
private fun SpineSearchBar(
    filterActive: Boolean,
    onOpenFilter: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Always-visible search field (decorative — real search in filter sheet)
        Row(
            modifier =
                Modifier
                    .weight(1f)
                    .background(SpineColors.Surface, ShapeSmall)
                    .border(1.dp, SpineColors.Border, ShapeSmall)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .clickable { /* TODO: open search keyboard */ },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Outlined.Search,
                contentDescription = null,
                tint = SpineColors.OnSurfaceMuted,
                modifier = Modifier.size(14.dp),
            )
            Spacer(modifier = Modifier.padding(start = 8.dp))
            Text(
                "search · scene title, performer, tag…",
                style = MonoSmall,
                color = SpineColors.OnSurfaceMuted,
                modifier = Modifier.weight(1f),
            )
            Text(
                "⌘K",
                style = MetaMono,
                color = SpineColors.OnSurfaceMuted,
                modifier =
                    Modifier
                        .background(SpineColors.SurfaceHigh, RoundedCornerShape(3.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp),
            )
        }

        // Filter icon
        androidx.compose.material3.IconButton(onClick = onOpenFilter) {
            Icon(
                Icons.Filled.FilterList,
                contentDescription = "Filters",
                tint = if (filterActive) SpineColors.AccentPrimary else SpineColors.OnSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ScenesGrid(
    scenes: androidx.paging.compose.LazyPagingItems<io.stashapp.android.core.model.SceneSummary>,
    inner: PaddingValues,
    filterActive: Boolean,
    onOpenFilter: () -> Unit,
    onSceneClick: (String, List<String>, Int) -> Unit,
    onPlayQueue: (List<String>, Int) -> Unit,
) {
    val refresh = scenes.loadState.refresh
    val append = scenes.loadState.append

    when {
        refresh is LoadState.Loading && scenes.itemCount == 0 -> {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(inner),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }
        }
        refresh is LoadState.Error && scenes.itemCount == 0 -> {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(inner),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Can't load scenes", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        refresh.error.message ?: "Unknown error",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        scenes.itemCount == 0 && refresh !is LoadState.Loading -> {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(inner),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "No scenes match your filter.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        else -> {
            // Only recompute the id list when the paged count actually changes
            val allIds =
                remember(scenes.itemCount) {
                    (0 until scenes.itemCount).mapNotNull { scenes.peek(it)?.id }
                }

            // resultCount / organizedCount: not yet in LibraryUiState — placeholder zeros
            val resultCount = scenes.itemCount
            val organizedCount = 0 // TODO: wire from LibraryUiState once field exists

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 180.dp),
                contentPadding =
                    PaddingValues(
                        start = 18.dp,
                        end = 18.dp,
                        top = inner.calculateTopPadding(),
                        bottom = inner.calculateBottomPadding() + 100.dp,
                    ),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                // Spine search bar + filter chips as sticky header item
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column {
                        SpineSearchBar(
                            filterActive = filterActive,
                            onOpenFilter = onOpenFilter,
                        )

                        // Filter chip row (active filters visual summary)
                        if (filterActive) {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 18.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(bottom = 4.dp),
                            ) {
                                item {
                                    FilterChip(
                                        selected = true,
                                        onClick = onOpenFilter,
                                        label = {
                                            Text(
                                                "Filters active",
                                                style =
                                                    TextStyle(
                                                        fontFamily = SpaceGrotesk,
                                                        fontSize = 11.5.sp,
                                                        fontWeight = FontWeight.Medium,
                                                    ),
                                            )
                                        },
                                        shape = ShapeSmall,
                                        colors =
                                            FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = SpineColors.AccentPrimary.copy(alpha = 0.12f),
                                                selectedLabelColor = SpineColors.AccentPrimary,
                                            ),
                                        border =
                                            FilterChipDefaults.filterChipBorder(
                                                enabled = true,
                                                selected = true,
                                                selectedBorderColor = SpineColors.AccentPrimary.copy(alpha = 0.30f),
                                            ),
                                    )
                                }
                            }
                        }

                        // Results count row
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 4.dp),
                        ) {
                            Text(
                                "$resultCount results · $organizedCount organized",
                                style = MetaMono,
                                color = SpineColors.OnSurfaceMuted,
                            )
                            Spacer(Modifier.weight(1f))
                            Text(
                                "Grid · auto",
                                style = MetaMono,
                                color = SpineColors.OnSurfaceMuted,
                            )
                        }
                    }
                }

                items(
                    count = scenes.itemCount,
                    key = scenes.itemKey { it.id },
                    contentType = scenes.itemContentType { "scene" },
                ) { index ->
                    val scene = scenes[index] ?: return@items
                    SceneCard(
                        title = scene.displayTitle,
                        screenshotUrl = scene.screenshotUrl,
                        durationSeconds = scene.durationSeconds,
                        resolution = resolutionLabel(scene.width, scene.height),
                        rating100 = scene.rating100,
                        playCount = scene.playCount,
                        resumeFraction =
                            run {
                                val dur = scene.durationSeconds
                                val pos = scene.resumeTimeSeconds
                                if (dur != null && dur > 0 && pos != null) (pos / dur).toFloat() else null
                            },
                        onClick = { onSceneClick(scene.id, allIds, index) },
                        onLongClick = { onPlayQueue(allIds, index) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                if (append is LoadState.Loading) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(64.dp),
                            contentAlignment = Alignment.Center,
                        ) { CircularProgressIndicator() }
                    }
                }
            }
        }
    }
}
