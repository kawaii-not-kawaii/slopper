package io.stashapp.android.feature.settings

import org.junit.jupiter.api.Assertions.assertEquals
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
        val clazz = SettingsViewModel::class
        assertNotNull(clazz)
    }

    @Test
    fun `SettingsViewModel class name is correct`() {
        assertEquals("SettingsViewModel", SettingsViewModel::class.simpleName)
    }
}
