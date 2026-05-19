package io.stashapp.android.feature.connection

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ConnectionViewModelTest {
    @Test
    fun `initial state is not testing`() {
        val state = ConnectionUiState()
        assertFalse(state.testing)
    }

    @Test
    fun `initial state has empty url and apiKey`() {
        val state = ConnectionUiState()
        assertEquals("", state.baseUrl)
        assertEquals("", state.apiKey)
    }

    @Test
    fun `initial state has no error`() {
        val state = ConnectionUiState()
        assertNull(state.error)
    }

    @Test
    fun `initial state is not connected`() {
        val state = ConnectionUiState()
        assertFalse(state.connected)
    }

    @Test
    fun `state copy preserves baseUrl when updating testing flag`() {
        val state = ConnectionUiState(baseUrl = "http://example.com")
        val updated = state.copy(testing = true)
        assertNotNull(updated)
        assertEquals("http://example.com", updated.baseUrl)
    }
}
