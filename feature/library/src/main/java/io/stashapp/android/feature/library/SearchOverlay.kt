package io.stashapp.android.feature.library

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.stashapp.android.core.designsystem.theme.JetBrainsMono
import io.stashapp.android.core.designsystem.theme.MetaMono
import io.stashapp.android.core.designsystem.theme.MonoSmall
import io.stashapp.android.core.designsystem.theme.ShapeSmall
import io.stashapp.android.core.designsystem.theme.SpaceGrotesk
import io.stashapp.android.core.designsystem.theme.SpineColors
import io.stashapp.android.core.designsystem.theme.LocalAccentColors
import io.stashapp.android.core.model.SceneSummary

/*
 * Spine search overlay — SPINE-12 (D-10).
 *
 * Full-screen overlay with AnimatedVisibility, back arrow, scope chips, and
 * sectioned results list. SearchResults type is defined locally in this file —
 * search data flow is intentionally deferred; callers pass empty results until
 * the search ViewModel integration is wired.
 */

// ---- Local search result types (no new ViewModel methods) -------------------

sealed interface SearchScope {
    data object All : SearchScope

    data object Scenes : SearchScope

    data object Studios : SearchScope

    data object Performers : SearchScope

    data object Tags : SearchScope

    val label: String
        get() =
            when (this) {
                is All -> "All"
                is Scenes -> "Scenes"
                is Studios -> "Studios"
                is Performers -> "Performers"
                is Tags -> "Tags"
            }
}

data class SearchResults(
    val scenes: List<SceneSummary> = emptyList(),
    val performerNames: List<String> = emptyList(),
    val studioNames: List<String> = emptyList(),
    val tagNames: List<String> = emptyList(),
)

// ---- SearchOverlay composable -----------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SearchOverlay(
    query: String,
    onQueryChange: (String) -> Unit,
    results: SearchResults,
    onDismiss: () -> Unit,
) {
    val accent = LocalAccentColors.current
    var scope by remember { mutableStateOf<SearchScope>(SearchScope.All) }

    AnimatedVisibility(
        visible = true,
        enter = fadeIn() + slideInVertically { -it / 4 },
        exit = fadeOut() + slideOutVertically { -it / 4 },
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(SpineColors.Bg),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top bar: back arrow + search field
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Close search",
                            tint = SpineColors.OnSurface,
                        )
                    }

                    OutlinedTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        placeholder = { Text("Search scenes, performers, tags…", style = MetaMono) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = ShapeSmall,
                        textStyle = TextStyle(fontFamily = JetBrainsMono, fontSize = 12.sp),
                        colors =
                            OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = accent.primary,
                                unfocusedBorderColor = SpineColors.Border,
                                focusedContainerColor = SpineColors.Surface,
                                unfocusedContainerColor = SpineColors.Surface,
                                focusedTextColor = SpineColors.OnSurface,
                                unfocusedTextColor = SpineColors.OnSurface,
                                cursorColor = accent.primary,
                            ),
                    )
                }

                // Scope chip row
                val scopes =
                    listOf(
                        SearchScope.All,
                        SearchScope.Scenes,
                        SearchScope.Studios,
                        SearchScope.Performers,
                        SearchScope.Tags,
                    )
                FlowRow(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    scopes.forEach { s ->
                        val isActive = s == scope
                        Box(
                            modifier =
                                Modifier
                                    .height(32.dp)
                                    .clip(ShapeSmall)
                                    .background(
                                        if (isActive) {
                                            accent.primary.copy(alpha = 0.12f)
                                        } else {
                                            SpineColors.Surface
                                        },
                                    ).border(
                                        width = 1.dp,
                                        color =
                                            if (isActive) {
                                                accent.primary.copy(alpha = 0.30f)
                                            } else {
                                                SpineColors.Border
                                            },
                                        shape = ShapeSmall,
                                    ).clickable { scope = s }
                                    .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = s.label,
                                style = MetaMono,
                                color = if (isActive) accent.primary else SpineColors.OnSurfaceVariant,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Results list
                val visibleScenes =
                    when (scope) {
                        is SearchScope.All, is SearchScope.Scenes -> results.scenes
                        else -> emptyList()
                    }

                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    // Scenes section
                    if (visibleScenes.isNotEmpty()) {
                        item {
                            SectionHeader("Scenes · ${visibleScenes.size}")
                        }
                        items(visibleScenes, key = { it.id }) { scene ->
                            SceneResultRow(scene = scene)
                        }
                    }

                    // Performers section
                    val visiblePerformers =
                        when (scope) {
                            is SearchScope.All, is SearchScope.Performers -> results.performerNames
                            else -> emptyList()
                        }
                    if (visiblePerformers.isNotEmpty()) {
                        item {
                            SectionHeader("Performers · ${visiblePerformers.size}")
                        }
                        item {
                            PerformerChipRow(performers = visiblePerformers)
                        }
                    }

                    // Empty state
                    if (query.isNotBlank() && visibleScenes.isEmpty() && visiblePerformers.isEmpty()) {
                        item {
                            Box(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    "No results for \"$query\"",
                                    style = MetaMono,
                                    color = SpineColors.OnSurfaceMuted,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MetaMono,
        color = SpineColors.OnSurfaceMuted,
        modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
    )
}

@Composable
private fun SceneResultRow(scene: SceneSummary) {
    val accent = LocalAccentColors.current
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Thumbnail placeholder: 86×50dp (coil AsyncImage wired by feature:library consumer — stub for v1)
        Box(
            modifier =
                Modifier
                    .size(width = 86.dp, height = 50.dp)
                    .clip(ShapeSmall)
                    .background(SpineColors.SurfaceHigh),
        )

        Spacer(Modifier.width(12.dp))

        Column(Modifier.weight(1f)) {
            Text(
                text = scene.displayTitle,
                style = TextStyle(fontFamily = SpaceGrotesk, fontSize = 13.sp),
                color = SpineColors.OnSurface,
                maxLines = 2,
            )
            scene.studio?.let { studio ->
                Text(
                    text = studio.name,
                    style = MonoSmall,
                    color = accent.primary,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun PerformerChipRow(performers: List<String>) {
    FlowRow(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        performers.forEach { name ->
            Row(
                modifier =
                    Modifier
                        .clip(ShapeSmall)
                        .background(SpineColors.Surface)
                        .border(BorderStroke(1.dp, SpineColors.Border), ShapeSmall)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // 18dp circle placeholder (real performer thumbnail would need URL)
                Box(
                    modifier =
                        Modifier
                            .size(18.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(SpineColors.SurfaceHigh),
                )
                Spacer(Modifier.width(6.dp))
                Text(text = name, style = MonoSmall, color = SpineColors.OnSurface)
            }
        }
    }
}
