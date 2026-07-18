package io.stashapp.android.core.data.scene

import com.apollographql.apollo.api.Optional
import io.stashapp.android.core.domain.SceneFilter
import io.stashapp.android.graphql.type.CriterionModifier
import io.stashapp.android.graphql.type.DateCriterionInput
import io.stashapp.android.graphql.type.HierarchicalMultiCriterionInput
import io.stashapp.android.graphql.type.IntCriterionInput
import io.stashapp.android.graphql.type.MultiCriterionInput
import io.stashapp.android.graphql.type.OrientationCriterionInput
import io.stashapp.android.graphql.type.OrientationEnum
import io.stashapp.android.graphql.type.ResolutionCriterionInput
import io.stashapp.android.graphql.type.ResolutionEnum
import io.stashapp.android.graphql.type.SceneFilterType
import io.stashapp.android.graphql.type.StringCriterionInput

/**
 * Translate our UI-facing [SceneFilter] into Stash's GraphQL [SceneFilterType].
 *
 * Returns null when the filter is empty, so we avoid sending a noisy empty
 * object with the query (keeps the wire protocol clean + easier to debug).
 */
internal fun SceneFilter.toGql(): SceneFilterType? {
    if (!isActive) return null

    return SceneFilterType(
        rating100 = intRange(minRating100, maxRating100),
        resolution = resolutionCriterion(),
        organized = Optional.presentIfNotNull(organized),
        has_markers = Optional.presentIfNotNull(hasMarkers?.let { if (it) "true" else "false" }),
        interactive = Optional.presentIfNotNull(interactive),
        tags = tagsCriterion(),
        studios = studiosCriterion(),
        performers = performersCriterion(),
        resume_time = resumeTimeCriterion(),
        duration = intRange(minDurationSeconds, maxDurationSeconds),
        play_count = intRange(minPlayCount, maxPlayCount),
        o_counter = intRange(minOCounter, maxOCounter),
        date = dateRange(minDate, maxDate),
        orientation = orientationCriterion(),
        captions = captionsCriterion(),
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
