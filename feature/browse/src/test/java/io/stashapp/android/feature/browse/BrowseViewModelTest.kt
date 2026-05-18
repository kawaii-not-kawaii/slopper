package io.stashapp.android.feature.browse

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class BrowseViewModelTest {
    @Test
    fun `BrowseUiState constructs with given kind`() {
        val state = BrowseUiState(kind = BrowseKind.Performers)
        assertNotNull(state)
        assertEquals(BrowseKind.Performers, state.kind)
    }

    @Test
    fun `BrowseUiState initial search is empty`() {
        val state = BrowseUiState(kind = BrowseKind.Studios)
        assertEquals("", state.search)
    }

    @Test
    fun `BrowseKind has expected entries`() {
        val kinds = BrowseKind.entries
        assert(BrowseKind.Performers in kinds)
        assert(BrowseKind.Studios in kinds)
        assert(BrowseKind.Tags in kinds)
    }

    @Test
    fun `state copy updates kind`() {
        val state = BrowseUiState(kind = BrowseKind.Performers)
        val updated = state.copy(kind = BrowseKind.Tags)
        assertEquals(BrowseKind.Tags, updated.kind)
    }
}
