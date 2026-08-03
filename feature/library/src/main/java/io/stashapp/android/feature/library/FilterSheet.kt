package io.stashapp.android.feature.library

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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import io.stashapp.android.core.designsystem.component.SectionLabel
import io.stashapp.android.core.designsystem.component.SegmentedRail
import io.stashapp.android.core.designsystem.component.SpineChip
import io.stashapp.android.core.designsystem.component.SpineTriStateChip
import io.stashapp.android.core.designsystem.component.StarRatingPicker
import io.stashapp.android.core.designsystem.component.StepperField
import io.stashapp.android.core.designsystem.theme.JetBrainsMono
import io.stashapp.android.core.designsystem.theme.LocalAccentColors
import io.stashapp.android.core.designsystem.theme.MetaMono
import io.stashapp.android.core.designsystem.theme.MonoSmall
import io.stashapp.android.core.designsystem.theme.ShapeSmall
import io.stashapp.android.core.designsystem.theme.SpineColors
import io.stashapp.android.core.domain.DateBucket
import io.stashapp.android.core.domain.SavedFilterPreset
import io.stashapp.android.core.domain.SceneDurationBucket
import io.stashapp.android.core.domain.SceneFilter
import io.stashapp.android.core.domain.SceneFilterField
import io.stashapp.android.core.domain.SceneOrientation
import io.stashapp.android.core.domain.SceneResolution
import io.stashapp.android.core.domain.SceneSort
import java.time.LocalDate

/** Filter + sort bottom sheet. Edits remain local until [onApply]. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FilterSheet(
    sheetState: SheetState,
    initialFilter: SceneFilter,
    initialSort: SceneSort,
    presets: List<SavedFilterPreset>,
    onDismiss: () -> Unit,
    onApply: (SceneFilter, SceneSort) -> Unit,
    onSavePreset: (String, SceneFilter, SceneSort) -> Unit,
) {
    val accent = LocalAccentColors.current
    var filter by remember(initialFilter) { mutableStateOf(initialFilter) }
    var sort by remember(initialSort) { mutableStateOf(initialSort) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var showStashFilters by remember { mutableStateOf(false) }
    val reset = {
        filter = SceneFilter()
        sort = SceneSort.DateDesc
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        contentWindowInsets = { WindowInsets.navigationBars },
        containerColor = SpineColors.Bg,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        dragHandle = {
            Box(Modifier.padding(top = 8.dp, bottom = 2.dp), contentAlignment = Alignment.Center) {
                Box(
                    Modifier
                        .size(width = 28.dp, height = 3.dp)
                        .background(SpineColors.OnSurfaceFaint, RoundedCornerShape(2.dp)),
                )
            }
        },
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.86f)
                    .border(
                        1.dp,
                        SpineColors.BorderStrong,
                        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                    ),
        ) {
            FilterHeader(activeCount = filter.activeCount, onClear = reset)

            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                FilterBody(
                    filter = filter,
                    sort = sort,
                    presets = presets,
                    showStashFilters = showStashFilters,
                    onFilterChange = { filter = it },
                    onSortChange = { sort = it },
                    onSavePreset = { showSaveDialog = true },
                    onToggleStashFilters = { showStashFilters = !showStashFilters },
                )
            }

            FilterFooter(
                canApply = filter.criteria.all { it.isValid },
                onReset = reset,
                onSave = { showSaveDialog = true },
                onApply = {
                    onApply(filter, sort)
                    onDismiss()
                },
            )
        }
    }

    if (showSaveDialog) {
        SavePresetDialog(
            initialName = generatedPresetName(filter, presets.size + 1),
            onDismiss = { showSaveDialog = false },
            onSave = { name ->
                onSavePreset(name, filter, sort)
                showSaveDialog = false
            },
        )
    }
}

@Composable
private fun FilterBody(
    filter: SceneFilter,
    sort: SceneSort,
    presets: List<SavedFilterPreset>,
    showStashFilters: Boolean,
    onFilterChange: (SceneFilter) -> Unit,
    onSortChange: (SceneSort) -> Unit,
    onSavePreset: () -> Unit,
    onToggleStashFilters: () -> Unit,
) {
    PresetSection(filter, sort, presets, onFilterChange, onSortChange, onSavePreset)
    FilterSection("Sort by") { SortRow(sort = sort, onChange = onSortChange) }
    FilterSection("Duration") { DurationSection(filter = filter, onChange = onFilterChange) }
    DateSection(filter, onFilterChange)
    ResolutionSection(filter, onFilterChange)
    OrientationAndRatingSections(filter, onFilterChange)
    CountSections(filter, onFilterChange)
    FilterSection("Flags — tap to cycle yes / no / any") {
        FlagChips(filter = filter, onChange = onFilterChange)
    }
    StashFiltersSection(filter, showStashFilters, onToggleStashFilters, onFilterChange)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PresetSection(
    filter: SceneFilter,
    sort: SceneSort,
    presets: List<SavedFilterPreset>,
    onFilterChange: (SceneFilter) -> Unit,
    onSortChange: (SceneSort) -> Unit,
    onSavePreset: () -> Unit,
) {
    if (presets.isEmpty() && !filter.isActive) return
    FilterSection("Presets") {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            presets.forEach { preset ->
                SpineChip(
                    label = preset.name,
                    selected = filter == preset.filter && sort == preset.sort,
                    onClick = {
                        onFilterChange(preset.filter)
                        onSortChange(preset.sort)
                    },
                )
            }
            SpineChip("+ save current", false, onSavePreset, dashed = true)
        }
    }
}

@Composable
private fun DateSection(
    filter: SceneFilter,
    onChange: (SceneFilter) -> Unit,
) {
    FilterSection("Release date") {
        val currentYear = LocalDate.now().year
        SegmentedRail(
            options =
                listOf(
                    "any" to null,
                    "week" to DateBucket.LastWeek,
                    "month" to DateBucket.LastMonth,
                    "year" to DateBucket.LastYear,
                    currentYear.toString() to DateBucket.ThisYear,
                ),
            selected = currentDateBucket(filter),
            onSelect = { bucket ->
                val (min, max) = datesFor(bucket)
                onChange(filter.withoutCriterion(SceneFilterField.Date).copy(minDate = min, maxDate = max))
            },
        )
    }
}

@Composable
private fun ResolutionSection(
    filter: SceneFilter,
    onChange: (SceneFilter) -> Unit,
) {
    FilterSection("Min resolution") {
        val baseOptions =
            listOf(
                "any" to null,
                "480" to SceneResolution.Sd480,
                "720" to SceneResolution.Hd720,
                "1080" to SceneResolution.Fhd1080,
                "1440" to SceneResolution.Qhd1440,
                "4K" to SceneResolution.Uhd4k,
                "8K" to SceneResolution.Uhd8k,
            )
        val selected = filter.minResolution
        val options =
            if (selected != null &&
                baseOptions.none { it.second == selected }
            ) {
                baseOptions + (selected.label to selected)
            } else {
                baseOptions
            }
        SegmentedRail(
            options = options,
            selected = selected,
            onSelect = { onChange(filter.withoutCriterion(SceneFilterField.Resolution).copy(minResolution = it)) },
        )
        selected?.let {
            Text(
                text = "${it.label} and above",
                style = MonoSmall,
                color = SpineColors.OnSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

@Composable
private fun OrientationAndRatingSections(
    filter: SceneFilter,
    onChange: (SceneFilter) -> Unit,
) {
    FilterSection("Orientation") {
        SegmentedRail(
            options = listOf("any" to null) + SceneOrientation.entries.map { it.label.lowercase() to it },
            selected = filter.orientation,
            onSelect = { onChange(filter.withoutCriterion(SceneFilterField.Orientation).copy(orientation = it)) },
        )
    }
    FilterSection("Rating — minimum") {
        StarRatingPicker(
            rating100 = filter.minRating100,
            onChange = { onChange(filter.withoutCriterion(SceneFilterField.Rating).copy(minRating100 = it, maxRating100 = null)) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun CountSections(
    filter: SceneFilter,
    onChange: (SceneFilter) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        FilterSection("Play count", Modifier.weight(1f)) {
            StepperField(
                value = filter.minPlayCount,
                max = 50,
                onChange = { onChange(filter.withoutCriterion(SceneFilterField.PlayCount).copy(minPlayCount = it, maxPlayCount = null)) },
            )
        }
        FilterSection("O-counter", Modifier.weight(1f)) {
            StepperField(
                value = filter.minOCounter,
                max = 20,
                onChange = { onChange(filter.withoutCriterion(SceneFilterField.OCounter).copy(minOCounter = it, maxOCounter = null)) },
            )
        }
    }
}

@Composable
private fun StashFiltersSection(
    filter: SceneFilter,
    expanded: Boolean,
    onToggle: () -> Unit,
    onChange: (SceneFilter) -> Unit,
) {
    Column {
        Surface(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle),
            color = SpineColors.Surface,
            shape = ShapeSmall,
            border = BorderStroke(1.dp, SpineColors.Border),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "ALL STASH FILTERS",
                    style = MetaMono.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp),
                    color = SpineColors.OnSurfaceMuted,
                )
                Spacer(Modifier.weight(1f))
                val criteriaCount = filter.criteria.count { it.isValid }
                if (criteriaCount > 0) {
                    Text(criteriaCount.toString(), style = MetaMono, color = SpineColors.OnSurfaceVariant)
                    Spacer(Modifier.width(8.dp))
                }
                Icon(
                    imageVector = Icons.Outlined.KeyboardArrowDown,
                    contentDescription = if (expanded) "Collapse all Stash filters" else "Expand all Stash filters",
                    tint = SpineColors.OnSurfaceMuted,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
        if (expanded) {
            Spacer(Modifier.height(10.dp))
            StashCriteriaSection(
                criteria = filter.criteria,
                onChange = { criteria ->
                    onChange(filter.clearQuickFields(criteria.mapTo(mutableSetOf()) { it.field }).copy(criteria = criteria))
                },
            )
        }
    }
}

@Composable
private fun FilterHeader(
    activeCount: Int,
    onClear: () -> Unit,
) {
    val accent = LocalAccentColors.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, top = 10.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "FILTERS",
            style = MetaMono.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.2.sp),
            color = SpineColors.OnSurface,
        )
        if (activeCount > 0) {
            Spacer(Modifier.width(10.dp))
            Text(
                text = "$activeCount ACTIVE",
                style = MonoSmall.copy(fontWeight = FontWeight.SemiBold),
                color = accent.primary,
                modifier =
                    Modifier
                        .background(accent.primary.copy(alpha = 0.10f), RoundedCornerShape(4.dp))
                        .border(1.dp, accent.primary.copy(alpha = 0.30f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 3.dp),
            )
        }
        Spacer(Modifier.weight(1f))
        Text(
            text = "CLEAR ALL",
            style = MetaMono,
            color = SpineColors.OnSurfaceVariant,
            modifier = Modifier.clickable(onClick = onClear).padding(vertical = 8.dp),
        )
    }
    HorizontalDivider(color = SpineColors.Border)
}

@Composable
private fun FilterFooter(
    canApply: Boolean,
    onReset: () -> Unit,
    onSave: () -> Unit,
    onApply: () -> Unit,
) {
    val accent = LocalAccentColors.current
    HorizontalDivider(color = SpineColors.Border)
    Row(
        modifier = Modifier.fillMaxWidth().background(SpineColors.Bg).padding(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Reset",
            style = MetaMono.copy(fontSize = 11.sp),
            color = SpineColors.OnSurfaceMuted,
            modifier = Modifier.clickable(onClick = onReset).padding(vertical = 11.dp),
        )
        Spacer(Modifier.weight(1f))
        Surface(
            onClick = onSave,
            color = Color.Transparent,
            shape = ShapeSmall,
            border = BorderStroke(1.dp, SpineColors.Border),
        ) {
            Text(
                text = "Save view",
                style = MetaMono.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                color = SpineColors.OnSurface,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
            )
        }
        Spacer(Modifier.width(10.dp))
        Surface(
            onClick = onApply,
            enabled = canApply,
            color = if (canApply) accent.primary else SpineColors.SurfaceHigh,
            contentColor = if (canApply) accent.onPrimary else SpineColors.OnSurfaceMuted,
            shape = ShapeSmall,
        ) {
            Text(
                text = "Apply",
                style =
                    androidx.compose.material3.MaterialTheme.typography.bodySmall
                        .copy(fontWeight = FontWeight.SemiBold),
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 11.dp),
            )
        }
    }
}

@Composable
private fun FilterSection(
    label: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(modifier) {
        SectionLabel(label)
        content()
    }
}

@Composable
private fun SortRow(
    sort: SceneSort,
    onChange: (SceneSort) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val options =
        listOf(
            "Newest first" to SceneSort.DateDesc,
            "Oldest first" to SceneSort.DateAsc,
            "Recently added" to SceneSort.CreatedDesc,
            "Title A–Z" to SceneSort.TitleAsc,
            "Random" to SceneSort.Random,
            "Highest rated" to SceneSort.Rating,
            "Most played" to SceneSort.PlayCount,
            "Recently played" to SceneSort.RecentlyPlayed,
            "Longest" to SceneSort.Duration,
        )
    Box {
        Surface(
            modifier = Modifier.fillMaxWidth().clickable { expanded = true },
            color = SpineColors.Surface,
            shape = ShapeSmall,
            border = BorderStroke(1.dp, SpineColors.Border),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = options.firstOrNull { it.second == sort }?.first ?: sort.label,
                    style = MetaMono.copy(fontSize = 12.sp, fontWeight = FontWeight.Medium),
                    color = SpineColors.OnSurface,
                )
                Spacer(Modifier.weight(1f))
                Icon(
                    imageVector = Icons.Outlined.KeyboardArrowDown,
                    contentDescription = "Choose sort order",
                    tint = SpineColors.OnSurfaceMuted,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = SpineColors.SurfaceHigh,
        ) {
            options.forEach { (label, value) ->
                DropdownMenuItem(
                    text = { Text(label, style = MetaMono.copy(fontSize = 11.sp)) },
                    onClick = {
                        onChange(value)
                        expanded = false
                    },
                )
            }
        }
    }
}

/**
 * Duration control: preset buckets OR a custom min/max range in minutes.
 * Custom mode remains latched while entered bounds do not match a preset.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DurationSection(
    filter: SceneFilter,
    onChange: (SceneFilter) -> Unit,
) {
    val activeBucket = currentDurationBucket(filter)
    val hasBoundsWithoutPreset =
        (filter.minDurationSeconds != null || filter.maxDurationSeconds != null) &&
            activeBucket == null
    var customMode by remember(hasBoundsWithoutPreset) { mutableStateOf(hasBoundsWithoutPreset) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            SpineChip(
                label = "Any",
                selected = activeBucket == null && !customMode && !hasBoundsWithoutPreset,
                onClick = {
                    customMode = false
                    onChange(
                        filter
                            .withoutCriterion(SceneFilterField.Duration)
                            .copy(minDurationSeconds = null, maxDurationSeconds = null),
                    )
                },
            )
            SceneDurationBucket.entries.forEach { bucket ->
                SpineChip(
                    label = bucket.shortLabel,
                    selected = activeBucket == bucket && !customMode,
                    onClick = {
                        customMode = false
                        val targetBucket = if (activeBucket == bucket) null else bucket
                        onChange(
                            filter
                                .withoutCriterion(SceneFilterField.Duration)
                                .copy(
                                    minDurationSeconds = targetBucket?.minSeconds,
                                    maxDurationSeconds = targetBucket?.maxSeconds,
                                ),
                        )
                    },
                )
            }
            SpineChip(
                label = "custom",
                selected = customMode || hasBoundsWithoutPreset,
                onClick = { customMode = !customMode },
            )
        }

        if (customMode) {
            DurationCustomRange(
                minSeconds = filter.minDurationSeconds,
                maxSeconds = filter.maxDurationSeconds,
                onChange = { min, max ->
                    onChange(
                        filter
                            .withoutCriterion(SceneFilterField.Duration)
                            .copy(minDurationSeconds = min, maxDurationSeconds = max),
                    )
                },
            )
        }
    }
}

private val SceneDurationBucket.shortLabel: String
    get() =
        when (this) {
            SceneDurationBucket.UnderFive -> "<5m"
            SceneDurationBucket.OverTwoHours -> "2h+"
            else -> label
        }

@Composable
private fun DurationCustomRange(
    minSeconds: Int?,
    maxSeconds: Int?,
    onChange: (Int?, Int?) -> Unit,
) {
    var minText by remember(minSeconds) {
        mutableStateOf(minSeconds?.let { (it / 60).toString() }.orEmpty())
    }
    var maxText by remember(maxSeconds) {
        mutableStateOf(maxSeconds?.let { (it / 60).toString() }.orEmpty())
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DurationInput(
                label = "MIN · MINUTES",
                value = minText,
                onValueChange = { raw ->
                    val clean = raw.filter { it.isDigit() }.take(4)
                    minText = clean
                    onChange(
                        clean.toIntOrNull()?.takeIf { it > 0 }?.times(60),
                        maxText.toIntOrNull()?.takeIf { it > 0 }?.times(60),
                    )
                },
                modifier = Modifier.weight(1f),
            )
            Text("–", style = MetaMono.copy(fontSize = 12.sp), color = SpineColors.OnSurfaceMuted)
            DurationInput(
                label = "MAX · MINUTES",
                value = maxText,
                onValueChange = { raw ->
                    val clean = raw.filter { it.isDigit() }.take(4)
                    maxText = clean
                    onChange(
                        minText.toIntOrNull()?.takeIf { it > 0 }?.times(60),
                        clean.toIntOrNull()?.takeIf { it > 0 }?.times(60),
                    )
                },
                modifier = Modifier.weight(1f),
            )
        }
        Text(
            text = durationSummary(minText.toIntOrNull(), maxText.toIntOrNull()),
            style = MonoSmall,
            color = SpineColors.OnSurfaceVariant,
        )
    }
}

@Composable
private fun DurationInput(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = LocalAccentColors.current
    var focused by remember { mutableStateOf(false) }
    Column(
        modifier =
            modifier
                .background(SpineColors.Surface, ShapeSmall)
                .border(1.dp, if (focused) accent.primary.copy(alpha = 0.45f) else SpineColors.Border, ShapeSmall)
                .padding(horizontal = 11.dp, vertical = 9.dp),
    ) {
        Text(
            text = label,
            style = MonoSmall.copy(fontSize = 8.5.sp, letterSpacing = 0.8.sp),
            color = SpineColors.OnSurfaceMuted,
        )
        Spacer(Modifier.height(5.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            textStyle =
                TextStyle(
                    fontFamily = JetBrainsMono,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = SpineColors.OnSurface,
                ),
            modifier = Modifier.fillMaxWidth().onFocusChanged { focused = it.isFocused },
            decorationBox = { inner ->
                if (value.isEmpty()) {
                    Text("no limit", style = MetaMono.copy(fontSize = 13.sp), color = SpineColors.OnSurfaceMuted)
                }
                inner()
            },
        )
    }
}

private fun durationSummary(
    min: Int?,
    max: Int?,
): String =
    when {
        min != null && max != null -> "$min–${max}m"
        min != null -> "${min}m and longer"
        max != null -> "up to ${max}m"
        else -> "no duration limit"
    }

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlagChips(
    filter: SceneFilter,
    onChange: (SceneFilter) -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        SpineTriStateChip(
            label = "Organized",
            state = filter.organized,
            onChange = { onChange(filter.withoutCriterion(SceneFilterField.Organized).copy(organized = it)) },
        )
        SpineTriStateChip(
            label = "Has markers",
            state = filter.hasMarkers,
            onChange = { onChange(filter.withoutCriterion(SceneFilterField.HasMarkers).copy(hasMarkers = it)) },
        )
        SpineTriStateChip(
            label = "Interactive",
            state = filter.interactive,
            onChange = { onChange(filter.withoutCriterion(SceneFilterField.Interactive).copy(interactive = it)) },
        )
        SpineTriStateChip(
            label = "In progress",
            state = filter.hasResumeTime,
            onChange = { onChange(filter.withoutCriterion(SceneFilterField.ResumeTime).copy(hasResumeTime = it)) },
        )
        SpineTriStateChip(
            label = "Has captions",
            state = filter.hasCaptions,
            onChange = { onChange(filter.withoutCriterion(SceneFilterField.Captions).copy(hasCaptions = it)) },
        )
    }
}

@Composable
private fun SavePresetDialog(
    initialName: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var name by remember(initialName) { mutableStateOf(initialName) }
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = SpineColors.SurfaceTop,
            shape = ShapeSmall,
            border = BorderStroke(1.dp, SpineColors.BorderStrong),
        ) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionLabel("Save filter preset")
                BasicTextField(
                    value = name,
                    onValueChange = { name = it.take(40) },
                    singleLine = true,
                    textStyle = MetaMono.copy(fontSize = 12.sp, color = SpineColors.OnSurface),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .background(SpineColors.Surface, ShapeSmall)
                            .border(1.dp, SpineColors.Border, ShapeSmall)
                            .padding(horizontal = 12.dp, vertical = 11.dp),
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Text(
                        "Cancel",
                        style = MetaMono.copy(fontSize = 11.sp),
                        color = SpineColors.OnSurfaceVariant,
                        modifier = Modifier.clickable(onClick = onDismiss).padding(10.dp),
                    )
                    val accent = LocalAccentColors.current
                    Surface(
                        onClick = { onSave(name.trim()) },
                        enabled = name.trim().isNotEmpty(),
                        color = accent.primary,
                        contentColor = accent.onPrimary,
                        shape = ShapeSmall,
                    ) {
                        Text(
                            "Save",
                            style = MetaMono.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        )
                    }
                }
            }
        }
    }
}

private fun generatedPresetName(
    filter: SceneFilter,
    number: Int,
): String {
    val parts =
        buildList {
            currentDurationBucket(filter)?.let { add(it.shortLabel) }
            filter.minResolution?.let { add("≥${it.label}") }
            filter.minRating100?.let { add("★${it / 20f}+") }
            filter.minPlayCount?.let { add("≥$it plays") }
        }
    return (parts.takeIf { it.isNotEmpty() }?.joinToString(" · ") ?: "Preset $number").take(40)
}

private fun currentDurationBucket(f: SceneFilter): SceneDurationBucket? =
    SceneDurationBucket.entries.firstOrNull {
        it.minSeconds == f.minDurationSeconds && it.maxSeconds == f.maxDurationSeconds
    }

private fun currentDateBucket(f: SceneFilter): DateBucket? {
    val (min, _) = f.minDate to f.maxDate
    return DateBucket.entries.firstOrNull { b ->
        val (bMin, _) = datesFor(b)
        bMin == min
    }
}

private fun datesFor(bucket: DateBucket?): Pair<String?, String?> {
    val today = LocalDate.now()
    return when (bucket) {
        null -> null to null
        DateBucket.LastWeek -> today.minusWeeks(1).toString() to today.toString()
        DateBucket.LastMonth -> today.minusMonths(1).toString() to today.toString()
        DateBucket.LastYear -> today.minusYears(1).toString() to today.toString()
        DateBucket.ThisYear -> LocalDate.of(today.year, 1, 1).toString() to today.toString()
    }
}

private fun SceneFilter.withoutCriterion(field: SceneFilterField): SceneFilter = copy(criteria = criteria.filterNot { it.field == field })

private fun SceneFilter.clearQuickFields(fields: Set<SceneFilterField>): SceneFilter =
    copy(
        minResolution = minResolution.takeUnless { SceneFilterField.Resolution in fields },
        minRating100 = minRating100.takeUnless { SceneFilterField.Rating in fields },
        maxRating100 = maxRating100.takeUnless { SceneFilterField.Rating in fields },
        organized = organized.takeUnless { SceneFilterField.Organized in fields },
        hasMarkers = hasMarkers.takeUnless { SceneFilterField.HasMarkers in fields },
        interactive = interactive.takeUnless { SceneFilterField.Interactive in fields },
        performerIds = performerIds.takeUnless { SceneFilterField.Performers in fields }.orEmpty(),
        studioIds = studioIds.takeUnless { SceneFilterField.Studios in fields }.orEmpty(),
        tagIds = tagIds.takeUnless { SceneFilterField.Tags in fields }.orEmpty(),
        hasResumeTime = hasResumeTime.takeUnless { SceneFilterField.ResumeTime in fields },
        minDurationSeconds = minDurationSeconds.takeUnless { SceneFilterField.Duration in fields },
        maxDurationSeconds = maxDurationSeconds.takeUnless { SceneFilterField.Duration in fields },
        minDate = minDate.takeUnless { SceneFilterField.Date in fields },
        maxDate = maxDate.takeUnless { SceneFilterField.Date in fields },
        minPlayCount = minPlayCount.takeUnless { SceneFilterField.PlayCount in fields },
        maxPlayCount = maxPlayCount.takeUnless { SceneFilterField.PlayCount in fields },
        minOCounter = minOCounter.takeUnless { SceneFilterField.OCounter in fields },
        maxOCounter = maxOCounter.takeUnless { SceneFilterField.OCounter in fields },
        orientation = orientation.takeUnless { SceneFilterField.Orientation in fields },
        hasCaptions = hasCaptions.takeUnless { SceneFilterField.Captions in fields },
    )
