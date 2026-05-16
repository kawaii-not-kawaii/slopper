package io.stashapp.android.core.domain

import androidx.paging.PagingData
import io.stashapp.android.core.common.AppResult
import io.stashapp.android.core.model.SceneDetail
import io.stashapp.android.core.model.SceneSummary
import kotlinx.coroutines.flow.Flow

data class SceneQuery(
    val searchText: String? = null,
    val sort: SceneSort = SceneSort.DateDesc,
    val filter: SceneFilter = SceneFilter(),
    val shuffleSeed: Int? = null,
)

enum class SceneSort(
    val label: String,
    val gqlSort: String,
    val gqlDir: String,
) {
    DateDesc("Newest first", "date", "DESC"),
    DateAsc("Oldest first", "date", "ASC"),
    CreatedDesc("Recently added", "created_at", "DESC"),
    TitleAsc("Title A→Z", "title", "ASC"),
    Random("Random", "random", "DESC"),
    Rating("Highest rated", "rating", "DESC"),
    PlayCount("Most played", "play_count", "DESC"),
    RecentlyPlayed("Recently played", "last_played_at", "DESC"),
    Duration("Longest first", "duration", "DESC"),
}

/** Single-resolution bucket — maps to Stash's [ResolutionEnum]. */
enum class SceneResolution(
    val label: String,
    val gqlName: String,
) {
    Sd480("480p", "STANDARD"),
    Hd720("720p", "STANDARD_HD"),
    Fhd1080("1080p", "FULL_HD"),
    Qhd1440("1440p", "QUAD_HD"),
    Uhd4k("4K", "FOUR_K"),
    Uhd5k("5K", "FIVE_K"),
    Uhd6k("6K", "SIX_K"),
    Uhd8k("8K", "EIGHT_K"),
}

enum class SceneOrientation(
    val label: String,
    val gqlName: String,
) {
    Landscape("Landscape", "LANDSCAPE"),
    Portrait("Portrait", "PORTRAIT"),
    Square("Square", "SQUARE"),
}

/** Preset duration ranges in seconds — quicker than a slider for common cases. */
enum class SceneDurationBucket(
    val label: String,
    val minSeconds: Int?,
    val maxSeconds: Int?,
) {
    UnderFive("Under 5m", null, 5 * 60),
    FiveToFifteen("5–15m", 5 * 60, 15 * 60),
    FifteenToThirty("15–30m", 15 * 60, 30 * 60),
    ThirtyToHour("30–60m", 30 * 60, 60 * 60),
    OneToTwoHours("1–2h", 60 * 60, 2 * 60 * 60),
    OverTwoHours("Over 2h", 2 * 60 * 60, null),
}

enum class DateBucket(
    val label: String,
) {
    LastWeek("Last week"),
    LastMonth("Last month"),
    LastYear("Last year"),
    ThisYear("This year"),
}

/**
 * User-facing subset of [io.stashapp.android.graphql.type.SceneFilterType].
 *
 * Kept narrow to avoid a hundred knobs in the UI — only fields the filter sheet
 * currently surfaces. Extend carefully, and update [SceneFilterMapper] when
 * adding a field.
 */
data class SceneFilter(
    /** Scenes at or above this resolution tier (GREATER_THAN semantics). */
    val minResolution: SceneResolution? = null,
    /** Inclusive rating range on the 0-100 scale; null means unconstrained. */
    val minRating100: Int? = null,
    val maxRating100: Int? = null,
    val organized: Boolean? = null,
    val hasMarkers: Boolean? = null,
    val interactive: Boolean? = null,
    val performerIds: List<String> = emptyList(),
    val studioIds: List<String> = emptyList(),
    val tagIds: List<String> = emptyList(),
    /** Include only scenes that have been partially watched (resume_time > 0). */
    val hasResumeTime: Boolean? = null,
    /** Duration bounds in seconds. A [SceneDurationBucket] can populate these. */
    val minDurationSeconds: Int? = null,
    val maxDurationSeconds: Int? = null,
    /** Release-date lower bound in `YYYY-MM-DD`. */
    val minDate: String? = null,
    val maxDate: String? = null,
    val minPlayCount: Int? = null,
    val maxPlayCount: Int? = null,
    val minOCounter: Int? = null,
    val maxOCounter: Int? = null,
    val orientation: SceneOrientation? = null,
    val hasCaptions: Boolean? = null,
) {
    val isActive: Boolean
        get() =
            minResolution != null ||
                minRating100 != null ||
                maxRating100 != null ||
                organized != null ||
                hasMarkers != null ||
                interactive != null ||
                performerIds.isNotEmpty() ||
                studioIds.isNotEmpty() ||
                tagIds.isNotEmpty() ||
                hasResumeTime != null ||
                minDurationSeconds != null ||
                maxDurationSeconds != null ||
                minDate != null ||
                maxDate != null ||
                minPlayCount != null ||
                maxPlayCount != null ||
                minOCounter != null ||
                maxOCounter != null ||
                orientation != null ||
                hasCaptions != null

    companion object {
        fun withDurationBucket(bucket: SceneDurationBucket?): SceneFilter =
            SceneFilter(
                minDurationSeconds = bucket?.minSeconds,
                maxDurationSeconds = bucket?.maxSeconds,
            )
    }
}

interface SceneRepository {
    fun pagedScenes(query: SceneQuery): Flow<PagingData<SceneSummary>>

    /** One-shot fetch — useful for home rails where paging is overkill. */
    suspend fun scenes(
        query: SceneQuery,
        limit: Int,
    ): AppResult<List<SceneSummary>>

    suspend fun scene(id: String): AppResult<SceneDetail>

    /** Persist resume position + accumulated play duration on the server. */
    suspend fun saveActivity(
        sceneId: String,
        resumeTimeSeconds: Double,
        playDurationSeconds: Double,
    ): AppResult<Unit>

    /** Record a completed play (increments play_count + appends to play_history). */
    suspend fun addPlay(sceneId: String): AppResult<Unit>

    /** Increment the O-counter. */
    suspend fun incrementO(sceneId: String): AppResult<Int>

    /** Decrement the O-counter (removes the most recent O). */
    suspend fun decrementO(sceneId: String): AppResult<Int>

    /** Set a scene's rating on the 0-100 scale. Null clears the rating. */
    suspend fun setRating(
        sceneId: String,
        rating100: Int?,
    ): AppResult<Unit>

    /** Toggle the organized flag. */
    suspend fun setOrganized(
        sceneId: String,
        organized: Boolean,
    ): AppResult<Unit>
}
