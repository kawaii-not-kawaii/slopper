package io.stashapp.android.feature.settings

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

/**
 * SettingsViewModel has no UiState data class — it surfaces preferences
 * directly via PlayerPreferences/UiPreferences flows.
 * These tests verify the module compiles cleanly with the test infrastructure.
 *
 * Plan 6.3 addition: confirms searchQuery member is present.
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

    @Test
    fun `SettingsViewModel has searchQuery member`() {
        // Confirms the class has the searchQuery property without full Hilt setup.
        val prop = SettingsViewModel::class.members.firstOrNull { it.name == "searchQuery" }
        assertNotNull(prop)
    }

    private fun assertEquals(
        expected: String,
        actual: String?,
    ) {
        org.junit.jupiter.api.Assertions
            .assertEquals(expected, actual)
    }
}
