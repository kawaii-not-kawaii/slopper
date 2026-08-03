package io.stashapp.android.feature.library

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import io.stashapp.android.core.designsystem.component.LocalPrivacyBlurEnabled
import io.stashapp.android.core.designsystem.component.SceneDurationBadge
import io.stashapp.android.core.designsystem.component.SectionLabel
import io.stashapp.android.core.designsystem.component.SegmentedRail
import io.stashapp.android.core.designsystem.component.privacyBlur
import io.stashapp.android.core.designsystem.component.resolutionLabel
import io.stashapp.android.core.designsystem.theme.JetBrainsMono
import io.stashapp.android.core.designsystem.theme.LocalAccentColors
import io.stashapp.android.core.designsystem.theme.MetaMono
import io.stashapp.android.core.designsystem.theme.MonoSmall
import io.stashapp.android.core.designsystem.theme.ShapeSmall
import io.stashapp.android.core.designsystem.theme.SpineColors
import io.stashapp.android.core.domain.FilterEntityKind
import io.stashapp.android.core.domain.FilterEntityOption
import io.stashapp.android.core.model.SceneSummary

enum class SearchScope(
    val abbreviation: String,
) {
    All("all"),
    Scenes("scn"),
    Studios("stu"),
    Performers("prf"),
    Tags("tag"),
}

/**
 * Each list is one fetched page; the matching `*Total` is how many the server actually
 * matched. Counts shown to the user must come from the totals — the lists are capped by
 * the fetch limit, so `scenes.size` would report "20" for a query matching hundreds.
 */
data class SearchResults(
    val scenes: List<SceneSummary> = emptyList(),
    val performers: List<FilterEntityOption> = emptyList(),
    val studios: List<FilterEntityOption> = emptyList(),
    val tags: List<FilterEntityOption> = emptyList(),
    val sceneTotal: Int = scenes.size,
    val performerTotal: Int = performers.size,
    val studioTotal: Int = studios.size,
    val tagTotal: Int = tags.size,
) {
    val totalCount: Int get() = sceneTotal + performerTotal + studioTotal + tagTotal

    fun count(scope: SearchScope): Int =
        when (scope) {
            SearchScope.All -> totalCount
            SearchScope.Scenes -> sceneTotal
            SearchScope.Studios -> studioTotal
            SearchScope.Performers -> performerTotal
            SearchScope.Tags -> tagTotal
        }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SearchOverlay(
    query: String,
    onQueryChange: (String) -> Unit,
    results: SearchResults,
    recents: List<String>,
    loading: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onSceneClick: (SceneSummary) -> Unit,
    onSeeAllScenes: () -> Unit,
    onEntityClick: (FilterEntityKind, FilterEntityOption) -> Unit,
) {
    val accent = LocalAccentColors.current
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    var scope by remember { mutableStateOf(SearchScope.All) }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboard?.show()
    }

    AnimatedVisibility(
        visible = true,
        enter = fadeIn() + slideInVertically { -it / 4 },
        exit = fadeOut() + slideOutVertically { -it / 4 },
    ) {
        Column(Modifier.fillMaxSize().background(SpineColors.Bg)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Close search",
                        tint = SpineColors.OnSurface,
                        modifier = Modifier.size(18.dp),
                    )
                }
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    placeholder = { Text("Search scenes, performers, tags…", style = MetaMono) },
                    leadingIcon = {
                        Icon(
                            Icons.Outlined.Search,
                            contentDescription = null,
                            tint = accent.primary,
                            modifier = Modifier.size(14.dp),
                        )
                    },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { onQueryChange("") }) {
                                Icon(
                                    Icons.Outlined.Close,
                                    contentDescription = "Clear search",
                                    tint = SpineColors.OnSurfaceVariant,
                                    modifier = Modifier.size(12.dp),
                                )
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.weight(1f).focusRequester(focusRequester),
                    shape = ShapeSmall,
                    textStyle = TextStyle(fontFamily = JetBrainsMono, fontSize = 12.sp),
                    colors =
                        OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accent.primary.copy(alpha = 0.45f),
                            unfocusedBorderColor = SpineColors.Border,
                            focusedContainerColor = SpineColors.Surface,
                            unfocusedContainerColor = SpineColors.Surface,
                            focusedTextColor = SpineColors.OnSurface,
                            unfocusedTextColor = SpineColors.OnSurface,
                            cursorColor = accent.primary,
                        ),
                )
            }

            SegmentedRail(
                options = SearchScope.entries.map { "${it.abbreviation} ${results.count(it)}" to it },
                selected = scope,
                onSelect = { scope = it },
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 2.dp),
            )
            Spacer(Modifier.height(12.dp))

            when {
                query.isBlank() -> RecentSearches(recents = recents, onSelect = onQueryChange)
                loading ->
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = accent.primary, modifier = Modifier.size(28.dp))
                    }
                error != null && results.totalCount == 0 -> SearchMessage(error)
                results.count(scope) == 0 -> SearchMessage("No results for \"$query\"")
                else ->
                    SearchResultList(
                        query = query,
                        scope = scope,
                        results = results,
                        onSceneClick = onSceneClick,
                        onSeeAllScenes = onSeeAllScenes,
                        onEntityClick = onEntityClick,
                    )
            }
        }
    }
}

@Composable
private fun SearchResultList(
    query: String,
    scope: SearchScope,
    results: SearchResults,
    onSceneClick: (SceneSummary) -> Unit,
    onSeeAllScenes: () -> Unit,
    onEntityClick: (FilterEntityKind, FilterEntityOption) -> Unit,
) {
    val topResult = if (scope == SearchScope.All) clearTopResult(query, results.scenes) else null
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        topResult?.let { scene ->
            item { SearchSectionHeader("Top result") }
            item { TopResultCard(scene = scene, query = query, onClick = { onSceneClick(scene) }) }
            item { Spacer(Modifier.height(10.dp)) }
        }

        if ((scope == SearchScope.All || scope == SearchScope.Scenes) && results.scenes.isNotEmpty()) {
            val visibleScenes = if (scope == SearchScope.All) results.scenes.take(3) else results.scenes
            item { SearchSectionHeader("Scenes", results.sceneTotal) }
            items(visibleScenes, key = { "scene-${it.id}" }) { scene ->
                SceneResultRow(scene = scene, query = query, onClick = { onSceneClick(scene) })
            }
            if (visibleScenes.size < results.sceneTotal) {
                item { SeeAllScenes(count = results.sceneTotal, onClick = onSeeAllScenes) }
            }
            item { Spacer(Modifier.height(10.dp)) }
        }

        if ((scope == SearchScope.All || scope == SearchScope.Performers) && results.performers.isNotEmpty()) {
            item { SearchSectionHeader("Performers", results.performerTotal) }
            item {
                EntityChipRow(
                    options = results.performers,
                    avatar = true,
                    onClick = { onEntityClick(FilterEntityKind.Performer, it) },
                )
            }
            item { Spacer(Modifier.height(10.dp)) }
        }

        if ((scope == SearchScope.All || scope == SearchScope.Studios) && results.studios.isNotEmpty()) {
            item { SearchSectionHeader("Studios", results.studioTotal) }
            item {
                EntityChipRow(
                    options = results.studios,
                    onClick = { onEntityClick(FilterEntityKind.Studio, it) },
                )
            }
            item { Spacer(Modifier.height(10.dp)) }
        }

        if ((scope == SearchScope.All || scope == SearchScope.Tags) && results.tags.isNotEmpty()) {
            item { SearchSectionHeader("Tags", results.tagTotal) }
            item {
                EntityChipRow(
                    options = results.tags,
                    contentColor = SpineColors.AccentCool,
                    onClick = { onEntityClick(FilterEntityKind.Tag, it) },
                )
            }
        }
    }
}

@Composable
private fun SearchSectionHeader(
    label: String,
    count: Int? = null,
) {
    SectionLabel(label, modifier = Modifier.padding(top = 2.dp)) {
        if (count != null) {
            Spacer(Modifier.width(10.dp))
            Text(count.toString(), style = MetaMono, color = SpineColors.OnSurfaceMuted)
        }
        Spacer(Modifier.width(10.dp))
        HorizontalDivider(modifier = Modifier.weight(1f), color = SpineColors.Border)
    }
}

@Composable
private fun TopResultCard(
    scene: SceneSummary,
    query: String,
    onClick: () -> Unit,
) {
    val accent = LocalAccentColors.current
    val blurThumbnails = LocalPrivacyBlurEnabled.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        color = SpineColors.Surface,
        shape = ShapeSmall,
        border = BorderStroke(1.dp, accent.primary.copy(alpha = 0.30f)),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                model = scene.screenshotUrl,
                contentDescription = scene.displayTitle,
                modifier = Modifier.size(56.dp).clip(ShapeSmall).privacyBlur(blurThumbnails),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            )
            Column(Modifier.weight(1f)) {
                Text(
                    text = highlightedText(scene.displayTitle, query, accent.primary),
                    style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                    color = SpineColors.OnSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = sceneMeta(scene),
                    style = MonoSmall,
                    color = SpineColors.OnSurfaceVariant,
                    maxLines = 1,
                )
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

@Composable
private fun SceneResultRow(
    scene: SceneSummary,
    query: String,
    onClick: () -> Unit,
) {
    val accent = LocalAccentColors.current
    val blurThumbnails = LocalPrivacyBlurEnabled.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        color = SpineColors.Surface,
        shape = ShapeSmall,
        border = BorderStroke(1.dp, SpineColors.Border),
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(width = 86.dp, height = 50.dp).clip(RoundedCornerShape4).background(SpineColors.SurfaceHigh)) {
                AsyncImage(
                    model = scene.screenshotUrl,
                    contentDescription = scene.displayTitle,
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().privacyBlur(blurThumbnails),
                )
                scene.durationSeconds?.takeIf { it > 0 }?.let {
                    SceneDurationBadge(
                        durationSeconds = it,
                        modifier = Modifier.align(Alignment.BottomStart).padding(4.dp),
                    )
                }
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = highlightedText(scene.displayTitle, query, accent.primary),
                    style = androidx.compose.material3.MaterialTheme.typography.titleSmall,
                    color = SpineColors.OnSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = sceneMeta(scene),
                    style = MonoSmall,
                    color = SpineColors.OnSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private val RoundedCornerShape4 =
    androidx.compose.foundation.shape
        .RoundedCornerShape(4.dp)

@Composable
private fun SeeAllScenes(
    count: Int,
    onClick: () -> Unit,
) {
    val accent = LocalAccentColors.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        color = Color.Transparent,
        shape = ShapeSmall,
        border = BorderStroke(1.dp, SpineColors.Border),
    ) {
        Text(
            text = "see all $count scenes →",
            style = MetaMono.copy(fontSize = 11.sp),
            color = accent.primary,
            modifier = Modifier.padding(vertical = 9.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EntityChipRow(
    options: List<FilterEntityOption>,
    avatar: Boolean = false,
    contentColor: Color = SpineColors.OnSurface,
    onClick: (FilterEntityOption) -> Unit,
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        options.forEach { option ->
            Surface(
                onClick = { onClick(option) },
                color = SpineColors.Surface,
                shape = ShapeSmall,
                border = BorderStroke(1.dp, SpineColors.Border),
            ) {
                Row(
                    modifier = Modifier.padding(start = if (avatar) 6.dp else 10.dp, end = 10.dp, top = 6.dp, bottom = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (avatar) Box(Modifier.size(18.dp).background(SpineColors.SurfaceHigh, CircleShape))
                    Text(option.label, style = MetaMono.copy(fontSize = 10.5.sp), color = contentColor)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RecentSearches(
    recents: List<String>,
    onSelect: (String) -> Unit,
) {
    if (recents.isEmpty()) return
    Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp)) {
        SearchSectionHeader("Recent")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            recents.forEach { recent ->
                Surface(
                    onClick = { onSelect(recent) },
                    color = Color.Transparent,
                    shape = ShapeSmall,
                    border = BorderStroke(1.dp, SpineColors.Border),
                ) {
                    Text(
                        recent,
                        style = MetaMono.copy(fontSize = 11.sp),
                        color = SpineColors.OnSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchMessage(message: String) {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(message, style = MetaMono, color = SpineColors.OnSurfaceMuted)
    }
}

internal fun clearTopResult(
    query: String,
    scenes: List<SceneSummary>,
): SceneSummary? {
    val normalized = query.trim()
    if (normalized.isEmpty()) return null
    val exact = scenes.filter { it.displayTitle.equals(normalized, ignoreCase = true) }
    if (exact.size == 1) return exact.single()
    val prefix = scenes.filter { it.displayTitle.startsWith(normalized, ignoreCase = true) }
    return prefix.singleOrNull()
}

internal fun highlightedText(
    text: String,
    query: String,
    accent: Color,
): AnnotatedString {
    val needle = query.trim()
    if (needle.isEmpty()) return AnnotatedString(text)
    return buildAnnotatedString {
        var start = 0
        while (start < text.length) {
            val match = text.indexOf(needle, startIndex = start, ignoreCase = true)
            if (match < 0) {
                append(text.substring(start))
                break
            }
            append(text.substring(start, match))
            withStyle(SpanStyle(background = accent.copy(alpha = 0.25f), color = accent)) {
                append(text.substring(match, match + needle.length))
            }
            start = match + needle.length
        }
    }
}

private fun sceneMeta(scene: SceneSummary): String =
    listOfNotNull(
        scene.studio?.name,
        resolutionLabel(scene.width, scene.height),
        scene.rating100?.let { "★%.1f".format(it / 20f) },
    ).joinToString(" · ")
