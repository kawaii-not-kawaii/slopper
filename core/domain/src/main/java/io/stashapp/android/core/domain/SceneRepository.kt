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

enum class SceneSortDirection(
    val label: String,
    val gqlName: String,
) {
    Ascending("Ascending", "ASC"),
    Descending("Descending", "DESC"),
}

enum class SceneSortField(
    val label: String,
    val gqlName: String,
) {
    Organized("Organized", "organized"),
    Date("Date", "date"),
    FileCount("File count", "file_count"),
    FileSize("File size", "filesize"),
    Duration("Duration", "duration"),
    FrameRate("Frame rate", "framerate"),
    Resolution("Resolution", "resolution"),
    BitRate("Bit rate", "bitrate"),
    LastPlayed("Last played", "last_played_at"),
    ResumeTime("Resume time", "resume_time"),
    PlayDuration("Play duration", "play_duration"),
    PlayCount("Play count", "play_count"),
    Interactive("Interactive", "interactive"),
    InteractiveSpeed("Interactive speed", "interactive_speed"),
    PerceptualSimilarity("Perceptual similarity", "perceptual_similarity"),
    PerformerAge("Performer age", "performer_age"),
    Studio("Studio", "studio"),
    Title("Title", "title"),
    Path("Path", "path"),
    Rating("Rating", "rating"),
    FileModified("File modified", "file_mod_time"),
    TagCount("Tag count", "tag_count"),
    PerformerCount("Performer count", "performer_count"),
    Random("Random", "random"),
    OCounter("O-counter", "o_counter"),
    LastO("Last O", "last_o_at"),
    GroupSceneNumber("Group scene number", "group_scene_number"),
    Code("Scene code", "code"),
    Created("Created", "created_at"),
    Updated("Updated", "updated_at"),
}

data class SceneSort(
    val field: SceneSortField = SceneSortField.Date,
    val direction: SceneSortDirection = SceneSortDirection.Descending,
) {
    val label: String get() = this.field.label
    val gqlSort: String get() = this.field.gqlName
    val gqlDir: String get() = direction.gqlName

    companion object {
        val DateDesc = SceneSort()
        val DateAsc = SceneSort(direction = SceneSortDirection.Ascending)
        val CreatedDesc = SceneSort(SceneSortField.Created)
        val TitleAsc = SceneSort(SceneSortField.Title, SceneSortDirection.Ascending)
        val Random = SceneSort(SceneSortField.Random)
        val Rating = SceneSort(SceneSortField.Rating)
        val PlayCount = SceneSort(SceneSortField.PlayCount)
        val RecentlyPlayed = SceneSort(SceneSortField.LastPlayed)
        val Duration = SceneSort(SceneSortField.Duration)
    }
}

/** Single-resolution bucket — maps to Stash's [ResolutionEnum]. */
enum class SceneResolution(
    val label: String,
    val gqlName: String,
) {
    P144("144p", "VERY_LOW"),
    P240("240p", "LOW"),
    P360("360p", "R360P"),
    Sd480("480p", "STANDARD"),
    P540("540p", "WEB_HD"),
    Hd720("720p", "STANDARD_HD"),
    Fhd1080("1080p", "FULL_HD"),
    Qhd1440("1440p", "QUAD_HD"),
    Vr1920("1920p", "VR_HD"),
    Uhd4k("4K", "FOUR_K"),
    Uhd5k("5K", "FIVE_K"),
    Uhd6k("6K", "SIX_K"),
    Uhd7k("7K", "SEVEN_K"),
    Uhd8k("8K", "EIGHT_K"),
    Huge("8K+", "HUGE"),
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

enum class SceneFilterModifier(
    val label: String,
) {
    Equals("Equals"),
    NotEquals("Does not equal"),
    GreaterThan("Greater than"),
    LessThan("Less than"),
    IsNull("Is empty"),
    NotNull("Is not empty"),
    IncludesAll("Includes all"),
    Includes("Includes"),
    Excludes("Excludes"),
    MatchesRegex("Matches regex"),
    NotMatchesRegex("Does not match regex"),
    Between("Between"),
    NotBetween("Not between"),
}

enum class SceneFilterInput {
    Text,
    Number,
    Duration,
    Date,
    Timestamp,
    Boolean,
    StringBoolean,
    Resolution,
    Orientation,
    Entity,
    HierarchicalEntity,
    MissingProperty,
    PerceptualHash,
    Duplication,
    StashIds,
    CustomField,
}

enum class FilterEntityKind {
    Performer,
    Studio,
    Tag,
    Group,
    Gallery,
    Folder,
}

data class FilterEntityOption(
    val id: String,
    val label: String,
)

enum class SceneFilterField(
    val label: String,
    val input: SceneFilterInput,
    val nullable: Boolean = true,
    val entityKind: FilterEntityKind? = null,
) {
    Title("Title", SceneFilterInput.Text),
    Code("Scene code", SceneFilterInput.Text),
    Path("Path", SceneFilterInput.Text),
    Folder("Folder", SceneFilterInput.HierarchicalEntity, entityKind = FilterEntityKind.Folder),
    Details("Details", SceneFilterInput.Text),
    Director("Director", SceneFilterInput.Text),
    Oshash("OSHash", SceneFilterInput.Text, nullable = false),
    Checksum("Checksum", SceneFilterInput.Text),
    Phash("Perceptual hash", SceneFilterInput.PerceptualHash),
    Duplicated("Duplicated", SceneFilterInput.Duplication, nullable = false),
    Organized("Organized", SceneFilterInput.Boolean, nullable = false),
    Rating("Rating", SceneFilterInput.Number),
    OCounter("O-counter", SceneFilterInput.Number, nullable = false),
    Resolution("Resolution", SceneFilterInput.Resolution, nullable = false),
    Orientation("Orientation", SceneFilterInput.Orientation, nullable = false),
    FrameRate("Frame rate", SceneFilterInput.Number, nullable = false),
    BitRate("Bit rate", SceneFilterInput.Number, nullable = false),
    VideoCodec("Video codec", SceneFilterInput.Text),
    AudioCodec("Audio codec", SceneFilterInput.Text),
    Duration("Duration", SceneFilterInput.Duration, nullable = false),
    ResumeTime("Resume time", SceneFilterInput.Duration),
    PlayDuration("Play duration", SceneFilterInput.Duration, nullable = false),
    PlayCount("Play count", SceneFilterInput.Number, nullable = false),
    LastPlayedAt("Last played", SceneFilterInput.Timestamp, nullable = false),
    HasMarkers("Has markers", SceneFilterInput.StringBoolean, nullable = false),
    IsMissing("Missing property", SceneFilterInput.MissingProperty, nullable = false),
    Tags("Tags", SceneFilterInput.HierarchicalEntity, entityKind = FilterEntityKind.Tag),
    TagCount("Tag count", SceneFilterInput.Number, nullable = false),
    PerformerTags("Performer tags", SceneFilterInput.HierarchicalEntity, entityKind = FilterEntityKind.Tag),
    Performers("Performers", SceneFilterInput.Entity, entityKind = FilterEntityKind.Performer),
    PerformerCount("Performer count", SceneFilterInput.Number, nullable = false),
    PerformerAge("Performer age", SceneFilterInput.Number, nullable = false),
    PerformerFavorite("Favorite performers", SceneFilterInput.Boolean, nullable = false),
    Studios("Studios", SceneFilterInput.HierarchicalEntity, entityKind = FilterEntityKind.Studio),
    Groups("Groups", SceneFilterInput.HierarchicalEntity, entityKind = FilterEntityKind.Group),
    Galleries("Galleries", SceneFilterInput.Entity, entityKind = FilterEntityKind.Gallery),
    Url("URL", SceneFilterInput.Text),
    StashIds("Stash IDs", SceneFilterInput.StashIds),
    StashIdCount("Stash ID count", SceneFilterInput.Number, nullable = false),
    Interactive("Interactive", SceneFilterInput.Boolean, nullable = false),
    Captions("Captions", SceneFilterInput.Text),
    InteractiveSpeed("Interactive speed", SceneFilterInput.Number, nullable = false),
    FileCount("File count", SceneFilterInput.Number, nullable = false),
    Date("Date", SceneFilterInput.Date),
    CreatedAt("Created", SceneFilterInput.Timestamp, nullable = false),
    UpdatedAt("Updated", SceneFilterInput.Timestamp, nullable = false),
    CustomField("Custom field", SceneFilterInput.CustomField),
    ;

    val defaultModifier: SceneFilterModifier
        get() =
            when {
                this == Captions -> SceneFilterModifier.Includes
                input == SceneFilterInput.Timestamp -> SceneFilterModifier.GreaterThan
                input == SceneFilterInput.Entity || input == SceneFilterInput.HierarchicalEntity ->
                    if (this == Tags || this == PerformerTags || this == Performers) {
                        SceneFilterModifier.IncludesAll
                    } else {
                        SceneFilterModifier.Includes
                    }
                input == SceneFilterInput.StashIds -> SceneFilterModifier.Includes
                else -> SceneFilterModifier.Equals
            }

    val modifiers: List<SceneFilterModifier>
        get() =
            when {
                input == SceneFilterInput.Boolean ||
                    input == SceneFilterInput.StringBoolean ||
                    input == SceneFilterInput.Orientation ||
                    input == SceneFilterInput.MissingProperty ||
                    input == SceneFilterInput.Duplication -> None
                input == SceneFilterInput.Resolution -> ResolutionModifiers
                input == SceneFilterInput.PerceptualHash -> PhashModifiers
                this == Captions -> CaptionModifiers
                input == SceneFilterInput.Entity || input == SceneFilterInput.HierarchicalEntity ->
                    if (this == Tags || this == PerformerTags || this == Performers) {
                        EntityAllModifiers
                    } else {
                        EntityModifiers
                    }
                input == SceneFilterInput.Text -> if (nullable) StringModifiers else MandatoryStringModifiers
                input == SceneFilterInput.Number || input == SceneFilterInput.Duration ->
                    if (nullable) NumberModifiers else MandatoryNumberModifiers
                input == SceneFilterInput.Date -> NumberModifiers
                input == SceneFilterInput.Timestamp ->
                    if (nullable) TimestampModifiers else MandatoryTimestampModifiers
                input == SceneFilterInput.StashIds -> EntityModifiers
                input == SceneFilterInput.CustomField -> StringModifiers
                else -> None
            }

    companion object {
        private val None = emptyList<SceneFilterModifier>()
        private val MandatoryStringModifiers =
            listOf(
                SceneFilterModifier.Equals,
                SceneFilterModifier.NotEquals,
                SceneFilterModifier.Includes,
                SceneFilterModifier.Excludes,
                SceneFilterModifier.MatchesRegex,
                SceneFilterModifier.NotMatchesRegex,
            )
        private val StringModifiers =
            MandatoryStringModifiers +
                listOf(SceneFilterModifier.IsNull, SceneFilterModifier.NotNull)
        private val MandatoryNumberModifiers =
            listOf(
                SceneFilterModifier.Equals,
                SceneFilterModifier.NotEquals,
                SceneFilterModifier.GreaterThan,
                SceneFilterModifier.LessThan,
                SceneFilterModifier.Between,
                SceneFilterModifier.NotBetween,
            )
        private val NumberModifiers =
            MandatoryNumberModifiers +
                listOf(SceneFilterModifier.IsNull, SceneFilterModifier.NotNull)
        private val MandatoryTimestampModifiers =
            listOf(
                SceneFilterModifier.GreaterThan,
                SceneFilterModifier.LessThan,
                SceneFilterModifier.Between,
                SceneFilterModifier.NotBetween,
            )
        private val TimestampModifiers =
            MandatoryTimestampModifiers +
                listOf(SceneFilterModifier.IsNull, SceneFilterModifier.NotNull)
        private val ResolutionModifiers =
            listOf(
                SceneFilterModifier.Equals,
                SceneFilterModifier.NotEquals,
                SceneFilterModifier.GreaterThan,
                SceneFilterModifier.LessThan,
            )
        private val EntityModifiers =
            listOf(
                SceneFilterModifier.Includes,
                SceneFilterModifier.Excludes,
                SceneFilterModifier.IsNull,
                SceneFilterModifier.NotNull,
            )
        private val EntityAllModifiers = listOf(SceneFilterModifier.IncludesAll) + EntityModifiers
        private val CaptionModifiers =
            listOf(
                SceneFilterModifier.Includes,
                SceneFilterModifier.Excludes,
                SceneFilterModifier.IsNull,
                SceneFilterModifier.NotNull,
            )
        private val PhashModifiers =
            listOf(
                SceneFilterModifier.Equals,
                SceneFilterModifier.NotEquals,
                SceneFilterModifier.IsNull,
                SceneFilterModifier.NotNull,
            )
    }
}

data class SceneFilterCriterion(
    val field: SceneFilterField,
    val modifier: SceneFilterModifier = field.defaultModifier,
    val value: String = "",
    val value2: String? = null,
    val selected: List<FilterEntityOption> = emptyList(),
    val excluded: List<FilterEntityOption> = emptyList(),
    val depth: Int = 0,
    val auxiliary: String? = null,
) {
    val isValid: Boolean
        get() {
            if (modifier == SceneFilterModifier.IsNull || modifier == SceneFilterModifier.NotNull) return true
            val needsSecond =
                modifier == SceneFilterModifier.Between || modifier == SceneFilterModifier.NotBetween
            return when (this.field.input) {
                SceneFilterInput.Number,
                SceneFilterInput.Duration,
                -> value.toIntOrNull() != null && (!needsSecond || value2?.toIntOrNull() != null)
                SceneFilterInput.Boolean,
                SceneFilterInput.StringBoolean,
                -> value == "true" || value == "false"
                SceneFilterInput.Resolution -> SceneResolution.entries.any { it.gqlName == value }
                SceneFilterInput.Orientation,
                SceneFilterInput.Entity,
                SceneFilterInput.HierarchicalEntity,
                SceneFilterInput.Duplication,
                -> selected.isNotEmpty()
                SceneFilterInput.PerceptualHash ->
                    value.isNotBlank() && (value2.isNullOrBlank() || value2.toIntOrNull() != null)
                SceneFilterInput.StashIds -> !auxiliary.isNullOrBlank() || value.isNotBlank()
                SceneFilterInput.CustomField -> !auxiliary.isNullOrBlank() && value.isNotBlank()
                else -> value.isNotBlank() && (!needsSecond || !value2.isNullOrBlank())
            }
        }
}

/**
 * User-facing scene filters. Quick fields back the preset controls; [criteria]
 * carries the complete criterion set exposed by Stash's Scenes UI.
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
    val criteria: List<SceneFilterCriterion> = emptyList(),
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
                hasCaptions != null ||
                criteria.any { it.isValid }

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
