package io.stashapp.android.feature.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import io.stashapp.android.core.designsystem.component.SceneCard
import io.stashapp.android.core.designsystem.component.resolutionLabel

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
        topBar = {
            LibraryTopBar(
                searchExpanded = ui.searchExpanded,
                searchText = ui.searchText,
                filterActive = ui.query.filter.isActive,
                onToggleSearch = { viewModel.setSearchExpanded(!ui.searchExpanded) },
                onSearchChange = viewModel::setSearchText,
                onClearSearch = { viewModel.setSearchText("") },
                onOpenFilter = { showFilterSheet = true },
                onSettings = onSettingsClick,
            )
        },
    ) { inner ->
        ScenesGrid(
            scenes = scenes,
            inner = inner,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LibraryTopBar(
    searchExpanded: Boolean,
    searchText: String,
    filterActive: Boolean,
    onToggleSearch: () -> Unit,
    onSearchChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    onOpenFilter: () -> Unit,
    onSettings: () -> Unit,
) {
    TopAppBar(
        title = {
            if (searchExpanded) {
                OutlinedTextField(
                    value = searchText,
                    onValueChange = onSearchChange,
                    placeholder = { Text("Search scenes") },
                    singleLine = true,
                    trailingIcon = {
                        if (searchText.isNotEmpty()) {
                            IconButton(onClick = onClearSearch) {
                                Icon(Icons.Filled.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                Text("Library")
            }
        },
        actions = {
            IconButton(onClick = onToggleSearch) {
                Icon(
                    if (searchExpanded) Icons.Filled.Clear else Icons.Filled.Search,
                    contentDescription = if (searchExpanded) "Close search" else "Search",
                )
            }
            BadgedBox(badge = { if (filterActive) Badge() }) {
                IconButton(onClick = onOpenFilter) {
                    Icon(Icons.Filled.FilterList, contentDescription = "Filters")
                }
            }
            IconButton(onClick = onSettings) {
                Icon(Icons.Filled.Settings, contentDescription = "Settings")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    )
}

@Composable
private fun ScenesGrid(
    scenes: androidx.paging.compose.LazyPagingItems<io.stashapp.android.core.model.SceneSummary>,
    inner: PaddingValues,
    onSceneClick: (String, List<String>, Int) -> Unit,
    onPlayQueue: (List<String>, Int) -> Unit,
) {
    val refresh = scenes.loadState.refresh
    val append = scenes.loadState.append

    when {
        refresh is LoadState.Loading && scenes.itemCount == 0 -> {
            Box(
                Modifier.fillMaxSize().padding(inner),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }
        }
        refresh is LoadState.Error && scenes.itemCount == 0 -> {
            Box(
                Modifier.fillMaxSize().padding(inner),
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
        scenes.itemCount == 0 -> {
            Box(
                Modifier.fillMaxSize().padding(inner),
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
            // Only recompute the id list when the paged count actually changes;
            // otherwise we allocate a fresh List on every 250ms-ish recomposition
            // as pages get appended.
            val allIds = remember(scenes.itemCount) {
                (0 until scenes.itemCount).mapNotNull { scenes.peek(it)?.id }
            }

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 180.dp),
                contentPadding = PaddingValues(
                    start = 12.dp,
                    end = 12.dp,
                    top = inner.calculateTopPadding() + 8.dp,
                    bottom = inner.calculateBottomPadding() + 12.dp,
                ),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(
                    count = scenes.itemCount,
                    key = scenes.itemKey { it.id },
                    // Single content type — lets LazyVerticalGrid recycle
                    // SceneCard slot holders across pages instead of rebuilding
                    // them during scroll.
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
                        resumeFraction = run {
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
                            Modifier.fillMaxWidth().height(64.dp),
                            contentAlignment = Alignment.Center,
                        ) { CircularProgressIndicator() }
                    }
                }
            }
        }
    }
}
