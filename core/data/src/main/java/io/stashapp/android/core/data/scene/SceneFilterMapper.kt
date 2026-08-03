package io.stashapp.android.core.data.scene

import com.apollographql.apollo.api.Optional
import io.stashapp.android.core.domain.SceneFilter
import io.stashapp.android.core.domain.SceneFilterCriterion
import io.stashapp.android.core.domain.SceneFilterField
import io.stashapp.android.core.domain.SceneFilterModifier
import io.stashapp.android.graphql.type.CriterionModifier
import io.stashapp.android.graphql.type.CustomFieldCriterionInput
import io.stashapp.android.graphql.type.DateCriterionInput
import io.stashapp.android.graphql.type.DuplicationCriterionInput
import io.stashapp.android.graphql.type.FileFilterType
import io.stashapp.android.graphql.type.HierarchicalMultiCriterionInput
import io.stashapp.android.graphql.type.IntCriterionInput
import io.stashapp.android.graphql.type.MultiCriterionInput
import io.stashapp.android.graphql.type.OrientationCriterionInput
import io.stashapp.android.graphql.type.OrientationEnum
import io.stashapp.android.graphql.type.PhashDistanceCriterionInput
import io.stashapp.android.graphql.type.ResolutionCriterionInput
import io.stashapp.android.graphql.type.ResolutionEnum
import io.stashapp.android.graphql.type.SceneFilterType
import io.stashapp.android.graphql.type.StashIDsCriterionInput
import io.stashapp.android.graphql.type.StringCriterionInput

/**
 * Translate our UI-facing [SceneFilter] into Stash's GraphQL [SceneFilterType].
 *
 * Returns null when the filter is empty, so we avoid sending a noisy empty
 * object with the query (keeps the wire protocol clean + easier to debug).
 */
internal fun SceneFilter.toGql(): SceneFilterType? {
    if (!isActive) return null

    // Stash represents repeated criteria as nested AND filters. The editor prevents
    // duplicates, but preserving them here keeps persisted/external filters exact.
    val duplicate =
        criteria
            .filter(SceneFilterCriterion::isValid)
            .filterNot { it.field == SceneFilterField.CustomField }
            .groupBy(SceneFilterCriterion::field)
            .values
            .firstOrNull { it.size > 1 }
            ?.first()
    if (duplicate != null) {
        val remaining = criteria.toMutableList().apply { remove(duplicate) }
        val tail = copy(criteria = remaining).toGql()
        return SceneFilter(criteria = listOf(duplicate))
            .toGql()
            ?.copy(AND = Optional.presentIfNotNull(tail))
    }

    val criteriaByField =
        criteria
            .asSequence()
            .filter(SceneFilterCriterion::isValid)
            .associateBy(SceneFilterCriterion::field)

    fun criterion(field: SceneFilterField) = criteriaByField[field]

    return SceneFilterType(
        title = criterion(SceneFilterField.Title).stringCriterion(),
        code = criterion(SceneFilterField.Code).stringCriterion(),
        path = criterion(SceneFilterField.Path).stringCriterion(),
        details = criterion(SceneFilterField.Details).stringCriterion(),
        director = criterion(SceneFilterField.Director).stringCriterion(),
        oshash = criterion(SceneFilterField.Oshash).stringCriterion(),
        checksum = criterion(SceneFilterField.Checksum).stringCriterion(),
        phash_distance = criterion(SceneFilterField.Phash).phashCriterion(),
        file_count = criterion(SceneFilterField.FileCount).intCriterion(),
        rating100 =
            criterion(SceneFilterField.Rating)?.intCriterion()
                ?: intRange(minRating100, maxRating100),
        organized =
            criterion(SceneFilterField.Organized)?.booleanValue()
                ?: Optional.presentIfNotNull(organized),
        o_counter =
            criterion(SceneFilterField.OCounter)?.intCriterion()
                ?: intRange(minOCounter, maxOCounter),
        duplicated = criterion(SceneFilterField.Duplicated).duplicationCriterion(),
        resolution =
            criterion(SceneFilterField.Resolution)?.resolutionCriterion()
                ?: resolutionCriterion(),
        orientation =
            criterion(SceneFilterField.Orientation)?.orientationCriterion()
                ?: orientationCriterion(),
        framerate = criterion(SceneFilterField.FrameRate).intCriterion(),
        bitrate = criterion(SceneFilterField.BitRate).intCriterion(),
        video_codec = criterion(SceneFilterField.VideoCodec).stringCriterion(),
        audio_codec = criterion(SceneFilterField.AudioCodec).stringCriterion(),
        duration =
            criterion(SceneFilterField.Duration)?.intCriterion()
                ?: intRange(minDurationSeconds, maxDurationSeconds),
        has_markers =
            criterion(SceneFilterField.HasMarkers)?.stringBooleanValue()
                ?: Optional.presentIfNotNull(hasMarkers?.toString()),
        is_missing = criterion(SceneFilterField.IsMissing).rawStringValue(),
        studios =
            criterion(SceneFilterField.Studios)?.hierarchicalCriterion()
                ?: studiosCriterion(),
        groups = criterion(SceneFilterField.Groups).hierarchicalCriterion(),
        galleries = criterion(SceneFilterField.Galleries).multiCriterion(),
        tags =
            criterion(SceneFilterField.Tags)?.hierarchicalCriterion()
                ?: tagsCriterion(),
        tag_count = criterion(SceneFilterField.TagCount).intCriterion(),
        performer_tags = criterion(SceneFilterField.PerformerTags).hierarchicalCriterion(),
        performer_favorite = criterion(SceneFilterField.PerformerFavorite).booleanValue(),
        performer_age = criterion(SceneFilterField.PerformerAge).intCriterion(),
        performers =
            criterion(SceneFilterField.Performers)?.multiCriterion()
                ?: performersCriterion(),
        performer_count = criterion(SceneFilterField.PerformerCount).intCriterion(),
        stash_ids_endpoint = criterion(SceneFilterField.StashIds).stashIdsCriterion(),
        stash_id_count = criterion(SceneFilterField.StashIdCount).intCriterion(),
        url = criterion(SceneFilterField.Url).stringCriterion(),
        interactive =
            criterion(SceneFilterField.Interactive)?.booleanValue()
                ?: Optional.presentIfNotNull(interactive),
        interactive_speed = criterion(SceneFilterField.InteractiveSpeed).intCriterion(),
        captions =
            criterion(SceneFilterField.Captions)?.stringCriterion()
                ?: captionsCriterion(),
        resume_time =
            criterion(SceneFilterField.ResumeTime)?.intCriterion()
                ?: resumeTimeCriterion(),
        play_count =
            criterion(SceneFilterField.PlayCount)?.intCriterion()
                ?: intRange(minPlayCount, maxPlayCount),
        play_duration = criterion(SceneFilterField.PlayDuration).intCriterion(),
        last_played_at = criterion(SceneFilterField.LastPlayedAt).timestampCriterion(),
        date =
            criterion(SceneFilterField.Date)?.dateCriterion()
                ?: dateRange(minDate, maxDate),
        created_at = criterion(SceneFilterField.CreatedAt).timestampCriterion(),
        updated_at = criterion(SceneFilterField.UpdatedAt).timestampCriterion(),
        files_filter = criterion(SceneFilterField.Folder).folderCriterion(),
        custom_fields = criteria.customFieldsCriterion(),
    )
}

private fun SceneFilter.resumeTimeCriterion(): Optional<IntCriterionInput?> =
    when (hasResumeTime) {
        true ->
            Optional.present(
                IntCriterionInput(value = 0, modifier = CriterionModifier.GREATER_THAN),
            )
        false ->
            Optional.present(
                IntCriterionInput(value = 0, modifier = CriterionModifier.EQUALS),
            )
        null -> Optional.absent()
    }

/** BETWEEN / GREATER_THAN / LESS_THAN depending on which bounds are set. */
private fun intRange(
    min: Int?,
    max: Int?,
): Optional<IntCriterionInput?> =
    when {
        min != null && max != null ->
            Optional.present(
                IntCriterionInput(
                    value = min,
                    value2 = Optional.present(max),
                    modifier = CriterionModifier.BETWEEN,
                ),
            )
        min != null ->
            Optional.present(
                IntCriterionInput(value = min, modifier = CriterionModifier.GREATER_THAN),
            )
        max != null ->
            Optional.present(
                IntCriterionInput(value = max, modifier = CriterionModifier.LESS_THAN),
            )
        else -> Optional.absent()
    }

private fun dateRange(
    min: String?,
    max: String?,
): Optional<DateCriterionInput?> =
    when {
        min != null && max != null ->
            Optional.present(
                DateCriterionInput(
                    value = min,
                    value2 = Optional.present(max),
                    modifier = CriterionModifier.BETWEEN,
                ),
            )
        min != null ->
            Optional.present(
                DateCriterionInput(value = min, modifier = CriterionModifier.GREATER_THAN),
            )
        max != null ->
            Optional.present(
                DateCriterionInput(value = max, modifier = CriterionModifier.LESS_THAN),
            )
        else -> Optional.absent()
    }

private fun SceneFilter.orientationCriterion(): Optional<OrientationCriterionInput?> =
    orientation?.let {
        Optional.present(
            OrientationCriterionInput(value = listOf(OrientationEnum.valueOf(it.gqlName))),
        )
    } ?: Optional.absent()

private fun SceneFilter.captionsCriterion(): Optional<StringCriterionInput?> =
    when (hasCaptions) {
        true ->
            Optional.present(
                StringCriterionInput(value = "", modifier = CriterionModifier.NOT_NULL),
            )
        false ->
            Optional.present(
                StringCriterionInput(value = "", modifier = CriterionModifier.IS_NULL),
            )
        null -> Optional.absent()
    }

private fun SceneFilter.resolutionCriterion(): Optional<ResolutionCriterionInput?> =
    minResolution?.let {
        Optional.present(
            ResolutionCriterionInput(
                value = ResolutionEnum.valueOf(it.gqlName),
                modifier = CriterionModifier.GREATER_THAN,
            ),
        )
    } ?: Optional.absent()

private fun SceneFilter.tagsCriterion(): Optional<HierarchicalMultiCriterionInput?> =
    if (tagIds.isEmpty()) {
        Optional.absent()
    } else {
        Optional.present(
            HierarchicalMultiCriterionInput(
                value = Optional.present(tagIds),
                modifier = CriterionModifier.INCLUDES_ALL,
            ),
        )
    }

private fun SceneFilter.studiosCriterion(): Optional<HierarchicalMultiCriterionInput?> =
    if (studioIds.isEmpty()) {
        Optional.absent()
    } else {
        Optional.present(
            HierarchicalMultiCriterionInput(
                value = Optional.present(studioIds),
                modifier = CriterionModifier.INCLUDES,
            ),
        )
    }

private fun SceneFilter.performersCriterion(): Optional<MultiCriterionInput?> =
    if (performerIds.isEmpty()) {
        Optional.absent()
    } else {
        Optional.present(
            MultiCriterionInput(
                value = Optional.present(performerIds),
                modifier = CriterionModifier.INCLUDES_ALL,
            ),
        )
    }

private fun SceneFilterCriterion?.stringCriterion(): Optional<StringCriterionInput?> =
    this?.let {
        Optional.present(
            StringCriterionInput(
                value = value,
                modifier = modifier.toGql(),
            ),
        )
    } ?: Optional.absent()

private fun SceneFilterCriterion?.intCriterion(): Optional<IntCriterionInput?> =
    this?.let {
        Optional.present(
            IntCriterionInput(
                value = value.toIntOrNull() ?: 0,
                value2 = Optional.presentIfNotNull(value2?.toIntOrNull()),
                modifier = modifier.toGql(),
            ),
        )
    } ?: Optional.absent()

private fun SceneFilterCriterion?.dateCriterion(): Optional<DateCriterionInput?> =
    this?.let {
        Optional.present(
            DateCriterionInput(
                value = value,
                value2 = Optional.presentIfNotNull(value2),
                modifier = modifier.toGql(),
            ),
        )
    } ?: Optional.absent()

private fun SceneFilterCriterion?.timestampCriterion(): Optional<io.stashapp.android.graphql.type.TimestampCriterionInput?> =
    this?.let {
        Optional.present(
            io.stashapp.android.graphql.type.TimestampCriterionInput(
                value = value.replace(' ', 'T'),
                value2 = Optional.presentIfNotNull(value2?.replace(' ', 'T')),
                modifier = modifier.toGql(),
            ),
        )
    } ?: Optional.absent()

private fun SceneFilterCriterion?.multiCriterion(): Optional<MultiCriterionInput?> =
    this?.let {
        Optional.present(
            MultiCriterionInput(
                value = Optional.present(selected.map { option -> option.id }),
                modifier = modifier.toGql(),
                excludes = Optional.present(excluded.map { option -> option.id }),
            ),
        )
    } ?: Optional.absent()

private fun SceneFilterCriterion?.hierarchicalCriterion(): Optional<HierarchicalMultiCriterionInput?> =
    this?.let {
        Optional.present(
            HierarchicalMultiCriterionInput(
                value = Optional.present(selected.map { option -> option.id }),
                modifier = modifier.toGql(),
                depth = Optional.present(depth),
                excludes = Optional.present(excluded.map { option -> option.id }),
            ),
        )
    } ?: Optional.absent()

private fun SceneFilterCriterion?.resolutionCriterion(): Optional<ResolutionCriterionInput?> =
    this?.let {
        Optional.present(
            ResolutionCriterionInput(
                value = ResolutionEnum.valueOf(value),
                modifier = modifier.toGql(),
            ),
        )
    } ?: Optional.absent()

private fun SceneFilterCriterion?.orientationCriterion(): Optional<OrientationCriterionInput?> =
    this?.let {
        Optional.present(
            OrientationCriterionInput(
                value = selected.map { option -> OrientationEnum.valueOf(option.id) },
            ),
        )
    } ?: Optional.absent()

private fun SceneFilterCriterion?.phashCriterion(): Optional<PhashDistanceCriterionInput?> =
    this?.let {
        Optional.present(
            PhashDistanceCriterionInput(
                value = value,
                modifier = modifier.toGql(),
                distance = Optional.presentIfNotNull(value2?.toIntOrNull()),
            ),
        )
    } ?: Optional.absent()

private fun SceneFilterCriterion?.duplicationCriterion(): Optional<DuplicationCriterionInput?> =
    this?.let {
        fun flag(id: String): Optional<Boolean?> =
            when {
                selected.any { option -> option.id == id } -> Optional.present(true)
                excluded.any { option -> option.id == id } -> Optional.present(false)
                else -> Optional.absent()
            }
        Optional.present(
            DuplicationCriterionInput(
                phash = flag("phash"),
                url = flag("url"),
                stash_id = flag("stash_id"),
                title = flag("title"),
            ),
        )
    } ?: Optional.absent()

private fun SceneFilterCriterion?.stashIdsCriterion(): Optional<StashIDsCriterionInput?> =
    this?.let {
        Optional.present(
            StashIDsCriterionInput(
                endpoint = Optional.presentIfNotNull(auxiliary?.takeIf(String::isNotBlank)),
                stash_ids =
                    Optional.presentIfNotNull(
                        value
                            .split(',')
                            .map(String::trim)
                            .filter(String::isNotEmpty)
                            .takeIf(List<String>::isNotEmpty),
                    ),
                modifier = modifier.toGql(),
            ),
        )
    } ?: Optional.absent()

private fun SceneFilterCriterion?.folderCriterion(): Optional<FileFilterType?> =
    this?.let {
        Optional.present(
            FileFilterType(parent_folder = hierarchicalCriterion()),
        )
    } ?: Optional.absent()

private fun List<SceneFilterCriterion>.customFieldsCriterion(): Optional<List<CustomFieldCriterionInput>?> {
    val values =
        asSequence()
            .filter { it.field == SceneFilterField.CustomField && it.isValid }
            .map {
                CustomFieldCriterionInput(
                    field = it.auxiliary.orEmpty(),
                    value =
                        Optional.present(
                            it.value
                                .split(',')
                                .map(String::trim)
                                .filter(String::isNotEmpty),
                        ),
                    modifier = it.modifier.toGql(),
                )
            }.toList()
    return Optional.presentIfNotNull(values.takeIf(List<CustomFieldCriterionInput>::isNotEmpty))
}

private fun SceneFilterCriterion?.booleanValue(): Optional<Boolean?> = Optional.presentIfNotNull(this?.value?.toBooleanStrictOrNull())

private fun SceneFilterCriterion?.stringBooleanValue(): Optional<String?> =
    Optional.presentIfNotNull(this?.value?.takeIf { it == "true" || it == "false" })

private fun SceneFilterCriterion?.rawStringValue(): Optional<String?> = Optional.presentIfNotNull(this?.value)

private fun SceneFilterModifier.toGql(): CriterionModifier =
    when (this) {
        SceneFilterModifier.Equals -> CriterionModifier.EQUALS
        SceneFilterModifier.NotEquals -> CriterionModifier.NOT_EQUALS
        SceneFilterModifier.GreaterThan -> CriterionModifier.GREATER_THAN
        SceneFilterModifier.LessThan -> CriterionModifier.LESS_THAN
        SceneFilterModifier.IsNull -> CriterionModifier.IS_NULL
        SceneFilterModifier.NotNull -> CriterionModifier.NOT_NULL
        SceneFilterModifier.IncludesAll -> CriterionModifier.INCLUDES_ALL
        SceneFilterModifier.Includes -> CriterionModifier.INCLUDES
        SceneFilterModifier.Excludes -> CriterionModifier.EXCLUDES
        SceneFilterModifier.MatchesRegex -> CriterionModifier.MATCHES_REGEX
        SceneFilterModifier.NotMatchesRegex -> CriterionModifier.NOT_MATCHES_REGEX
        SceneFilterModifier.Between -> CriterionModifier.BETWEEN
        SceneFilterModifier.NotBetween -> CriterionModifier.NOT_BETWEEN
    }
