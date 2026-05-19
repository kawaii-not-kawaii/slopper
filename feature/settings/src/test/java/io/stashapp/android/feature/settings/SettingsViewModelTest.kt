package io.stashapp.android.feature.settings

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

/**
 * SettingsViewModel has no UiState data class — it surfaces preferences
 * directly via PlayerPreferences/UiPreferences flows.
 * These tests verify the module compiles cleanly with the test infrastructure.
 */
class SettingsViewModelTest {
    @Test
    fun `SettingsViewModel class is loadable`() {
        // Confirms the module compiles and the class is accessible.
        // Full ViewModel instantiation requires Hilt + DataStore setup;
        // deferred to integration tests.
        val clazz = SettingsViewModel::class
        assertNotNull(clazz)
    }

    @Test
    fun `SettingsViewModel class name is correct`() {
        assertEquals("SettingsViewModel", SettingsViewModel::class.simpleName)
    }

    private fun assertEquals(
        expected: String,
        actual: String?,
    ) {
        org.junit.jupiter.api.Assertions
            .assertEquals(expected, actual)
    }
}
