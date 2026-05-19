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
import io.stashapp.android.core.domain.UiSettings
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
class UiPreferences
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : UiSettings {
        private val json =
            Json {
                ignoreUnknownKeys = true
                encodeDefaults = false
            }

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

        override val amoledBlackMode: Flow<Boolean> = flow(KEY_AMOLED, DEFAULT_AMOLED)

        override suspend fun setAmoledBlackMode(value: Boolean) = put(KEY_AMOLED, value)

        override val showRatingOnCards: Flow<Boolean> = flow(KEY_SHOW_RATING, true)

        override suspend fun setShowRatingOnCards(value: Boolean) = put(KEY_SHOW_RATING, value)

        override val showPlayCountOnCards: Flow<Boolean> = flow(KEY_SHOW_PLAY_COUNT, true)

        override suspend fun setShowPlayCountOnCards(value: Boolean) = put(KEY_SHOW_PLAY_COUNT, value)

        override val showResolutionOnCards: Flow<Boolean> = flow(KEY_SHOW_RESOLUTION, true)

        override suspend fun setShowResolutionOnCards(value: Boolean) = put(KEY_SHOW_RESOLUTION, value)

        // ---- App behavior --------------------------------------------------------

        override val activityTracking: Flow<Boolean> = flow(KEY_ACTIVITY_TRACKING, true)

        override suspend fun setActivityTracking(value: Boolean) = put(KEY_ACTIVITY_TRACKING, value)

        override val autoRotatePlayer: Flow<Boolean> = flow(KEY_AUTO_ROTATE, true)

        override suspend fun setAutoRotatePlayer(value: Boolean) = put(KEY_AUTO_ROTATE, value)

        // ---- Accent palette (D-05) -----------------------------------------------

        override val accentPalette: Flow<String> = flow(KEY_ACCENT_PALETTE, DEFAULT_ACCENT_PALETTE)
        override suspend fun setAccentPalette(name: String) = put(KEY_ACCENT_PALETTE, name)

        // ---- Display (SETTINGS-08) -----------------------------------------------

        override val reduceMotion: Flow<Boolean> = flow(KEY_REDUCE_MOTION, DEFAULT_REDUCE_MOTION)
        override suspend fun setReduceMotion(v: Boolean) = put(KEY_REDUCE_MOTION, v)

        override val cardDensity: Flow<String> = flow(KEY_CARD_DENSITY, DEFAULT_CARD_DENSITY)
        override suspend fun setCardDensity(v: String) = put(KEY_CARD_DENSITY, v)

        override val longPressBehavior: Flow<String> = flow(KEY_LONG_PRESS_BEHAVIOR, DEFAULT_LONG_PRESS_BEHAVIOR)
        override suspend fun setLongPressBehavior(v: String) = put(KEY_LONG_PRESS_BEHAVIOR, v)

        override val showResumeBar: Flow<Boolean> = flow(KEY_SHOW_RESUME_BAR, DEFAULT_SHOW_RESUME_BAR)
        override suspend fun setShowResumeBar(v: Boolean) = put(KEY_SHOW_RESUME_BAR, v)

        override val showStudioCaption: Flow<Boolean> = flow(KEY_SHOW_STUDIO_CAPTION, DEFAULT_SHOW_STUDIO_CAPTION)
        override suspend fun setShowStudioCaption(v: Boolean) = put(KEY_SHOW_STUDIO_CAPTION, v)

        override val showChapterStrip: Flow<Boolean> = flow(KEY_SHOW_CHAPTER_STRIP, DEFAULT_SHOW_CHAPTER_STRIP)
        override suspend fun setShowChapterStrip(v: Boolean) = put(KEY_SHOW_CHAPTER_STRIP, v)

        override val tapToPeekInfo: Flow<Boolean> = flow(KEY_TAP_TO_PEEK_INFO, DEFAULT_TAP_TO_PEEK_INFO)
        override suspend fun setTapToPeekInfo(v: Boolean) = put(KEY_TAP_TO_PEEK_INFO, v)

        // ---- Library (SETTINGS-09) -----------------------------------------------

        override val syncRatings: Flow<Boolean> = flow(KEY_SYNC_RATINGS, DEFAULT_SYNC_RATINGS)
        override suspend fun setSyncRatings(v: Boolean) = put(KEY_SYNC_RATINGS, v)

        override val syncOCounter: Flow<Boolean> = flow(KEY_SYNC_O_COUNTER, DEFAULT_SYNC_O_COUNTER)
        override suspend fun setSyncOCounter(v: Boolean) = put(KEY_SYNC_O_COUNTER, v)

        override val syncMarkers: Flow<Boolean> = flow(KEY_SYNC_MARKERS, DEFAULT_SYNC_MARKERS)
        override suspend fun setSyncMarkers(v: Boolean) = put(KEY_SYNC_MARKERS, v)

        override val cacheDuration: Flow<String> = flow(KEY_CACHE_DURATION, DEFAULT_CACHE_DURATION)
        override suspend fun setCacheDuration(v: String) = put(KEY_CACHE_DURATION, v)

        override val keepWatchHistory: Flow<Boolean> = flow(KEY_KEEP_WATCH_HISTORY, DEFAULT_KEEP_WATCH_HISTORY)
        override suspend fun setKeepWatchHistory(v: Boolean) = put(KEY_KEEP_WATCH_HISTORY, v)

        override val historyOnHome: Flow<Boolean> = flow(KEY_HISTORY_ON_HOME, DEFAULT_HISTORY_ON_HOME)
        override suspend fun setHistoryOnHome(v: Boolean) = put(KEY_HISTORY_ON_HOME, v)

        override val smartRails: Flow<Boolean> = flow(KEY_SMART_RAILS, DEFAULT_SMART_RAILS)
        override suspend fun setSmartRails(v: Boolean) = put(KEY_SMART_RAILS, v)

        // ---- Helpers -------------------------------------------------------------

        @Suppress("UNCHECKED_CAST")
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
            private val KEY_AMOLED = booleanPreferencesKey("amoled_black")
            private val KEY_SHOW_RATING = booleanPreferencesKey("card_show_rating")
            private val KEY_SHOW_PLAY_COUNT = booleanPreferencesKey("card_show_play_count")
            private val KEY_SHOW_RESOLUTION = booleanPreferencesKey("card_show_resolution")
            private val KEY_ACTIVITY_TRACKING = booleanPreferencesKey("activity_tracking")
            private val KEY_AUTO_ROTATE = booleanPreferencesKey("auto_rotate_player")

            // Accent palette
            private val KEY_ACCENT_PALETTE = stringPreferencesKey("accent_palette")
            const val DEFAULT_ACCENT_PALETTE = "sage"

            // Display (SETTINGS-08)
            private val KEY_REDUCE_MOTION = booleanPreferencesKey("reduce_motion")
            private val KEY_CARD_DENSITY = stringPreferencesKey("card_density")
            private val KEY_LONG_PRESS_BEHAVIOR = stringPreferencesKey("long_press_behavior")
            private val KEY_SHOW_RESUME_BAR = booleanPreferencesKey("show_resume_bar")
            private val KEY_SHOW_STUDIO_CAPTION = booleanPreferencesKey("show_studio_caption")
            private val KEY_SHOW_CHAPTER_STRIP = booleanPreferencesKey("show_chapter_strip")
            private val KEY_TAP_TO_PEEK_INFO = booleanPreferencesKey("tap_to_peek_info")

            // Library (SETTINGS-09)
            private val KEY_SYNC_RATINGS = booleanPreferencesKey("sync_ratings")
            private val KEY_SYNC_O_COUNTER = booleanPreferencesKey("sync_o_counter")
            private val KEY_SYNC_MARKERS = booleanPreferencesKey("sync_markers")
            private val KEY_CACHE_DURATION = stringPreferencesKey("cache_duration")
            private val KEY_KEEP_WATCH_HISTORY = booleanPreferencesKey("keep_watch_history")
            private val KEY_HISTORY_ON_HOME = booleanPreferencesKey("history_on_home")
            private val KEY_SMART_RAILS = booleanPreferencesKey("smart_rails")

            val DefaultVisible = listOf("home", "scenes", "browse", "settings")
            const val DEFAULT_IMAGE_CACHE_MB = 256
            const val DEFAULT_GRID_COLUMNS = "auto"
            const val DEFAULT_AMOLED = false

            // Display defaults
            const val DEFAULT_REDUCE_MOTION = false
            const val DEFAULT_CARD_DENSITY = "comfortable"
            const val DEFAULT_LONG_PRESS_BEHAVIOR = "quick_menu"
            const val DEFAULT_SHOW_RESUME_BAR = false
            const val DEFAULT_SHOW_STUDIO_CAPTION = false
            const val DEFAULT_SHOW_CHAPTER_STRIP = false
            const val DEFAULT_TAP_TO_PEEK_INFO = false

            // Library defaults
            const val DEFAULT_SYNC_RATINGS = true
            const val DEFAULT_SYNC_O_COUNTER = true
            const val DEFAULT_SYNC_MARKERS = true
            const val DEFAULT_CACHE_DURATION = "1week"
            const val DEFAULT_KEEP_WATCH_HISTORY = true
            const val DEFAULT_HISTORY_ON_HOME = false
            const val DEFAULT_SMART_RAILS = false
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
            )
    }
}
