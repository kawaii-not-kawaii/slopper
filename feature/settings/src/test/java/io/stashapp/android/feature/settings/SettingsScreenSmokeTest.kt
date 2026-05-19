package io.stashapp.android.feature.settings

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
 * Smoke test for SettingsScreen data model.
 *
 * SettingsScreen takes a `viewModel: SettingsViewModel = hiltViewModel()` which
 * requires a full Hilt + DataStore setup. SettingsViewModel has no UiState data
 * class — it exposes PlayerPreferences and UiPreferences directly as DataStore
 * flows. These tests confirm the module compiles and the ViewModel class contract
 * is accessible.
 *
 * Plan 6.3 additions: SettingsSearchIndex and SettingsSearchEntry smoke tests.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SettingsScreenSmokeTest {
    @Test
    fun `SettingsViewModel class is accessible`() {
        val clazz = SettingsViewModel::class
        assertNotNull(clazz)
        assertNotNull(clazz.simpleName)
    }

    @Test
    fun `SettingsViewModel class simple name is correct`() {
        assertEquals("SettingsViewModel", SettingsViewModel::class.simpleName)
    }

    @Test
    fun `SettingsSearchIndex is non-empty`() {
        assert(SettingsSearchIndex.isNotEmpty())
        assert(SettingsSearchIndex.size >= 20)
    }

    @Test
    fun `SettingsSearchEntry fields are accessible`() {
        val entry = SettingsSearchIndex.first()
        assertNotNull(entry.label)
        assertNotNull(entry.hint)
        assertNotNull(entry.breadcrumb)
        assertNotNull(entry.route)
    }

    private fun assertEquals(
        expected: String,
        actual: String?,
    ) {
        org.junit.jupiter.api.Assertions
            .assertEquals(expected, actual)
    }
}
