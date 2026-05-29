package io.stashapp.android.feature.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import io.stashapp.android.core.designsystem.component.resolutionLabel
import io.stashapp.android.core.designsystem.theme.JetBrainsMono
import io.stashapp.android.core.designsystem.theme.MetaMono
import io.stashapp.android.core.designsystem.theme.MonoSmall
import io.stashapp.android.core.designsystem.theme.ShapeMedium
import io.stashapp.android.core.designsystem.theme.ShapeSmall
import io.stashapp.android.core.designsystem.theme.SpaceGrotesk
import io.stashapp.android.core.designsystem.theme.SpineColors
import io.stashapp.android.core.model.Marker
import io.stashapp.android.core.model.PerformerRef
import io.stashapp.android.core.model.SceneDetail
import io.stashapp.android.core.model.TagRef

/**
 * Spine-redesigned scene detail page:
 *  - Card-style hero (16:10, ShapeMedium, 8dp/18dp padding) with back button,
 *    meta pills, and 44dp AccentPrimary play circle
 *  - Title block with studio (AccentPrimary MetaMono), SpaceGrotesk title,
 *    date/rating/playcount row
 *  - Full-width CTA button (AccentPrimary)
 *  - 2-column technical metadata grid (SpineColors.Border hairline via bg)
 *  - Cast rows (Surface ShapeSmall, 36dp avatar)
 *  - Chapter/marker rows (Surface ShapeSmall, 64x36dp thumbnail)
 *  - Tags FlowRow (CircleShape, SpineColors.Border)
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
                    onBack = onBack,
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
    onBack: () -> Unit,
    onPlay: (sceneId: String, startSeconds: Double?) -> Unit,
    onRatingChange: (Int?) -> Unit,
    onOrganizedToggle: (Boolean) -> Unit,
    onIncrementO: () -> Unit,
    onDecrementO: () -> Unit,
) {
    val s = scene.summary

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = contentPadding.calculateTopPadding()),
    ) {
        // ---- Card-style hero ----
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 8.dp)
                    .aspectRatio(16f / 10f)
                    .clip(ShapeMedium)
                    .border(1.dp, SpineColors.Border, ShapeMedium),
        ) {
            AsyncImage(
                model = s.screenshotUrl,
                contentDescription = s.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )

            // Back button — top-left
            Box(
                modifier =
                    Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.55f))
                        .clickable { onBack() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp),
                )
            }

            // Meta pills — bottom-left
            Row(
                modifier =
                    Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                listOfNotNull(
                    s.durationSeconds?.takeIf { it > 0 }?.let { formatDuration(it) },
                    resolutionLabel(s.width, s.height),
                    s.videoCodec?.uppercase(),
                ).forEach { label ->
                    Text(
                        label,
                        style = MetaMono,
                        color = Color.White,
                        modifier =
                            Modifier
                                .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 5.dp, vertical = 2.dp),
                    )
                }
            }

            // Play button — bottom-right (44dp AccentPrimary circle)
            Box(
                modifier =
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(SpineColors.AccentPrimary)
                        .clickable { onPlay(s.id, s.resumeTimeSeconds?.takeIf { it > 2 }) },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.PlayArrow,
                    contentDescription = "Play",
                    tint = SpineColors.AccentOnPrimary,
                    modifier = Modifier.size(24.dp),
                )
            }
        }

        // ---- Title block ----
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
        ) {
            s.studio?.name?.let { studioName ->
                Text(
                    studioName.uppercase(),
                    style = MetaMono.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                    color = SpineColors.AccentPrimary,
                )
                Spacer(Modifier.height(4.dp))
            }
            Text(
                s.displayTitle,
                style =
                    MaterialTheme.typography.headlineMedium.copy(
                        fontSize = 24.sp,
                        letterSpacing = (-0.6).sp,
                        lineHeight = 26.sp,
                    ),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                s.date?.let {
                    Text(it, style = MetaMono, color = SpineColors.OnSurfaceVariant)
                }
                s.rating100?.let { r ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        Icon(Icons.Filled.Star, null, tint = SpineColors.Warning, modifier = Modifier.size(10.dp))
                        Text("%.1f".format(r / 20.0), style = MetaMono, color = SpineColors.OnSurfaceVariant)
                    }
                }
                if (s.playCount > 0) {
                    Text("${s.playCount} plays", style = MetaMono, color = SpineColors.OnSurfaceMuted)
                }
            }
        }

        // ---- Primary CTA ----
        Button(
            onClick = { onPlay(s.id, s.resumeTimeSeconds?.takeIf { it > 2 }) },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp),
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = SpineColors.AccentPrimary,
                    contentColor = SpineColors.AccentOnPrimary,
                ),
            shape = ShapeSmall,
            contentPadding = PaddingValues(12.dp),
        ) {
            Icon(Icons.Filled.PlayArrow, null, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
            val label =
                s.resumeTimeSeconds
                    ?.takeIf { it > 2 }
                    ?.let { "Resume · ${formatDuration(it)}" }
                    ?: "Play"
            Text(
                label,
                style =
                    TextStyle(
                        fontFamily = SpaceGrotesk,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                    ),
            )
        }

        Spacer(Modifier.height(16.dp))

        // ---- Action row (rating, o-counter, organized) ----
        ActionRow(
            rating100 = s.rating100,
            organized = s.organized,
            oCounter = s.oCounter,
            onRatingChange = onRatingChange,
            onOrganizedToggle = onOrganizedToggle,
            onIncrementO = onIncrementO,
            onDecrementO = onDecrementO,
            modifier = Modifier.padding(horizontal = 18.dp),
        )

        Spacer(Modifier.height(24.dp))

        // ---- 2-col technical metadata grid ----
        // SpineColors.Border bg shows through 1dp gaps as hairline dividers (T-05-10: non-lazy, no nested scroll)
        val meta =
            listOfNotNull(
                s.videoCodec?.let { "Codec" to it },
                s.bitrate?.takeIf { it > 0 }?.let { "Bitrate" to "${it / 1000}kbps" },
                resolutionLabel(s.width, s.height)?.let { "Resolution" to it },
                null, // Framerate not in SceneDetail.summary — skip
                null, // Size not in SceneDetail.summary — skip
                s.date?.let { "Added" to it },
            )
        if (meta.isNotEmpty()) {
            MetadataGrid(
                meta = meta,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp),
            )
        }

        Spacer(Modifier.height(24.dp))

        // ---- Cast ----
        if (s.performers.isNotEmpty()) {
            Section(title = "Performers") {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    s.performers.forEach { performer ->
                        PerformerRow(performer)
                    }
                }
            }
        }

        // ---- Tags ----
        if (s.tags.isNotEmpty()) {
            Section(title = "Tags") {
                TagFlow(s.tags)
            }
        }

        // ---- Chapters / Markers ----
        if (scene.markers.isNotEmpty()) {
            Section(title = "Markers") {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    scene.markers.forEach { marker ->
                        ChapterRow(marker) { onPlay(s.id, marker.seconds) }
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

/** Non-lazy 2-column metadata grid. Uses SpineColors.Border background with 1dp gaps for hairline divider effect. */
@Composable
private fun MetadataGrid(
    meta: List<Pair<String, String>>,
    modifier: Modifier = Modifier,
) {
    // Build rows of 2 items each
    val rows = meta.chunked(2)
    Column(
        modifier = modifier.background(SpineColors.Border),
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        rows.forEach { rowItems ->
            Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                rowItems.forEach { (key, value) ->
                    Column(
                        Modifier
                            .weight(1f)
                            .background(SpineColors.Surface)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    ) {
                        Text(key, style = MetaMono, color = SpineColors.OnSurfaceMuted)
                        Spacer(Modifier.height(2.dp))
                        Text(
                            value,
                            style = TextStyle(fontFamily = JetBrainsMono, fontSize = 11.sp),
                            color = SpineColors.OnSurface,
                        )
                    }
                }
                // Pad odd last row
                if (rowItems.size == 1) {
                    Box(Modifier.weight(1f).background(SpineColors.Surface))
                }
            }
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
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // 5-star rating row
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
                val threshold = star * 20
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
                        tint = if (active) SpineColors.Warning else SpineColors.OnSurfaceMuted,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                color = if (organized) SpineColors.AccentPrimary else MaterialTheme.colorScheme.surfaceContainer,
                contentColor = if (organized) SpineColors.AccentOnPrimary else MaterialTheme.colorScheme.onSurface,
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
                        Icon(Icons.Filled.Remove, contentDescription = "Decrement O", modifier = Modifier.size(18.dp))
                    }
                    Text(
                        "● $oCounter",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = if (oCounter > 0) SpineColors.AccentPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                    IconButton(
                        onClick = onIncrementO,
                        modifier = Modifier.size(36.dp),
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Increment O", modifier = Modifier.size(18.dp))
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
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
        )
        content()
    }
    Spacer(Modifier.height(24.dp))
}

/** Spine-styled performer row: 36dp circle avatar, name, chevron */
@Composable
private fun PerformerRow(performer: PerformerRef) {
    Surface(
        color = SpineColors.Surface,
        shape = ShapeSmall,
        border = BorderStroke(1.dp, SpineColors.Border),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                model = performer.imageUrl,
                contentDescription = performer.name,
                contentScale = ContentScale.Crop,
                modifier =
                    Modifier
                        .size(36.dp)
                        .clip(CircleShape),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    performer.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                performer.gender?.let { gender ->
                    Text(
                        gender,
                        style = MetaMono,
                        color = SpineColors.OnSurfaceMuted,
                    )
                }
            }
            Icon(
                Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = SpineColors.OnSurfaceFaint,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

/** Spine-styled chapter/marker row: title + AccentPrimary tag label, timestamp */
@Composable
private fun ChapterRow(
    marker: Marker,
    onClick: () -> Unit,
) {
    Surface(
        color = SpineColors.Surface,
        shape = ShapeSmall,
        border = BorderStroke(1.dp, SpineColors.Border),
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    marker.title.ifBlank { marker.primaryTagName },
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (marker.primaryTagName.isNotBlank() && marker.title.isNotBlank()) {
                    Text(
                        marker.primaryTagName,
                        style = MetaMono,
                        color = SpineColors.AccentPrimary,
                    )
                }
            }
            Text(
                formatDuration(marker.seconds),
                style = MonoSmall,
                color = SpineColors.OnSurfaceMuted,
            )
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
        tags.forEach { tag ->
            Text(
                tag.name,
                style = MaterialTheme.typography.bodySmall,
                color = SpineColors.OnSurface,
                modifier =
                    Modifier
                        .background(SpineColors.Surface, CircleShape)
                        .border(1.dp, SpineColors.Border, CircleShape)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }
    }
}

// Helpers
private fun formatDuration(seconds: Double): String {
    val s = seconds.toInt()
    val h = s / 3600
    val m = (s % 3600) / 60
    val sec = s % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, sec) else "%d:%02d".format(m, sec)
}

private fun Double.format1(): String = "%.1f".format(this)
