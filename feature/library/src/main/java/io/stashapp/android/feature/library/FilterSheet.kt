package io.stashapp.android.feature.library

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SheetState
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import io.stashapp.android.core.designsystem.theme.LocalAccentColors
import io.stashapp.android.core.designsystem.theme.SpaceGrotesk
import io.stashapp.android.core.designsystem.theme.SpineColors
import io.stashapp.android.core.domain.DateBucket
import io.stashapp.android.core.domain.SceneDurationBucket
import io.stashapp.android.core.domain.SceneFilter
import io.stashapp.android.core.domain.SceneFilterField
import io.stashapp.android.core.domain.SceneOrientation
import io.stashapp.android.core.domain.SceneResolution
import io.stashapp.android.core.domain.SceneSort
import io.stashapp.android.core.domain.SceneSortDirection
import io.stashapp.android.core.domain.SceneSortField
import java.time.LocalDate

/**
 * Filter + sort bottom sheet.
 *
 * All edits happen against local state and only commit on [onApply].
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FilterSheet(
    sheetState: SheetState,
    initialFilter: SceneFilter,
    initialSort: SceneSort,
    onDismiss: () -> Unit,
    onApply: (SceneFilter, SceneSort) -> Unit,
) {
    val accent = LocalAccentColors.current
    var filter by remember { mutableStateOf(initialFilter) }
    var sort by remember { mutableStateOf(initialSort) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        contentWindowInsets = { WindowInsets.navigationBars },
        containerColor = SpineColors.Bg,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.84f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            SectionTitle("Sort by")
            SortDropdown(sort) { sort = it }
            SortDirectionChips(sort.direction) { direction ->
                sort = sort.copy(direction = direction)
            }

            SectionTitle("Duration")
            DurationSection(
                filter = filter,
                onChange = { filter = it },
            )

            SectionTitle("Release date")
            DateChips(
                activeBucket = currentDateBucket(filter),
                onChange = { bucket ->
                    val (min, max) = datesFor(bucket)
                    filter =
                        filter
                            .withoutCriterion(SceneFilterField.Date)
                            .copy(minDate = min, maxDate = max)
                },
            )

            SectionTitle("Minimum resolution")
            ResolutionChips(filter.minResolution) {
                filter =
                    filter
                        .withoutCriterion(SceneFilterField.Resolution)
                        .copy(minResolution = it)
            }

            SectionTitle("Orientation")
            OrientationChips(filter.orientation) {
                filter =
                    filter
                        .withoutCriterion(SceneFilterField.Orientation)
                        .copy(orientation = it)
            }

            SectionTitle("Rating (★ 0–5)")
            RatingRange(filter.minRating100, filter.maxRating100) { min, max ->
                filter =
                    filter
                        .withoutCriterion(SceneFilterField.Rating)
                        .copy(minRating100 = min, maxRating100 = max)
            }

            SectionTitle("Play count")
            IntMinSlider(
                value = filter.minPlayCount ?: 0,
                maxValue = 50,
                labelForValue = { n ->
                    when (n) {
                        0 -> "Any"
                        else -> "At least $n"
                    }
                },
                onChange = { v ->
                    filter =
                        filter
                            .withoutCriterion(SceneFilterField.PlayCount)
                            .copy(minPlayCount = if (v == 0) null else v)
                },
            )

            SectionTitle("O-counter")
            IntMinSlider(
                value = filter.minOCounter ?: 0,
                maxValue = 20,
                labelForValue = { n ->
                    when (n) {
                        0 -> "Any"
                        else -> "At least $n"
                    }
                },
                onChange = { v ->
                    filter =
                        filter
                            .withoutCriterion(SceneFilterField.OCounter)
                            .copy(minOCounter = if (v == 0) null else v)
                },
            )

            SectionTitle("Flags")
            FlagChips(filter) { filter = it }

            SectionTitle("All Stash filters")
            StashCriteriaSection(
                criteria = filter.criteria,
                onChange = { criteria ->
                    filter =
                        filter
                            .clearQuickFields(criteria.mapTo(mutableSetOf()) { it.field })
                            .copy(criteria = criteria)
                },
            )

            Spacer(Modifier.height(8.dp))

            // Footer — Reset (ghost) + Apply (AccentPrimary filled). No Save view per D-01 Q4.
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = {
                    filter = SceneFilter()
                    sort = SceneSort.DateDesc
                }) { Text("Reset") }
                Spacer(Modifier.weight(1f))
                Button(
                    enabled = filter.criteria.all { it.isValid },
                    onClick = {
                        onApply(filter, sort)
                        onDismiss()
                    },
                    colors =
                        androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = accent.primary,
                            contentColor = accent.onPrimary,
                        ),
                ) {
                    Text("Apply")
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

// ---- Section components ----------------------------------------------------

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style =
            TextStyle(
                fontFamily = SpaceGrotesk,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            ),
        color = MaterialTheme.colorScheme.onSurface,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SortDropdown(
    current: SceneSort,
    onChange: (SceneSort) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        OutlinedTextField(
            value = current.field.label,
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .menuAnchor(type = MenuAnchorType.PrimaryEditable, enabled = true),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            SceneSortField.entries.forEach { field ->
                DropdownMenuItem(
                    text = { Text(field.label) },
                    onClick = {
                        onChange(current.copy(field = field))
                        expanded = false
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SortDirectionChips(
    current: SceneSortDirection,
    onChange: (SceneSortDirection) -> Unit,
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SceneSortDirection.entries.forEach { direction ->
            FilterChip(
                selected = current == direction,
                onClick = { onChange(direction) },
                label = { Text(direction.label) },
            )
        }
    }
}

/**
 * Duration control: preset buckets OR a custom min/max range in minutes.
 *
 * "Custom" is a latched mode — when active, the preset chips become a way to
 * snap back to one of the canonical ranges. When the user edits the min/max
 * fields and the values no longer match any preset, the Custom chip stays
 * selected.
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
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
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            FilterChip(
                selected = activeBucket == null && !customMode && !hasBoundsWithoutPreset,
                onClick = {
                    customMode = false
                    onChange(
                        filter
                            .withoutCriterion(SceneFilterField.Duration)
                            .copy(minDurationSeconds = null, maxDurationSeconds = null),
                    )
                },
                label = { Text("Any") },
            )
            SceneDurationBucket.entries.forEach { bucket ->
                FilterChip(
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
                    label = { Text(bucket.label) },
                )
            }
            FilterChip(
                selected = customMode || hasBoundsWithoutPreset,
                onClick = { customMode = !customMode },
                label = { Text("Custom") },
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DurationCustomRange(
    minSeconds: Int?,
    maxSeconds: Int?,
    onChange: (Int?, Int?) -> Unit,
) {
    // Internal state decoupled from the live filter so typing doesn't thrash
    // the query on every keystroke — we push changes through as numeric
    // values, but hold the raw strings (so "" is a valid "no bound" state).
    var minText by remember(minSeconds) {
        mutableStateOf(minSeconds?.let { (it / 60).toString() }.orEmpty())
    }
    var maxText by remember(maxSeconds) {
        mutableStateOf(maxSeconds?.let { (it / 60).toString() }.orEmpty())
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = minText,
            onValueChange = { raw ->
                // Digits-only, max 4 chars. 9999 minutes = ~166 hours is plenty.
                val clean = raw.filter { it.isDigit() }.take(4)
                minText = clean
                onChange(
                    clean.toIntOrNull()?.takeIf { it > 0 }?.times(60),
                    maxText.toIntOrNull()?.takeIf { it > 0 }?.times(60),
                )
            },
            label = { Text("Min (min)") },
            singleLine = true,
            keyboardOptions =
                androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                ),
            modifier = Modifier.weight(1f),
        )
        Text("–", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = maxText,
            onValueChange = { raw ->
                val clean = raw.filter { it.isDigit() }.take(4)
                maxText = clean
                onChange(
                    minText.toIntOrNull()?.takeIf { it > 0 }?.times(60),
                    clean.toIntOrNull()?.takeIf { it > 0 }?.times(60),
                )
            },
            label = { Text("Max (min)") },
            singleLine = true,
            keyboardOptions =
                androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                ),
            modifier = Modifier.weight(1f),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DateChips(
    activeBucket: DateBucket?,
    onChange: (DateBucket?) -> Unit,
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        FilterChip(selected = activeBucket == null, onClick = { onChange(null) }, label = { Text("Any") })
        DateBucket.entries.forEach { b ->
            FilterChip(
                selected = activeBucket == b,
                onClick = { onChange(if (activeBucket == b) null else b) },
                label = { Text(b.label) },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ResolutionChips(
    current: SceneResolution?,
    onChange: (SceneResolution?) -> Unit,
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        FilterChip(selected = current == null, onClick = { onChange(null) }, label = { Text("Any") })
        SceneResolution.entries.forEach { r ->
            FilterChip(
                selected = current == r,
                onClick = { onChange(if (current == r) null else r) },
                label = { Text(r.label) },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OrientationChips(
    current: SceneOrientation?,
    onChange: (SceneOrientation?) -> Unit,
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        FilterChip(selected = current == null, onClick = { onChange(null) }, label = { Text("Any") })
        SceneOrientation.entries.forEach { o ->
            FilterChip(
                selected = current == o,
                onClick = { onChange(if (current == o) null else o) },
                label = { Text(o.label) },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RatingRange(
    min: Int?,
    max: Int?,
    onChange: (Int?, Int?) -> Unit,
) {
    val lo = (min ?: 0).coerceIn(0, 100).toFloat()
    val hi = (max ?: 100).coerceIn(0, 100).toFloat()
    Column {
        RangeSlider(
            value = lo..hi,
            onValueChange = { range ->
                onChange(
                    range.start.toInt().takeIf { it > 0 },
                    range.endInclusive.toInt().takeIf { it < 100 },
                )
            },
            valueRange = 0f..100f,
            steps = 19,
        )
        Text(
            "${"%.1f".format(lo / 20)} – ${"%.1f".format(hi / 20)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun IntMinSlider(
    value: Int,
    maxValue: Int,
    labelForValue: (Int) -> String,
    onChange: (Int) -> Unit,
) {
    Column {
        Slider(
            value = value.toFloat(),
            onValueChange = { onChange(it.toInt()) },
            valueRange = 0f..maxValue.toFloat(),
            steps = maxValue - 1,
        )
        Text(
            labelForValue(value),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlagChips(
    filter: SceneFilter,
    onChange: (SceneFilter) -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ToggleChip("Organized", filter.organized) {
            onChange(filter.withoutCriterion(SceneFilterField.Organized).copy(organized = it))
        }
        ToggleChip("Has markers", filter.hasMarkers) {
            onChange(filter.withoutCriterion(SceneFilterField.HasMarkers).copy(hasMarkers = it))
        }
        ToggleChip("Interactive", filter.interactive) {
            onChange(filter.withoutCriterion(SceneFilterField.Interactive).copy(interactive = it))
        }
        ToggleChip("In progress", filter.hasResumeTime) {
            onChange(filter.withoutCriterion(SceneFilterField.ResumeTime).copy(hasResumeTime = it))
        }
        ToggleChip("Has captions", filter.hasCaptions) {
            onChange(filter.withoutCriterion(SceneFilterField.Captions).copy(hasCaptions = it))
        }
    }
}

@Composable
private fun ToggleChip(
    label: String,
    state: Boolean?,
    onChange: (Boolean?) -> Unit,
) {
    val accent = LocalAccentColors.current
    val display =
        when (state) {
            true -> "$label: yes"
            false -> "$label: no"
            null -> label
        }
    FilterChip(
        selected = state != null,
        onClick = {
            // Tri-state: null → true → false → null
            val next =
                when (state) {
                    null -> true
                    true -> false
                    false -> null
                }
            onChange(next)
        },
        label = { Text(display) },
        colors =
            FilterChipDefaults.filterChipColors(
                selectedContainerColor = accent.primary.copy(alpha = 0.25f),
            ),
    )
}

// ---- Filter→bucket derivation ----------------------------------------------

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

/**
 * Resolve a [DateBucket] to `YYYY-MM-DD` bounds. Uses ISO local date; no
 * timezone math — Stash's `date` is a plain date string, not a timestamp.
 */
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
