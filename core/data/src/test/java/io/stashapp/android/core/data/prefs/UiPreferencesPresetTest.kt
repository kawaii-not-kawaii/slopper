package io.stashapp.android.core.data.prefs

import io.stashapp.android.core.domain.SceneFilter
import io.stashapp.android.core.domain.SceneResolution
import io.stashapp.android.core.domain.SceneSort
import io.stashapp.android.core.domain.SceneSortDirection
import io.stashapp.android.core.domain.SceneSortField
import kotlinx.serialization.encodeToString
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class UiPreferencesPresetTest {
    @Test
    fun `preset json round trips filter and sort`() {
        val stored =
            StoredPreset(
                id = "preset-1",
                name = "Evening 4K",
                filter =
                    StoredSceneFilter.from(
                        SceneFilter(
                            minResolution = SceneResolution.Uhd4k,
                            minDurationSeconds = 15 * 60,
                            maxDurationSeconds = 30 * 60,
                        ),
                    ),
                sort = "Title:Ascending",
            )

        val raw = StoredPresetCodec.json.encodeToString(listOf(stored))
        val decoded = StoredPresetCodec.decodeDomain(raw).single()

        assertEquals("preset-1", decoded.id)
        assertEquals("Evening 4K", decoded.name)
        assertEquals(SceneResolution.Uhd4k, decoded.filter.minResolution)
        assertEquals(15 * 60, decoded.filter.minDurationSeconds)
        assertEquals(30 * 60, decoded.filter.maxDurationSeconds)
        assertEquals(SceneSort(SceneSortField.Title, SceneSortDirection.Ascending), decoded.sort)
    }

    @Test
    fun `corrupt preset blob returns empty`() {
        assertTrue(StoredPresetCodec.decodeDomain("{ definitely not json").isEmpty())
    }
}
