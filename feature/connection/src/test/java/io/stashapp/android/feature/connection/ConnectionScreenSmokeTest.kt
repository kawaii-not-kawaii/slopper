package io.stashapp.android.feature.connection

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
 * Smoke test for ConnectionScreen data model.
 *
 * ConnectionScreen takes a `viewModel: ConnectionViewModel = hiltViewModel()` and
 * requires a full Hilt + Activity context that Robolectric cannot satisfy without
 * a full HiltTestApplication setup (deferred to integration tests).
 *
 * These tests verify that the UiState data class the screen consumes is
 * correctly shaped — confirming the module compiles and the screen's data
 * contract is intact.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ConnectionScreenSmokeTest {
    @Test
    fun `ConnectionUiState default values represent pre-input screen state`() {
        val state = ConnectionUiState()
        assertNotNull(state)
        assertEquals("", state.baseUrl)
        assertEquals("", state.apiKey)
        assertFalse(state.testing)
        assertNull(state.error)
        assertNull(state.serverInfo)
        assertFalse(state.connected)
    }

    @Test
    fun `ConnectionUiState with error represents failed test state`() {
        val state = ConnectionUiState(
            baseUrl = "http://192.168.1.10:9999",
            testing = false,
            error = "Can't reach server: Connection refused",
        )
        assertNotNull(state.error)
        assertFalse(state.testing)
    }
}
