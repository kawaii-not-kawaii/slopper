package io.stashapp.android.feature.browse

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import coil3.compose.AsyncImage
import io.stashapp.android.core.designsystem.theme.MetaMono
import io.stashapp.android.core.designsystem.theme.ShapeMedium
import io.stashapp.android.core.designsystem.theme.ShapeSmall
import io.stashapp.android.core.designsystem.theme.SpaceGrotesk
import io.stashapp.android.core.designsystem.theme.SpineColors
import io.stashapp.android.core.model.PerformerBrowseItem
import io.stashapp.android.core.model.StudioBrowseItem
import io.stashapp.android.core.model.TagBrowseItem

/**
 * Spine-redesigned browse screen.
 *
 * Replaced TopAppBar + single-kind display with a 4-tab segmented control.
 * The initial tab is determined by [BrowseViewModel.ui.kind] (set from nav route).
 * Tabs for Studios/Performers/Tags wire to their respective paging flows.
 * Markers tab shows a placeholder (data source not yet implemented).
 */
@Composable
fun BrowseScreen(
    onBack: () -> Unit,
    onPerformerClick: (id: String) -> Unit,
    onStudioClick: (id: String) -> Unit,
    onTagClick: (id: String) -> Unit,
    viewModel: BrowseViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()

    // Map initial BrowseKind to tab index for segmented control
    var selectedTab by remember {
        mutableIntStateOf(
            when (ui.kind) {
                BrowseKind.Studios -> 0
                BrowseKind.Performers -> 1
                BrowseKind.Tags -> 2
            },
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
    ) { inner ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(top = inner.calculateTopPadding()),
        ) {
            // ---- Spine 4-tab segmented control ----
            SpineSegmentedControl(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it },
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
            )

            // ---- Tab content ----
            when (selectedTab) {
                0 -> {
                    val items = viewModel.studios.collectAsLazyPagingItems()
                    StudiosGrid(items, inner, onStudioClick)
                }
                1 -> {
                    val items = viewModel.performers.collectAsLazyPagingItems()
                    PerformersList(items, inner, onPerformerClick)
                }
                2 -> {
                    val items = viewModel.tags.collectAsLazyPagingItems()
                    TagsGrid(items, inner, onTagClick)
                }
                3 -> {
                    // Markers tab — placeholder, data source not yet implemented
                    Box(
                        Modifier
                            .fillMaxSize()
                            .padding(inner),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "Markers coming soon",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SpineColors.OnSurfaceMuted,
                        )
                    }
                }
            }
        }
    }
}

/** 4-tab segmented control (Studios / Performers / Tags / Markers) */
@Composable
private fun SpineSegmentedControl(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tabs = listOf("Studios", "Performers", "Tags", "Markers")

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .background(SpineColors.Surface, ShapeSmall)
                .border(1.dp, SpineColors.Border, ShapeSmall)
                .padding(3.dp),
    ) {
        tabs.forEachIndexed { index, label ->
            val isSelected = selectedTab == index
            Text(
                text = label,
                style =
                    TextStyle(
                        fontFamily = SpaceGrotesk,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                color = if (isSelected) SpineColors.OnSurface else SpineColors.OnSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier =
                    Modifier
                        .weight(1f)
                        .then(
                            if (isSelected) {
                                Modifier
                                    .background(SpineColors.SurfaceHigh, ShapeSmall)
                                    .border(1.dp, SpineColors.BorderStrong, ShapeSmall)
                            } else {
                                Modifier
                            },
                        ).clickable { onTabSelected(index) }
                        .padding(vertical = 6.dp),
            )
        }
    }
}

/** Spine performers list — full-width rows with 44dp circle avatar */
@Composable
private fun PerformersList(
    items: LazyPagingItems<PerformerBrowseItem>,
    inner: PaddingValues,
    onClick: (String) -> Unit,
) {
    when {
        items.loadState.refresh is androidx.paging.LoadState.Loading && items.itemCount == 0 -> {
            Box(Modifier.fillMaxSize().padding(inner), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        items.loadState.refresh is androidx.paging.LoadState.Error && items.itemCount == 0 -> {
            val err = items.loadState.refresh as androidx.paging.LoadState.Error
            Box(Modifier.fillMaxSize().padding(inner), contentAlignment = Alignment.Center) {
                Text(err.error.message ?: "Failed to load", color = MaterialTheme.colorScheme.error)
            }
        }
        items.itemCount == 0 -> {
            Box(Modifier.fillMaxSize().padding(inner), contentAlignment = Alignment.Center) {
                Text("No results", color = SpineColors.OnSurfaceVariant)
            }
        }
        else -> {
            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding =
                    PaddingValues(
                        bottom = inner.calculateBottomPadding() + 12.dp,
                    ),
            ) {
                items(
                    count = items.itemCount,
                    key = items.itemKey { it.id },
                ) { index ->
                    val performer = items[index] ?: return@items
                    PerformerRow(performer, onClick)
                    HorizontalDivider(color = SpineColors.Border, thickness = 1.dp)
                }
            }
        }
    }
}

@Composable
private fun PerformerRow(
    performer: PerformerBrowseItem,
    onClick: (String) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable { onClick(performer.id) }
                .padding(horizontal = 18.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = performer.imageUrl,
            contentDescription = performer.name,
            contentScale = ContentScale.Crop,
            modifier =
                Modifier
                    .size(44.dp)
                    .clip(CircleShape),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                performer.name,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "${performer.sceneCount} scenes",
                style = MetaMono,
                color = SpineColors.OnSurfaceMuted,
            )
        }
        androidx.compose.material3.Icon(
            Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = SpineColors.OnSurfaceFaint,
            modifier = Modifier.size(16.dp),
        )
    }
}

/** Studios 2-col grid with full-bleed image + gradient overlay */
@Composable
private fun StudiosGrid(
    items: LazyPagingItems<StudioBrowseItem>,
    inner: PaddingValues,
    onClick: (String) -> Unit,
) {
    when {
        items.loadState.refresh is androidx.paging.LoadState.Loading && items.itemCount == 0 -> {
            Box(Modifier.fillMaxSize().padding(inner), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        items.loadState.refresh is androidx.paging.LoadState.Error && items.itemCount == 0 -> {
            val err = items.loadState.refresh as androidx.paging.LoadState.Error
            Box(Modifier.fillMaxSize().padding(inner), contentAlignment = Alignment.Center) {
                Text(err.error.message ?: "Failed to load", color = MaterialTheme.colorScheme.error)
            }
        }
        items.itemCount == 0 -> {
            Box(Modifier.fillMaxSize().padding(inner), contentAlignment = Alignment.Center) {
                Text("No results", color = SpineColors.OnSurfaceVariant)
            }
        }
        else -> {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding =
                    PaddingValues(
                        start = 18.dp,
                        end = 18.dp,
                        bottom = inner.calculateBottomPadding() + 18.dp,
                    ),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(
                    count = items.itemCount,
                    key = items.itemKey { it.id },
                ) { index ->
                    val studio = items[index] ?: return@items
                    StudioCard(studio, onClick)
                }
            }
        }
    }
}

@Composable
private fun StudioCard(
    studio: StudioBrowseItem,
    onClick: (String) -> Unit,
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .aspectRatio(4f / 3f)
                .clip(ShapeMedium)
                .clickable { onClick(studio.id) },
    ) {
        AsyncImage(
            model = studio.imageUrl,
            contentDescription = studio.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        // Bottom gradient overlay
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.4f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.75f),
                    ),
                ),
        )
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .padding(10.dp),
        ) {
            Text(
                studio.name,
                style =
                    TextStyle(
                        fontFamily = SpaceGrotesk,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "${studio.sceneCount} scenes",
                style = MetaMono,
                color = SpineColors.AccentCool,
            )
        }
    }
}

/** Tags grid with AccentCool text */
@Composable
private fun TagsGrid(
    items: LazyPagingItems<TagBrowseItem>,
    inner: PaddingValues,
    onClick: (String) -> Unit,
) {
    when {
        items.loadState.refresh is androidx.paging.LoadState.Loading && items.itemCount == 0 -> {
            Box(Modifier.fillMaxSize().padding(inner), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        items.loadState.refresh is androidx.paging.LoadState.Error && items.itemCount == 0 -> {
            val err = items.loadState.refresh as androidx.paging.LoadState.Error
            Box(Modifier.fillMaxSize().padding(inner), contentAlignment = Alignment.Center) {
                Text(err.error.message ?: "Failed to load", color = MaterialTheme.colorScheme.error)
            }
        }
        items.itemCount == 0 -> {
            Box(Modifier.fillMaxSize().padding(inner), contentAlignment = Alignment.Center) {
                Text("No results", color = SpineColors.OnSurfaceVariant)
            }
        }
        else -> {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 140.dp),
                contentPadding =
                    PaddingValues(
                        start = 12.dp,
                        end = 12.dp,
                        bottom = inner.calculateBottomPadding() + 12.dp,
                    ),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(
                    count = items.itemCount,
                    key = items.itemKey { it.id },
                ) { index ->
                    val tag = items[index] ?: return@items
                    androidx.compose.material3.Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        onClick = { onClick(tag.id) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text(
                                tag.name,
                                style = MaterialTheme.typography.titleSmall,
                                color = SpineColors.AccentCool,
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
        }
    }
}
