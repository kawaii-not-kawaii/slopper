package io.stashapp.android.feature.home

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
 * Smoke test for HomeScreen data model.
 *
 * HomeScreen takes a `viewModel: HomeViewModel = hiltViewModel()` and requires
 * a full Hilt + Activity context. These tests verify the UiState data model the
 * screen consumes is correctly shaped, confirming the module compiles and the
 * screen's data contract is intact.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HomeScreenSmokeTest {
    @Test
    fun `HomeUiState Initial is non-null`() {
        assertNotNull(HomeUiState.Initial)
    }

    @Test
    fun `HomeUiState Initial rails are non-empty`() {
        assertTrue(HomeUiState.Initial.rails.isNotEmpty())
    }

    @Test
    fun `HomeRail initial loading state is true`() {
        val rail = HomeRail(kind = HomeRailKind.RecentlyReleased)
        assertTrue(rail.loading)
        assertTrue(rail.scenes.isEmpty())
    }
}
