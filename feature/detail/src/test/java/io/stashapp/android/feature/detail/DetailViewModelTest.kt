package io.stashapp.android.feature.detail

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DetailViewModelTest {
    @Test
    fun `initial DetailUiState is loading`() {
        val state = DetailUiState()
        assertTrue(state.loading)
    }

    @Test
    fun `initial DetailUiState has no scene`() {
        val state = DetailUiState()
        assertNull(state.scene)
    }

    @Test
    fun `initial DetailUiState has no error`() {
        val state = DetailUiState()
        assertNull(state.error)
    }

    @Test
    fun `DetailUiState is non-null`() {
        assertNotNull(DetailUiState())
    }

    @Test
    fun `state copy reflects loaded state`() {
        val state = DetailUiState()
        val loaded = state.copy(loading = false, error = "Network failure")
        assertFalse(loaded.loading)
        assertNotNull(loaded.error)
    }
}
