package io.stashapp.android.core.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import io.stashapp.android.core.domain.SceneFilter
import io.stashapp.android.core.domain.SceneOrientation
import io.stashapp.android.core.domain.SceneResolution
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * UI-shell preferences: bottom-nav customization, default library filter,
 * and any future chrome-level personalization. Kept separate from
 * [PlayerPreferences] so the two concerns don't grow into each other.
 */
private val Context.uiDataStore by preferencesDataStore(name = "ui_prefs")

@Singleton
class UiPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = false }

    /** Ordered list of nav-item ids shown in the bottom bar. The "More" tab is
     *  always added separately and is never part of this list. */
    val bottomNavVisibleIds: Flow<List<String>> = context.uiDataStore.data.map { prefs ->
        prefs[KEY_NAV_VISIBLE]
            ?.split(",")
            ?.filter { it.isNotBlank() }
            ?.takeIf { it.isNotEmpty() }
            ?: DefaultVisible
    }

    suspend fun setBottomNavVisibleIds(ids: List<String>) {
        context.uiDataStore.edit { it[KEY_NAV_VISIBLE] = ids.joinToString(",") }
    }

    /**
     * The user's default library filter. Applied as the initial [SceneFilter]
     * when the Library tab opens. Stored as JSON so new fields added to
     * [SceneFilter] survive an upgrade without blowing up.
     *
     * Null = no saved default (use empty filter).
     */
    val defaultSceneFilter: Flow<SceneFilter?> = context.uiDataStore.data.map { prefs ->
        prefs[KEY_DEFAULT_FILTER]?.let {
            runCatching { json.decodeFromString<StoredSceneFilter>(it).toFilter() }.getOrNull()
        }
    }

    suspend fun setDefaultSceneFilter(filter: SceneFilter?) {
        context.uiDataStore.edit { prefs ->
            if (filter == null || !filter.isActive) {
                prefs.remove(KEY_DEFAULT_FILTER)
            } else {
                prefs[KEY_DEFAULT_FILTER] = json.encodeToString(StoredSceneFilter.from(filter))
            }
        }
    }

    // ---- Cache ---------------------------------------------------------------

    val imageCacheSizeMb: Flow<Int> = flow(KEY_IMAGE_CACHE_MB, DEFAULT_IMAGE_CACHE_MB)
    suspend fun setImageCacheSizeMb(value: Int) = put(KEY_IMAGE_CACHE_MB, value)

    // ---- Display -------------------------------------------------------------

    /** "auto" / "2" / "3" / "4" */
    val gridColumns: Flow<String> = flow(KEY_GRID_COLUMNS, DEFAULT_GRID_COLUMNS)
    suspend fun setGridColumns(value: String) = put(KEY_GRID_COLUMNS, value)

    val amoledBlackMode: Flow<Boolean> = flow(KEY_AMOLED, DEFAULT_AMOLED)
    suspend fun setAmoledBlackMode(value: Boolean) = put(KEY_AMOLED, value)

    val showRatingOnCards: Flow<Boolean> = flow(KEY_SHOW_RATING, true)
    suspend fun setShowRatingOnCards(value: Boolean) = put(KEY_SHOW_RATING, value)

    val showPlayCountOnCards: Flow<Boolean> = flow(KEY_SHOW_PLAY_COUNT, true)
    suspend fun setShowPlayCountOnCards(value: Boolean) = put(KEY_SHOW_PLAY_COUNT, value)

    val showResolutionOnCards: Flow<Boolean> = flow(KEY_SHOW_RESOLUTION, true)
    suspend fun setShowResolutionOnCards(value: Boolean) = put(KEY_SHOW_RESOLUTION, value)

    // ---- App behavior --------------------------------------------------------

    val activityTracking: Flow<Boolean> = flow(KEY_ACTIVITY_TRACKING, true)
    suspend fun setActivityTracking(value: Boolean) = put(KEY_ACTIVITY_TRACKING, value)

    val autoRotatePlayer: Flow<Boolean> = flow(KEY_AUTO_ROTATE, true)
    suspend fun setAutoRotatePlayer(value: Boolean) = put(KEY_AUTO_ROTATE, value)

    // ---- Helpers -------------------------------------------------------------

    @Suppress("UNCHECKED_CAST")
    private fun <T> flow(key: androidx.datastore.preferences.core.Preferences.Key<T>, default: T): Flow<T> =
        context.uiDataStore.data.map { it[key] ?: default }

    private suspend fun <T> put(key: androidx.datastore.preferences.core.Preferences.Key<T>, value: T) {
        context.uiDataStore.edit { it[key] = value }
    }

    companion object {
        private val KEY_NAV_VISIBLE = stringPreferencesKey("bottom_nav_visible")
        private val KEY_DEFAULT_FILTER = stringPreferencesKey("default_scene_filter")
        private val KEY_IMAGE_CACHE_MB = intPreferencesKey("image_cache_mb")
        private val KEY_GRID_COLUMNS = stringPreferencesKey("grid_columns")
        private val KEY_AMOLED = booleanPreferencesKey("amoled_black")
        private val KEY_SHOW_RATING = booleanPreferencesKey("card_show_rating")
        private val KEY_SHOW_PLAY_COUNT = booleanPreferencesKey("card_show_play_count")
        private val KEY_SHOW_RESOLUTION = booleanPreferencesKey("card_show_resolution")
        private val KEY_ACTIVITY_TRACKING = booleanPreferencesKey("activity_tracking")
        private val KEY_AUTO_ROTATE = booleanPreferencesKey("auto_rotate_player")

        val DefaultVisible = listOf("home", "scenes", "studios", "performers")
        const val DEFAULT_IMAGE_CACHE_MB = 256
        const val DEFAULT_GRID_COLUMNS = "auto"
        const val DEFAULT_AMOLED = false
    }
}

/**
 * Serializable mirror of [SceneFilter] — keeps the domain model free of
 * kotlinx.serialization annotations (the domain lives in `:core:domain`, which
 * shouldn't depend on Kotlinx serialization).
 */
@Serializable
private data class StoredSceneFilter(
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
) {
    fun toFilter() = SceneFilter(
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
    )

    companion object {
        fun from(f: SceneFilter) = StoredSceneFilter(
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
        )
    }
}
