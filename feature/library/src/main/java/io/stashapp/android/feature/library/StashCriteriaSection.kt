package io.stashapp.android.feature.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.stashapp.android.core.domain.FilterEntityKind
import io.stashapp.android.core.domain.FilterEntityOption
import io.stashapp.android.core.domain.SceneFilterCriterion
import io.stashapp.android.core.domain.SceneFilterField
import io.stashapp.android.core.domain.SceneFilterInput
import io.stashapp.android.core.domain.SceneFilterModifier
import io.stashapp.android.core.domain.SceneOrientation
import io.stashapp.android.core.domain.SceneResolution

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun StashCriteriaSection(
    criteria: List<SceneFilterCriterion>,
    onChange: (List<SceneFilterCriterion>) -> Unit,
    optionsViewModel: FilterOptionsViewModel = hiltViewModel(),
) {
    var expanded by remember { mutableStateOf(false) }
    val availableFields =
        SceneFilterField.entries.filter { field ->
            field == SceneFilterField.CustomField || criteria.none { it.field == field }
        }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        criteria.forEachIndexed { index, criterion ->
            CriterionEditor(
                criterion = criterion,
                onChange = { changed ->
                    onChange(criteria.toMutableList().apply { this[index] = changed })
                },
                onRemove = { onChange(criteria.toMutableList().apply { removeAt(index) }) },
                optionsViewModel = optionsViewModel,
            )
        }

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
        ) {
            Button(
                onClick = { expanded = true },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true),
            ) {
                Text("Add Stash filter")
            }
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                availableFields.forEach { field ->
                    DropdownMenuItem(
                        text = { Text(field.label) },
                        onClick = {
                            onChange(criteria + SceneFilterCriterion(field))
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CriterionEditor(
    criterion: SceneFilterCriterion,
    onChange: (SceneFilterCriterion) -> Unit,
    onRemove: () -> Unit,
    optionsViewModel: FilterOptionsViewModel,
) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    criterion.field.label,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onRemove) { Text("Remove") }
            }

            if (criterion.field.modifiers.isNotEmpty()) {
                ModifierDropdown(
                    current = criterion.modifier,
                    options = criterion.field.modifiers,
                    onChange = { onChange(criterion.copy(modifier = it)) },
                )
            }

            if (criterion.modifier != SceneFilterModifier.IsNull &&
                criterion.modifier != SceneFilterModifier.NotNull
            ) {
                CriterionValueEditor(criterion, onChange, optionsViewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModifierDropdown(
    current: SceneFilterModifier,
    options: List<SceneFilterModifier>,
    onChange: (SceneFilterModifier) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = current.label,
            onValueChange = {},
            readOnly = true,
            label = { Text("Condition") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label) },
                    onClick = {
                        onChange(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun CriterionValueEditor(
    criterion: SceneFilterCriterion,
    onChange: (SceneFilterCriterion) -> Unit,
    optionsViewModel: FilterOptionsViewModel,
) {
    when (criterion.field.input) {
        SceneFilterInput.Boolean,
        SceneFilterInput.StringBoolean,
        -> BooleanInput(criterion, onChange)
        SceneFilterInput.Resolution -> ResolutionInput(criterion, onChange)
        SceneFilterInput.Orientation -> OrientationInput(criterion, onChange)
        SceneFilterInput.Entity,
        SceneFilterInput.HierarchicalEntity,
        -> EntityInput(criterion, onChange, optionsViewModel)
        SceneFilterInput.MissingProperty -> MissingPropertyInput(criterion, onChange)
        SceneFilterInput.Duplication -> DuplicationInput(criterion, onChange)
        SceneFilterInput.PerceptualHash -> {
            TextInput(criterion, onChange, "Perceptual hash")
            OutlinedTextField(
                value = criterion.value2.orEmpty(),
                onValueChange = { onChange(criterion.copy(value2 = it.filter(Char::isDigit))) },
                label = { Text("Maximum distance") },
                keyboardOptions =
                    androidx.compose.foundation.text
                        .KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        SceneFilterInput.StashIds -> {
            OutlinedTextField(
                value = criterion.auxiliary.orEmpty(),
                onValueChange = { onChange(criterion.copy(auxiliary = it)) },
                label = { Text("Endpoint") },
                modifier = Modifier.fillMaxWidth(),
            )
            TextInput(criterion, onChange, "Stash IDs, comma separated")
        }
        SceneFilterInput.CustomField -> {
            OutlinedTextField(
                value = criterion.auxiliary.orEmpty(),
                onValueChange = { onChange(criterion.copy(auxiliary = it)) },
                label = { Text("Custom field name") },
                modifier = Modifier.fillMaxWidth(),
            )
            TextInput(criterion, onChange, "Values, comma separated")
        }
        else -> TextInput(criterion, onChange, valueLabel(criterion.field.input))
    }
}

@Composable
private fun TextInput(
    criterion: SceneFilterCriterion,
    onChange: (SceneFilterCriterion) -> Unit,
    label: String,
) {
    val numeric = criterion.field.input == SceneFilterInput.Number || criterion.field.input == SceneFilterInput.Duration
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = criterion.value,
            onValueChange = { raw ->
                onChange(criterion.copy(value = if (numeric) raw.filter(Char::isDigit) else raw))
            },
            label = { Text(label) },
            keyboardOptions =
                androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = if (numeric) KeyboardType.Number else KeyboardType.Text,
                ),
            modifier = Modifier.weight(1f),
        )
        if (criterion.modifier == SceneFilterModifier.Between ||
            criterion.modifier == SceneFilterModifier.NotBetween
        ) {
            OutlinedTextField(
                value = criterion.value2.orEmpty(),
                onValueChange = { raw ->
                    onChange(criterion.copy(value2 = if (numeric) raw.filter(Char::isDigit) else raw))
                },
                label = { Text("To") },
                keyboardOptions =
                    androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = if (numeric) KeyboardType.Number else KeyboardType.Text,
                    ),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

private fun valueLabel(input: SceneFilterInput): String =
    when (input) {
        SceneFilterInput.Number -> "Value"
        SceneFilterInput.Duration -> "Seconds"
        SceneFilterInput.Date -> "YYYY-MM-DD"
        SceneFilterInput.Timestamp -> "YYYY-MM-DD HH:MM"
        else -> "Value"
    }

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BooleanInput(
    criterion: SceneFilterCriterion,
    onChange: (SceneFilterCriterion) -> Unit,
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("true" to "Yes", "false" to "No").forEach { (value, label) ->
            FilterChip(
                selected = criterion.value == value,
                onClick = { onChange(criterion.copy(value = value)) },
                label = { Text(label) },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ResolutionInput(
    criterion: SceneFilterCriterion,
    onChange: (SceneFilterCriterion) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = SceneResolution.entries.firstOrNull { it.gqlName == criterion.value }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected?.label.orEmpty(),
            onValueChange = {},
            readOnly = true,
            label = { Text("Resolution") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            SceneResolution.entries.forEach { resolution ->
                DropdownMenuItem(
                    text = { Text(resolution.label) },
                    onClick = {
                        onChange(criterion.copy(value = resolution.gqlName))
                        expanded = false
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OrientationInput(
    criterion: SceneFilterCriterion,
    onChange: (SceneFilterCriterion) -> Unit,
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SceneOrientation.entries.forEach { orientation ->
            val selected = criterion.selected.any { it.id == orientation.gqlName }
            FilterChip(
                selected = selected,
                onClick = {
                    val option = FilterEntityOption(orientation.gqlName, orientation.label)
                    onChange(
                        criterion.copy(
                            selected =
                                if (selected) {
                                    criterion.selected.filterNot { it.id == option.id }
                                } else {
                                    criterion.selected + option
                                },
                        ),
                    )
                },
                label = { Text(orientation.label) },
            )
        }
    }
}

@Composable
private fun EntityInput(
    criterion: SceneFilterCriterion,
    onChange: (SceneFilterCriterion) -> Unit,
    optionsViewModel: FilterOptionsViewModel,
) {
    var open by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            criterion.selected.joinToString { it.label }.ifBlank { "Nothing selected" },
            style = MaterialTheme.typography.bodyMedium,
        )
        Button(onClick = { open = true }) { Text("Choose ${criterion.field.label.lowercase()}") }
        if (criterion.field.input == SceneFilterInput.HierarchicalEntity) {
            OutlinedTextField(
                value = criterion.depth.toString(),
                onValueChange = { raw -> onChange(criterion.copy(depth = raw.filter(Char::isDigit).toIntOrNull() ?: 0)) },
                label = { Text("Child depth (0 = exact)") },
                keyboardOptions =
                    androidx.compose.foundation.text
                        .KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
    if (open) {
        EntityPickerDialog(
            kind = requireNotNull(criterion.field.entityKind),
            selected = criterion.selected,
            onSelected = { onChange(criterion.copy(selected = it)) },
            onDismiss = { open = false },
            viewModel = optionsViewModel,
        )
    }
}

@Composable
private fun EntityPickerDialog(
    kind: FilterEntityKind,
    selected: List<FilterEntityOption>,
    onSelected: (List<FilterEntityOption>) -> Unit,
    onDismiss: () -> Unit,
    viewModel: FilterOptionsViewModel,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(kind) { viewModel.search(kind, "") }
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.large) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Choose ${kind.name.lowercase()}", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = if (state.kind == kind) state.search else "",
                    onValueChange = { viewModel.search(kind, it) },
                    label = { Text("Search") },
                    modifier = Modifier.fillMaxWidth(),
                )
                if (state.loading) Text("Loading…", style = MaterialTheme.typography.bodySmall)
                state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                LazyColumn(Modifier.fillMaxWidth().heightIn(max = 360.dp)) {
                    items(state.options, key = FilterEntityOption::id) { option ->
                        val checked = selected.any { it.id == option.id }
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onSelected(
                                            if (checked) {
                                                selected.filterNot { it.id == option.id }
                                            } else {
                                                selected + option
                                            },
                                        )
                                    }.padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(checked = checked, onCheckedChange = null)
                            Spacer(Modifier.width(8.dp))
                            Text(option.label)
                        }
                        HorizontalDivider()
                    }
                }
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) { Text("Done") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MissingPropertyInput(
    criterion: SceneFilterCriterion,
    onChange: (SceneFilterCriterion) -> Unit,
) {
    val options =
        listOf(
            "title",
            "code",
            "details",
            "director",
            "url",
            "date",
            "rating",
            "cover",
            "galleries",
            "studio",
            "group",
            "performers",
            "tags",
            "stash_id",
        )
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = criterion.value,
            onValueChange = {},
            readOnly = true,
            label = { Text("Property") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.replace('_', ' ').replaceFirstChar(Char::uppercase)) },
                    onClick = {
                        onChange(criterion.copy(value = option))
                        expanded = false
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DuplicationInput(
    criterion: SceneFilterCriterion,
    onChange: (SceneFilterCriterion) -> Unit,
) {
    val options =
        listOf(
            FilterEntityOption("phash", "Perceptual hash"),
            FilterEntityOption("stash_id", "Stash ID"),
            FilterEntityOption("title", "Title"),
            FilterEntityOption("url", "URL"),
        )
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            val yes = criterion.selected.any { it.id == option.id }
            val no = criterion.excluded.any { it.id == option.id }
            FilterChip(
                selected = yes || no,
                onClick = {
                    onChange(
                        when {
                            !yes && !no -> criterion.copy(selected = criterion.selected + option)
                            yes ->
                                criterion.copy(
                                    selected = criterion.selected.filterNot { it.id == option.id },
                                    excluded = criterion.excluded + option,
                                )
                            else -> criterion.copy(excluded = criterion.excluded.filterNot { it.id == option.id })
                        },
                    )
                },
                label = {
                    Text(
                        "${option.label}: ${if (yes) {
                            "yes"
                        } else if (no) {
                            "no"
                        } else {
                            "any"
                        }}",
                    )
                },
            )
        }
    }
}
