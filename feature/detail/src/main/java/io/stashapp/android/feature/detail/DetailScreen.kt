package io.stashapp.android.feature.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import io.stashapp.android.core.designsystem.component.resolutionLabel
import io.stashapp.android.core.designsystem.theme.LocalStashColors
import io.stashapp.android.core.designsystem.theme.StashColors
import io.stashapp.android.core.model.Marker
import io.stashapp.android.core.model.PerformerRef
import io.stashapp.android.core.model.SceneDetail
import io.stashapp.android.core.model.TagRef

/**
 * Plex-style scene detail page:
 *  - Hero backdrop (screenshot) with gradient scrim
 *  - Title, studio, date, codec/resolution row
 *  - Primary action: play
 *  - Performers row (avatar chips)
 *  - Tags (flow)
 *  - Markers (vertical list, tap to play from timestamp)
 *  - Details text block
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DetailScreen(
    onBack: () -> Unit,
    onPlay: (sceneId: String, startSeconds: Double?) -> Unit,
    viewModel: DetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                    ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { inner ->
        when {
            state.loading ->
                Box(
                    Modifier.fillMaxSize().padding(inner),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator() }

            state.error != null ->
                Box(
                    Modifier.fillMaxSize().padding(inner).padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "Can't load scene: ${state.error}",
                        color = MaterialTheme.colorScheme.error,
                    )
                }

            state.scene != null ->
                SceneBody(
                    scene = state.scene!!,
                    contentPadding = inner,
                    onPlay = onPlay,
                    onRatingChange = viewModel::setRating,
                    onOrganizedToggle = viewModel::setOrganized,
                    onIncrementO = viewModel::incrementO,
                    onDecrementO = viewModel::decrementO,
                )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SceneBody(
    scene: SceneDetail,
    contentPadding: PaddingValues,
    onPlay: (sceneId: String, startSeconds: Double?) -> Unit,
    onRatingChange: (Int?) -> Unit,
    onOrganizedToggle: (Boolean) -> Unit,
    onIncrementO: () -> Unit,
    onDecrementO: () -> Unit,
) {
    val extra = LocalStashColors.current
    val s = scene.summary

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        // Hero — screenshot with title overlay
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f),
        ) {
            AsyncImage(
                model = s.screenshotUrl,
                contentDescription = s.title,
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            // Bottom fade-to-background so the title reads & blends into the page
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.3f to Color.Transparent,
                            1f to MaterialTheme.colorScheme.background,
                        ),
                    ),
            )
            // Giant play button centered on hero
            Surface(
                color = StashColors.AccentPrimary.copy(alpha = 0.92f),
                contentColor = StashColors.AccentOnPrimary,
                shape = CircleShape,
                modifier =
                    Modifier
                        .align(Alignment.Center)
                        .size(72.dp),
                onClick = { onPlay(s.id, s.resumeTimeSeconds?.takeIf { it > 2 }) },
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Filled.PlayArrow,
                        contentDescription = "Play",
                        modifier = Modifier.size(40.dp),
                    )
                }
            }
        }

        Column(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Title + studio / date
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = s.displayTitle,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    s.studio?.name?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.titleSmall,
                            color = StashColors.AccentSecondary,
                        )
                    }
                    s.date?.let {
                        Dot()
                        Text(
                            it,
                            style = MaterialTheme.typography.bodySmall,
                            color = extra.onSurfaceMuted,
                        )
                    }
                    s.rating100?.let { r ->
                        Dot()
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.Star,
                                contentDescription = null,
                                tint = StashColors.Warning,
                                modifier = Modifier.size(14.dp),
                            )
                            Text(
                                " %.1f".format(r / 20.0),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }

            // Tech + stats pills
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                s.durationSeconds?.takeIf { it > 0 }?.let { Pill(formatDuration(it)) }
                resolutionLabel(s.width, s.height)?.let { Pill(it) }
                s.videoCodec?.let { Pill(it.uppercase()) }
                s.audioCodec?.let { Pill(it.uppercase()) }
                s.bitrate?.takeIf { it > 0 }?.let { Pill("${(it / 1_000_000.0).format1()} Mbps") }
                if (s.playCount > 0) Pill("▶ ${s.playCount}", accent = true)
                if (s.oCounter > 0) Pill("● ${s.oCounter}", accent = true)
                if (s.interactive) Pill("Interactive", accent = true)
            }

            // Primary play button (secondary to the hero one; useful after scroll)
            Button(
                onClick = { onPlay(s.id, s.resumeTimeSeconds?.takeIf { it > 2 }) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text(
                    s.resumeTimeSeconds
                        ?.takeIf { it > 2 }
                        ?.let { "Resume from ${formatDuration(it)}" }
                        ?: "Play",
                )
            }

            // Action row — rating, o-counter, organize
            ActionRow(
                rating100 = s.rating100,
                organized = s.organized,
                oCounter = s.oCounter,
                onRatingChange = onRatingChange,
                onOrganizedToggle = onOrganizedToggle,
                onIncrementO = onIncrementO,
                onDecrementO = onDecrementO,
            )

            if (s.performers.isNotEmpty()) {
                Section(title = "Performers") {
                    PerformerRow(s.performers)
                }
            }

            if (s.tags.isNotEmpty()) {
                Section(title = "Tags") {
                    TagFlow(s.tags)
                }
            }

            if (scene.markers.isNotEmpty()) {
                Section(title = "Markers") {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        scene.markers.forEach { marker ->
                            MarkerRow(marker) { onPlay(s.id, marker.seconds) }
                        }
                    }
                }
            }

            s.details?.takeIf { it.isNotBlank() }?.let { details ->
                Section(title = "Details") {
                    Text(
                        details,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(contentPadding.calculateBottomPadding() + 24.dp))
        }
    }
}

@Composable
private fun ActionRow(
    rating100: Int?,
    organized: Boolean,
    oCounter: Int,
    onRatingChange: (Int?) -> Unit,
    onOrganizedToggle: (Boolean) -> Unit,
    onIncrementO: () -> Unit,
    onDecrementO: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // 5-star rating row — each star is tappable, clicking an already-active
        // star clears (sets to null).
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                "Rating",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.size(8.dp))
            (1..5).forEach { star ->
                val threshold = star * 20 // 20/40/60/80/100
                val active = (rating100 ?: 0) >= threshold
                IconButton(
                    onClick = {
                        val current = rating100 ?: 0
                        onRatingChange(
                            if (current == threshold) {
                                (threshold - 20).takeIf { it > 0 }
                            } else {
                                threshold
                            },
                        )
                    },
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        if (active) Icons.Filled.Star else Icons.Filled.StarBorder,
                        contentDescription = "$star stars",
                        tint = if (active) StashColors.Warning else LocalStashColors.current.onSurfaceMuted,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        }

        // Organized + O-counter + reset rating, all in a wrap-friendly row
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                color = if (organized) StashColors.AccentPrimary else MaterialTheme.colorScheme.surfaceContainer,
                contentColor = if (organized) StashColors.AccentOnPrimary else MaterialTheme.colorScheme.onSurface,
                shape = RoundedCornerShape(8.dp),
                onClick = { onOrganizedToggle(!organized) },
            ) {
                Row(
                    Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        if (organized) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.size(6.dp))
                    Text(
                        if (organized) "Organized" else "Mark organized",
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            // O-counter stepper
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = RoundedCornerShape(8.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onDecrementO,
                        enabled = oCounter > 0,
                        modifier = Modifier.size(36.dp),
                    ) {
                        Icon(
                            Icons.Filled.Remove,
                            contentDescription = "Decrement O",
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Text(
                        "● $oCounter",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = if (oCounter > 0) StashColors.AccentPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                    IconButton(
                        onClick = onIncrementO,
                        modifier = Modifier.size(36.dp),
                    ) {
                        Icon(
                            Icons.Filled.Add,
                            contentDescription = "Increment O",
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Section(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
        )
        content()
    }
}

@Composable
private fun Dot() {
    Text("·", color = LocalStashColors.current.onSurfaceMuted)
}

@Composable
private fun Pill(
    text: String,
    accent: Boolean = false,
) {
    Surface(
        color = if (accent) StashColors.AccentPrimary else MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = if (accent) StashColors.AccentOnPrimary else MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(6.dp),
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun PerformerRow(performers: List<PerformerRef>) {
    androidx.compose.foundation.lazy.LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(
            count = performers.size,
            key = { performers[it].id },
        ) { i ->
            val p = performers[i]
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.size(width = 80.dp, height = 110.dp),
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.size(72.dp),
                ) {
                    AsyncImage(
                        model = p.imageUrl,
                        contentDescription = p.name,
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    p.name,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagFlow(tags: List<TagRef>) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        tags.forEach { t ->
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                contentColor = StashColors.AccentSecondary,
                shape = RoundedCornerShape(50),
            ) {
                Text(
                    t.name,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun MarkerRow(
    marker: Marker,
    onClick: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(8.dp),
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Filled.Bookmark,
                contentDescription = null,
                tint = StashColors.AccentPrimary,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.size(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    marker.title.ifBlank { marker.primaryTagName },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (marker.title.isNotBlank() && marker.primaryTagName.isNotBlank()) {
                    Text(
                        marker.primaryTagName,
                        style = MaterialTheme.typography.labelSmall,
                        color = LocalStashColors.current.onSurfaceMuted,
                    )
                }
            }
            Text(
                formatDuration(marker.seconds),
                style = MaterialTheme.typography.labelMedium,
                color = LocalStashColors.current.onSurfaceMuted,
            )
        }
    }
}

// Helpers — local copies to avoid making designsystem expose these broadly
private fun formatDuration(seconds: Double): String {
    val s = seconds.toInt()
    val h = s / 3600
    val m = (s % 3600) / 60
    val sec = s % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, sec) else "%d:%02d".format(m, sec)
}

private fun Double.format1(): String = "%.1f".format(this)
