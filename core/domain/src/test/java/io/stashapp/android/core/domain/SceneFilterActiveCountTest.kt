package io.stashapp.android.core.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SceneFilterActiveCountTest {
    @Test
    fun `empty filter has no active facets`() {
        assertEquals(0, SceneFilter().activeCount)
    }

    @Test
    fun `paired bounds count as one facet each`() {
        val filter =
            SceneFilter(
                minDurationSeconds = 60,
                maxDurationSeconds = 600,
                minRating100 = 40,
                maxRating100 = 80,
                minDate = "2026-01-01",
                maxDate = "2026-12-31",
                minPlayCount = 1,
                maxPlayCount = 10,
                minOCounter = 2,
                maxOCounter = 7,
            )

        assertEquals(5, filter.activeCount)
    }

    @Test
    fun `entity groups and valid stash criteria each count once`() {
        val filter =
            SceneFilter(
                performerIds = listOf("1", "2"),
                tagIds = listOf("3"),
                criteria =
                    listOf(
                        SceneFilterCriterion(SceneFilterField.Title, value = "night"),
                        SceneFilterCriterion(SceneFilterField.Details),
                    ),
            )

        assertEquals(3, filter.activeCount)
    }
}
