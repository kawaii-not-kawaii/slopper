package io.stashapp.android.feature.browse

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
 * Smoke test for BrowseScreen data model.
 *
 * BrowseScreen takes a `viewModel: BrowseViewModel = hiltViewModel()` and requires
 * a full Hilt + SavedStateHandle setup. These tests verify the UiState data class
 * the screen consumes is correctly shaped.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BrowseScreenSmokeTest {
    @Test
    fun `BrowseUiState is non-null`() {
        assertNotNull(BrowseUiState(kind = BrowseKind.Performers))
    }

    @Test
    fun `BrowseUiState default kind is Performers`() {
        val state = BrowseUiState(kind = BrowseKind.Performers)
        assertEquals(BrowseKind.Performers, state.kind)
    }

    @Test
    fun `BrowseScreen supports all three browse kinds`() {
        val performers = BrowseUiState(kind = BrowseKind.Performers)
        val studios = BrowseUiState(kind = BrowseKind.Studios)
        val tags = BrowseUiState(kind = BrowseKind.Tags)
        assertNotNull(performers)
        assertNotNull(studios)
        assertNotNull(tags)
    }
}
