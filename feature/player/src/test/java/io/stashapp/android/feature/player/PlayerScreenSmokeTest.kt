package io.stashapp.android.feature.player

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
 * Smoke test for PlayerScreen data model.
 *
 * PlayerScreen takes a `viewModel: PlayerViewModel = hiltViewModel()` which requires
 * a full Application + SavedStateHandle + ExoPlayer setup — not suitable for
 * unit-level tests. These tests verify the UiState data contracts consumed by
 * the screen are correctly shaped.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PlayerScreenSmokeTest {
    @Test
    fun `PlayerUiState initial state is loading`() {
        val state = PlayerUiState()
        assertNotNull(state)
        assertTrue(state.loading)
    }

    @Test
    fun `PlayerUiState initial current scene is null`() {
        val state = PlayerUiState()
        assertNull(state.current)
    }

    @Test
    fun `PlayerUiState default playback speed is 1x`() {
        val state = PlayerUiState()
        assertEquals(1f, state.playbackSpeed)
    }

    @Test
    fun `PlayerPositionState default positions are zero`() {
        val pos = PlayerPositionState()
        assertEquals(0L, pos.positionMs)
        assertEquals(0L, pos.durationMs)
        assertEquals(0L, pos.bufferedMs)
    }

    @Test
    fun `PLAYBACK_SPEEDS array is non-empty`() {
        val speeds = PlayerViewModel.PLAYBACK_SPEEDS.toList()
        assertTrue(speeds.isNotEmpty())
        assertTrue(speeds.any { kotlin.math.abs(it - 1.0f) < 0.001f })
    }
}
