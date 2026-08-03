# Spec — Saved filter presets (new)

A named filter+sort the user can re-apply in one tap. Appears in three places:
the `PRESETS` row at the top of the filter sheet, the `Save view` footer button, and the
`SAVED FILTER PRESETS` section in settings.

## Why this is new work
`UiSettings` today persists exactly **one** unnamed filter, `defaultSceneFilter` — the
filter the Library opens with. Presets are a *list* of *named* filters and are a separate
concept. Keep `defaultSceneFilter` as it is; `LibraryViewModel.init` depends on it.

## Domain — `core/domain/.../SceneRepository.kt`
Add next to `SceneFilter`:

```kotlin
data class SavedFilterPreset(
    val id: String,          // UUID string, generated on save
    val name: String,        // user-supplied, trimmed, 1..40 chars
    val filter: SceneFilter,
    val sort: SceneSort = SceneSort.DateDesc,
)
```

## Domain — `core/domain/.../UiSettings.kt`
```kotlin
val savedFilterPresets: Flow<List<SavedFilterPreset>>
suspend fun saveFilterPreset(name: String, filter: SceneFilter, sort: SceneSort): String  // returns id
suspend fun deleteFilterPreset(id: String)
suspend fun renameFilterPreset(id: String, name: String)
```

## Data — `core/data/.../prefs/UiPreferences.kt`
- New key: `private val KEY_PRESETS = stringPreferencesKey("saved_filter_presets")`
- Reuse the existing `StoredSceneFilter` (currently `private` — make it `internal`, and add
  `sort: String? = null`). It already round-trips every `SceneFilter` field and is built to
  survive new fields via `ignoreUnknownKeys = true`; do not write a second mapper.
- Store `@Serializable data class StoredPreset(val id: String, val name: String,
  val filter: StoredSceneFilter, val sort: String? = null)` as a JSON `List`, encoded with
  the same `Json` instance.
- Decode defensively: `runCatching { … }.getOrNull() ?: emptyList()`, matching how
  `defaultSceneFilter` handles corruption today. A bad blob must not crash the Library.
- Cap the list at 20; refuse (or drop-oldest) beyond that.
- `sort` decodes via `runCatching { SceneSort.valueOf(it) }.getOrNull() ?: SceneSort.DateDesc`.

## `feature/library/.../LibraryViewModel.kt`
```kotlin
val presets: StateFlow<List<SavedFilterPreset>> =
    uiPreferences.savedFilterPresets.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

fun savePreset(name: String)      // uses queryFlow.value.filter + .sort
fun applyPreset(id: String)       // updateQuery { it.copy(filter = p.filter, sort = p.sort) }
fun deletePreset(id: String)
```
`uiPreferences` is injected as the `UiSettings` interface — the new methods go on the
interface, so no DI change.

## `feature/settings/.../SettingsViewModel.kt`
Expose `presets` the same way, plus `deletePreset` / `renamePreset`. `SettingsViewModel`
injects the concrete `UiPreferences`, so it can call them directly.

## UI

**Filter sheet — `PRESETS` row.** `SpineChip` per preset, `selected` when the sheet's local
filter+sort equals `preset.filter`/`preset.sort` (structural equality — `SceneFilter` is a
data class, so `==` works). Tapping sets both wholesale. Then a `dashed` `+ save current`
chip. Hide the whole section when the list is empty **and** the current filter is inactive.

**`Save view` footer button.** Opens a small dialog: a text field prefilled with a generated
name, `Cancel` / `Save`. Style it with the same primitives — bordered `Surface`,
`MetaMono`. Saving does not apply or dismiss the sheet.

Generated name: derive from the active facets, e.g. `15–30m · ≥1080p · ★3+`, truncated at
40 chars. Falls back to `Preset N`.

**Settings — `SAVED FILTER PRESETS`.** Chips (never selected) + a `dashed` `manage…` chip.
Tapping a chip opens rename/delete; `manage…` opens a full-list sheet. Show the section with
just `manage…` when there are none, or hide the section entirely — designer's call, ask.

## Edge cases
- Duplicate names are allowed; `id` is the identity.
- Deleting a preset that is currently applied does not change the active filter.
- Presets and `defaultSceneFilter` are independent — saving a preset does not change what
  the Library opens with. If the user wants that, it is a separate "set as default" action;
  not in this scope.
