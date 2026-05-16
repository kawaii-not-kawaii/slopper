package io.stashapp.android.feature.browse

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import coil3.compose.AsyncImage
import io.stashapp.android.core.designsystem.theme.StashColors
import io.stashapp.android.core.model.PerformerBrowseItem
import io.stashapp.android.core.model.StudioBrowseItem
import io.stashapp.android.core.model.TagBrowseItem

/**
 * Single entry point for all three browse surfaces — parameterized by
 * [BrowseKind] via the nav route. One code path, three data flows, so the
 * grid/search/empty-state logic doesn't get duplicated.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowseScreen(
    onBack: () -> Unit,
    onPerformerClick: (id: String) -> Unit,
    onStudioClick: (id: String) -> Unit,
    onTagClick: (id: String) -> Unit,
    viewModel: BrowseViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    var searchExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (searchExpanded) {
                        OutlinedTextField(
                            value = ui.search,
                            onValueChange = viewModel::setSearch,
                            placeholder = { Text("Search ${titleFor(ui.kind).lowercase()}") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                if (ui.search.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.setSearch("") }) {
                                        Icon(Icons.Filled.Clear, contentDescription = "Clear")
                                    }
                                }
                            },
                        )
                    } else {
                        Text(titleFor(ui.kind))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { searchExpanded = !searchExpanded }) {
                        Icon(
                            if (searchExpanded) Icons.Filled.Clear else Icons.Filled.Search,
                            contentDescription = if (searchExpanded) "Close search" else "Search",
                        )
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
            )
        },
    ) { inner ->
        when (ui.kind) {
            BrowseKind.Performers -> {
                val items = viewModel.performers.collectAsLazyPagingItems()
                PerformersGrid(items, inner, onPerformerClick)
            }
            BrowseKind.Studios -> {
                val items = viewModel.studios.collectAsLazyPagingItems()
                StudiosGrid(items, inner, onStudioClick)
            }
            BrowseKind.Tags -> {
                val items = viewModel.tags.collectAsLazyPagingItems()
                TagsGrid(items, inner, onTagClick)
            }
        }
    }
}

private fun titleFor(kind: BrowseKind) =
    when (kind) {
        BrowseKind.Performers -> "Performers"
        BrowseKind.Studios -> "Studios"
        BrowseKind.Tags -> "Tags"
    }

@Composable
private fun PerformersGrid(
    items: LazyPagingItems<PerformerBrowseItem>,
    inner: PaddingValues,
    onClick: (String) -> Unit,
) {
    PagingGrid(items, inner, keyOf = { it.id }) { performer, mod ->
        Column(
            modifier = mod,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f),
                onClick = { onClick(performer.id) },
            ) {
                Box {
                    AsyncImage(
                        model = performer.imageUrl,
                        contentDescription = performer.name,
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                    )
                    if (performer.favorite) {
                        Icon(
                            Icons.Filled.Favorite,
                            contentDescription = null,
                            tint = StashColors.Error,
                            modifier =
                                Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                                    .size(18.dp),
                        )
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                performer.name,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
            Text(
                "${performer.sceneCount} scenes",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun StudiosGrid(
    items: LazyPagingItems<StudioBrowseItem>,
    inner: PaddingValues,
    onClick: (String) -> Unit,
) {
    PagingGrid(items, inner, keyOf = { it.id }) { studio, mod ->
        Column(modifier = mod) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f),
                onClick = { onClick(studio.id) },
            ) {
                AsyncImage(
                    model = studio.imageUrl,
                    contentDescription = studio.name,
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                studio.name,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "${studio.sceneCount} scenes",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TagsGrid(
    items: LazyPagingItems<TagBrowseItem>,
    inner: PaddingValues,
    onClick: (String) -> Unit,
) {
    PagingGrid(items, inner, keyOf = { it.id }) { tag, mod ->
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier = mod.fillMaxWidth(),
            onClick = { onClick(tag.id) },
        ) {
            Column(Modifier.padding(12.dp)) {
                Text(
                    tag.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = StashColors.AccentSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "${tag.sceneCount} scenes",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Shared grid wrapper — handles loading / error / empty states once. */
@Composable
private fun <T : Any> PagingGrid(
    items: LazyPagingItems<T>,
    inner: PaddingValues,
    keyOf: (T) -> String,
    itemContent: @Composable (T, Modifier) -> Unit,
) {
    when {
        items.loadState.refresh is androidx.paging.LoadState.Loading && items.itemCount == 0 -> {
            Box(
                Modifier.fillMaxSize().padding(inner),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }
        }
        items.loadState.refresh is androidx.paging.LoadState.Error && items.itemCount == 0 -> {
            val err = items.loadState.refresh as androidx.paging.LoadState.Error
            Box(
                Modifier.fillMaxSize().padding(inner),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    err.error.message ?: "Failed to load",
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
        items.itemCount == 0 -> {
            Box(
                Modifier.fillMaxSize().padding(inner),
                contentAlignment = Alignment.Center,
            ) { Text("No results", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        else -> {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 140.dp),
                contentPadding =
                    PaddingValues(
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
                    count = items.itemCount,
                    key = items.itemKey { keyOf(it) },
                ) { index ->
                    val entry = items[index] ?: return@items
                    itemContent(entry, Modifier)
                }
            }
        }
    }
}
