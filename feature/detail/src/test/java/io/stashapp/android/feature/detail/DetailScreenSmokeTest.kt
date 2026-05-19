package io.stashapp.android.feature.detail

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
 * Smoke test for DetailScreen data model.
 *
 * DetailScreen takes a `viewModel: DetailViewModel = hiltViewModel()` and requires
 * a full Hilt + SavedStateHandle setup with a valid sceneId. These tests verify
 * the UiState data class the screen consumes is correctly shaped.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DetailScreenSmokeTest {
    @Test
    fun `DetailUiState initial state is loading`() {
        val state = DetailUiState()
        assertNotNull(state)
        assertTrue(state.loading)
    }

    @Test
    fun `DetailUiState initial scene is null`() {
        val state = DetailUiState()
        assertNull(state.scene)
    }

    @Test
    fun `DetailUiState error state has message`() {
        val state = DetailUiState(loading = false, error = "Scene not found")
        assertNotNull(state.error)
    }
}
