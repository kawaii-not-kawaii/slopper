package io.stashapp.android.feature.library

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class LibraryViewModelTest {
    @Test
    fun `initial LibraryUiState has empty search text`() {
        val state = LibraryUiState()
        assertEquals("", state.searchText)
    }

    @Test
    fun `initial LibraryUiState has search collapsed`() {
        val state = LibraryUiState()
        assertFalse(state.searchExpanded)
    }

    @Test
    fun `initial LibraryUiState has no saved default`() {
        val state = LibraryUiState()
        assertFalse(state.hasSavedDefault)
    }

    @Test
    fun `initial LibraryUiState is non-null`() {
        assertNotNull(LibraryUiState())
    }

    @Test
    fun `state copy with searchExpanded true preserves searchText`() {
        val state = LibraryUiState(searchText = "comedy")
        val updated = state.copy(searchExpanded = true)
        assertEquals("comedy", updated.searchText)
    }
}
