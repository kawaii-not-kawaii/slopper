package io.stashapp.android.feature.home

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class HomeViewModelTest {
    @Test
    fun `Initial state is non-null`() {
        assertNotNull(HomeUiState.Initial)
    }

    @Test
    fun `Initial state has loading rails`() {
        val initial = HomeUiState.Initial
        assertTrue(initial.rails.isNotEmpty())
        assertTrue(initial.rails.all { it.loading })
    }

    @Test
    fun `Initial rails count matches HomeRailKind entries`() {
        assertEquals(HomeRailKind.entries.size, HomeUiState.Initial.rails.size)
    }

    @Test
    fun `Initial rails have empty scenes`() {
        val initial = HomeUiState.Initial
        assertTrue(initial.rails.all { it.scenes.isEmpty() })
    }

    @Test
    fun `HomeRailKind entries are all represented`() {
        val kinds =
            HomeUiState.Initial.rails
                .map { it.kind }
                .toSet()
        assertEquals(HomeRailKind.entries.toSet(), kinds)
    }
}
