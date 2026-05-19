package io.stashapp.android.feature.library

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
 * Smoke test for LibraryScreen data model.
 *
 * LibraryScreen takes a `viewModel: LibraryViewModel = hiltViewModel()` and requires
 * a full Hilt + SavedStateHandle + DataStore setup. These tests verify the UiState
 * data class the screen consumes is correctly shaped.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LibraryScreenSmokeTest {
    @Test
    fun `LibraryUiState is non-null`() {
        assertNotNull(LibraryUiState())
    }

    @Test
    fun `LibraryUiState initial search is not expanded`() {
        val state = LibraryUiState()
        assertFalse(state.searchExpanded)
    }

    @Test
    fun `LibraryUiState initial filter is inactive`() {
        val state = LibraryUiState()
        assertFalse(state.query.filter.isActive)
    }
}
