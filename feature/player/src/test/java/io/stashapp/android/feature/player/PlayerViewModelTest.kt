package io.stashapp.android.feature.player

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PlayerViewModelTest {
    @Test
    fun `initial PlayerUiState is loading`() {
        val state = PlayerUiState()
        assertTrue(state.loading)
    }

    @Test
    fun `initial PlayerUiState has no current scene`() {
        val state = PlayerUiState()
        assertNull(state.current)
    }

    @Test
    fun `initial PlayerUiState has no error`() {
        val state = PlayerUiState()
        assertNull(state.error)
    }

    @Test
    fun `initial PlayerUiState has default playback speed`() {
        val state = PlayerUiState()
        assertEquals(1f, state.playbackSpeed)
    }

    @Test
    fun `PlayerPositionState initial values are zero`() {
        val pos = PlayerPositionState()
        assertNotNull(pos)
        assertEquals(0L, pos.positionMs)
        assertEquals(0L, pos.durationMs)
        assertEquals(0L, pos.bufferedMs)
    }

    @Test
    fun `formatSpeed formats whole numbers without decimal`() {
        assertEquals("1x", PlayerViewModel.formatSpeed(1.0f))
        assertEquals("2x", PlayerViewModel.formatSpeed(2.0f))
    }

    @Test
    fun `formatSpeed formats fractional speeds correctly`() {
        assertEquals("0.5x", PlayerViewModel.formatSpeed(0.5f))
        assertEquals("1.25x", PlayerViewModel.formatSpeed(1.25f))
    }

    @Test
    fun `PLAYBACK_SPEEDS array is non-empty and includes 1x`() {
        val speeds = PlayerViewModel.PLAYBACK_SPEEDS.toList()
        assertTrue(speeds.isNotEmpty())
        assertTrue(speeds.any { kotlin.math.abs(it - 1.0f) < 0.001f })
    }
}
