package io.stashapp.android.core.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import io.stashapp.android.core.domain.FilterEntityOption
import io.stashapp.android.core.domain.SavedFilterPreset
import io.stashapp.android.core.domain.SceneFilter
import io.stashapp.android.core.domain.SceneFilterCriterion
import io.stashapp.android.core.domain.SceneFilterField
import io.stashapp.android.core.domain.SceneFilterModifier
import io.stashapp.android.core.domain.SceneOrientation
import io.stashapp.android.core.domain.SceneResolution
import io.stashapp.android.core.domain.SceneSort
import io.stashapp.android.core.domain.SceneSortDirection
import io.stashapp.android.core.domain.SceneSortField
import io.stashapp.android.core.domain.UiSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * UI-shell preferences: bottom-nav customization, default library filter,
 * and any future chrome-level personalization. Kept separate from
 * [PlayerPreferences] so the two concerns don't grow into each other.
 */
private val Context.uiDataStore by preferencesDataStore(name = "ui_prefs")

@Singleton
class UiPreferences
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : UiSettings {
        private val json = StoredPresetCodec.json

        /** Ordered list of nav-item ids shown in the bottom bar. The "More" tab is
         *  always added separately and is never part of this list. */
        override val bottomNavVisibleIds: Flow<List<String>> =
            context.uiDataStore.data.map { prefs ->
                prefs[KEY_NAV_VISIBLE]
                    ?.split(",")
                    ?.filter { it.isNotBlank() }
                    ?.takeIf { it.isNotEmpty() }
                    ?: DefaultVisible
            }

        override suspend fun setBottomNavVisibleIds(ids: List<String>) {
            context.uiDataStore.edit { it[KEY_NAV_VISIBLE] = ids.joinToString(",") }
        }

        /**
         * The user's default library filter. Applied as the initial [SceneFilter]
         * when the Library tab opens. Stored as JSON so new fields added to
         * [SceneFilter] survive an upgrade without blowing up.
         *
         * Null = no saved default (use empty filter).
         */
        override val defaultSceneFilter: Flow<SceneFilter?> =
            context.uiDataStore.data.map { prefs ->
                prefs[KEY_DEFAULT_FILTER]?.let {
                    runCatching { json.decodeFromString<StoredSceneFilter>(it).toFilter() }.getOrNull()
                }
            }

        override suspend fun setDefaultSceneFilter(filter: SceneFilter?) {
            context.uiDataStore.edit { prefs ->
                if (filter == null || !filter.isActive) {
                    prefs.remove(KEY_DEFAULT_FILTER)
                } else {
                    prefs[KEY_DEFAULT_FILTER] = json.encodeToString(StoredSceneFilter.from(filter))
                }
            }
        }

        // ---- Cache ---------------------------------------------------------------

        override val imageCacheSizeMb: Flow<Int> = flow(KEY_IMAGE_CACHE_MB, DEFAULT_IMAGE_CACHE_MB)

        override suspend fun setImageCacheSizeMb(value: Int) = put(KEY_IMAGE_CACHE_MB, value)

        // ---- Display -------------------------------------------------------------

        /** "auto" / "2" / "3" / "4" */
        override val gridColumns: Flow<String> = flow(KEY_GRID_COLUMNS, DEFAULT_GRID_COLUMNS)

        override suspend fun setGridColumns(value: String) = put(KEY_GRID_COLUMNS, value)

        // ---- Saved filter presets -----------------------------------------------

        override val savedFilterPresets: Flow<List<SavedFilterPreset>> =
            context.uiDataStore.data.map { prefs -> decodePresets(prefs[KEY_PRESETS]) }

        override suspend fun saveFilterPreset(
            name: String,
            filter: SceneFilter,
            sort: SceneSort,
        ): String {
            val cleanName = name.trim().also { require(it.isNotEmpty() && it.length <= MAX_PRESET_NAME) }
            val id = UUID.randomUUID().toString()
            context.uiDataStore.edit { prefs ->
                val presets = decodeStoredPresets(prefs[KEY_PRESETS]).toMutableList()
                presets +=
                    StoredPreset(
                        id = id,
                        name = cleanName,
                        filter = StoredSceneFilter.from(filter),
                        sort = sort.toStoredValue(),
                    )
                prefs[KEY_PRESETS] = json.encodeToString(presets.takeLast(MAX_PRESETS))
            }
            return id
        }

        override suspend fun deleteFilterPreset(id: String) {
            context.uiDataStore.edit { prefs ->
                val presets = decodeStoredPresets(prefs[KEY_PRESETS]).filterNot { it.id == id }
                prefs[KEY_PRESETS] = json.encodeToString(presets)
            }
        }

        override suspend fun renameFilterPreset(
            id: String,
            name: String,
        ) {
            val cleanName = name.trim().also { require(it.isNotEmpty() && it.length <= MAX_PRESET_NAME) }
            context.uiDataStore.edit { prefs ->
                val presets =
                    decodeStoredPresets(prefs[KEY_PRESETS]).map { preset ->
                        if (preset.id == id) preset.copy(name = cleanName) else preset
                    }
                prefs[KEY_PRESETS] = json.encodeToString(presets)
            }
        }

        // ---- Search and privacy -------------------------------------------------

        override val recentSearches: Flow<List<String>> =
            context.uiDataStore.data.map { prefs -> decodeRecentSearches(prefs[KEY_RECENT_SEARCHES]) }

        override suspend fun addRecentSearch(query: String) {
            val cleanQuery = query.trim().take(MAX_SEARCH_LENGTH)
            if (cleanQuery.isEmpty()) return
            context.uiDataStore.edit { prefs ->
                val previous = decodeRecentSearches(prefs[KEY_RECENT_SEARCHES])
                val updated = listOf(cleanQuery) + previous.filterNot { it.equals(cleanQuery, ignoreCase = true) }
                prefs[KEY_RECENT_SEARCHES] = json.encodeToString(updated.take(MAX_RECENT_SEARCHES))
            }
        }

        override val blurThumbnails: Flow<Boolean> = flow(KEY_BLUR_THUMBNAILS, true)

        override suspend fun setBlurThumbnails(enabled: Boolean) = put(KEY_BLUR_THUMBNAILS, enabled)

        // ---- Accent palette (D-05) -----------------------------------------------

        override val accentPalette: Flow<String> = flow(KEY_ACCENT_PALETTE, DEFAULT_ACCENT_PALETTE)

        override suspend fun setAccentPalette(name: String) = put(KEY_ACCENT_PALETTE, name)

        // ---- Helpers -------------------------------------------------------------

        private fun <T> flow(
            key: androidx.datastore.preferences.core.Preferences.Key<T>,
            default: T,
        ): Flow<T> = context.uiDataStore.data.map { it[key] ?: default }

        private suspend fun <T> put(
            key: androidx.datastore.preferences.core.Preferences.Key<T>,
            value: T,
        ) {
            context.uiDataStore.edit { it[key] = value }
        }

        companion object {
            private val KEY_NAV_VISIBLE = stringPreferencesKey("bottom_nav_visible")
            private val KEY_DEFAULT_FILTER = stringPreferencesKey("default_scene_filter")
            private val KEY_IMAGE_CACHE_MB = intPreferencesKey("image_cache_mb")
            private val KEY_GRID_COLUMNS = stringPreferencesKey("grid_columns")
            private val KEY_PRESETS = stringPreferencesKey("saved_filter_presets")
            private val KEY_RECENT_SEARCHES = stringPreferencesKey("recent_searches")
            private val KEY_BLUR_THUMBNAILS = booleanPreferencesKey("blur_thumbnails")

            // Accent palette
            private val KEY_ACCENT_PALETTE = stringPreferencesKey("accent_palette")
            const val DEFAULT_ACCENT_PALETTE = "sage"

            val DefaultVisible = listOf("home", "scenes", "browse", "settings")
            const val DEFAULT_IMAGE_CACHE_MB = 256
            const val DEFAULT_GRID_COLUMNS = "auto"
            private const val MAX_PRESETS = 20
            private const val MAX_PRESET_NAME = 40
            private const val MAX_RECENT_SEARCHES = 8
            private const val MAX_SEARCH_LENGTH = 120
        }

        internal fun decodePresets(raw: String?): List<SavedFilterPreset> = StoredPresetCodec.decodeDomain(raw)

        internal fun decodeRecentSearches(raw: String?): List<String> =
            raw
                ?.let { runCatching { json.decodeFromString<List<String>>(it) }.getOrNull() }
                ?.map(String::trim)
                ?.filter(String::isNotEmpty)
                ?.distinctBy(String::lowercase)
                ?.take(MAX_RECENT_SEARCHES)
                ?: emptyList()

        private fun decodeStoredPresets(raw: String?): List<StoredPreset> = StoredPresetCodec.decode(raw)
    }

@Serializable
internal data class StoredFilterEntityOption(
    val id: String,
    val label: String,
) {
    fun toDomain() = FilterEntityOption(id, label)

    companion object {
        fun from(option: FilterEntityOption) = StoredFilterEntityOption(option.id, option.label)
    }
}

@Serializable
internal data class StoredSceneFilterCriterion(
    val field: String,
    val modifier: String,
    val value: String = "",
    val value2: String? = null,
    val selected: List<StoredFilterEntityOption> = emptyList(),
    val excluded: List<StoredFilterEntityOption> = emptyList(),
    val depth: Int = 0,
    val auxiliary: String? = null,
) {
    fun toDomain(): SceneFilterCriterion? {
        val parsedField = runCatching { SceneFilterField.valueOf(field) }.getOrNull() ?: return null
        val parsedModifier =
            runCatching { SceneFilterModifier.valueOf(modifier) }.getOrDefault(parsedField.defaultModifier)
        return SceneFilterCriterion(
            field = parsedField,
            modifier = parsedModifier,
            value = value,
            value2 = value2,
            selected = selected.map(StoredFilterEntityOption::toDomain),
            excluded = excluded.map(StoredFilterEntityOption::toDomain),
            depth = depth,
            auxiliary = auxiliary,
        )
    }

    companion object {
        fun from(criterion: SceneFilterCriterion) =
            StoredSceneFilterCriterion(
                field = criterion.field.name,
                modifier = criterion.modifier.name,
                value = criterion.value,
                value2 = criterion.value2,
                selected = criterion.selected.map(StoredFilterEntityOption::from),
                excluded = criterion.excluded.map(StoredFilterEntityOption::from),
                depth = criterion.depth,
                auxiliary = criterion.auxiliary,
            )
    }
}

/**
 * Serializable mirror of [SceneFilter] — keeps the domain model free of
 * kotlinx.serialization annotations (the domain lives in `:core:domain`, which
 * shouldn't depend on Kotlinx serialization).
 */
@Serializable
internal data class StoredSceneFilter(
    val minResolution: String? = null,
    val minRating100: Int? = null,
    val maxRating100: Int? = null,
    val organized: Boolean? = null,
    val hasMarkers: Boolean? = null,
    val interactive: Boolean? = null,
    val performerIds: List<String> = emptyList(),
    val studioIds: List<String> = emptyList(),
    val tagIds: List<String> = emptyList(),
    val hasResumeTime: Boolean? = null,
    val minDurationSeconds: Int? = null,
    val maxDurationSeconds: Int? = null,
    val minDate: String? = null,
    val maxDate: String? = null,
    val minPlayCount: Int? = null,
    val maxPlayCount: Int? = null,
    val minOCounter: Int? = null,
    val maxOCounter: Int? = null,
    val orientation: String? = null,
    val hasCaptions: Boolean? = null,
    val criteria: List<StoredSceneFilterCriterion> = emptyList(),
    val sort: String? = null,
) {
    fun toFilter() =
        SceneFilter(
            minResolution = minResolution?.let { runCatching { SceneResolution.valueOf(it) }.getOrNull() },
            minRating100 = minRating100,
            maxRating100 = maxRating100,
            organized = organized,
            hasMarkers = hasMarkers,
            interactive = interactive,
            performerIds = performerIds,
            studioIds = studioIds,
            tagIds = tagIds,
            hasResumeTime = hasResumeTime,
            minDurationSeconds = minDurationSeconds,
            maxDurationSeconds = maxDurationSeconds,
            minDate = minDate,
            maxDate = maxDate,
            minPlayCount = minPlayCount,
            maxPlayCount = maxPlayCount,
            minOCounter = minOCounter,
            maxOCounter = maxOCounter,
            orientation = orientation?.let { runCatching { SceneOrientation.valueOf(it) }.getOrNull() },
            hasCaptions = hasCaptions,
            criteria = criteria.mapNotNull(StoredSceneFilterCriterion::toDomain),
        )

    companion object {
        fun from(f: SceneFilter) =
            StoredSceneFilter(
                minResolution = f.minResolution?.name,
                minRating100 = f.minRating100,
                maxRating100 = f.maxRating100,
                organized = f.organized,
                hasMarkers = f.hasMarkers,
                interactive = f.interactive,
                performerIds = f.performerIds,
                studioIds = f.studioIds,
                tagIds = f.tagIds,
                hasResumeTime = f.hasResumeTime,
                minDurationSeconds = f.minDurationSeconds,
                maxDurationSeconds = f.maxDurationSeconds,
                minDate = f.minDate,
                maxDate = f.maxDate,
                minPlayCount = f.minPlayCount,
                maxPlayCount = f.maxPlayCount,
                minOCounter = f.minOCounter,
                maxOCounter = f.maxOCounter,
                orientation = f.orientation?.name,
                hasCaptions = f.hasCaptions,
                criteria = f.criteria.map(StoredSceneFilterCriterion::from),
            )
    }
}

@Serializable
internal data class StoredPreset(
    val id: String,
    val name: String,
    val filter: StoredSceneFilter,
    val sort: String? = null,
) {
    fun toDomain() =
        SavedFilterPreset(
            id = id,
            name = name.trim().take(40),
            filter = filter.toFilter(),
            sort = sort.toSceneSort(),
        )
}

internal object StoredPresetCodec {
    const val MAX_PRESETS = 20

    val json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = false
        }

    fun decode(raw: String?): List<StoredPreset> =
        raw
            ?.let { runCatching { json.decodeFromString<List<StoredPreset>>(it) }.getOrNull() }
            ?.filter { it.id.isNotBlank() && it.name.isNotBlank() }
            ?.takeLast(MAX_PRESETS)
            ?: emptyList()

    fun decodeDomain(raw: String?): List<SavedFilterPreset> = decode(raw).map(StoredPreset::toDomain)
}

private fun SceneSort.toStoredValue(): String = "${field.name}:${direction.name}"

private fun String?.toSceneSort(): SceneSort {
    val (fieldName, directionName) = this?.split(':', limit = 2)?.takeIf { it.size == 2 } ?: return SceneSort.DateDesc
    val field = runCatching { SceneSortField.valueOf(fieldName) }.getOrNull() ?: return SceneSort.DateDesc
    val direction =
        runCatching { SceneSortDirection.valueOf(directionName) }.getOrNull() ?: return SceneSort.DateDesc
    return SceneSort(field, direction)
}
