package io.stashapp.android.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.stashapp.android.core.common.AppResult
import io.stashapp.android.core.domain.SceneFilter
import io.stashapp.android.core.domain.SceneQuery
import io.stashapp.android.core.domain.SceneRepository
import io.stashapp.android.core.domain.SceneSort
import io.stashapp.android.core.model.SceneSummary
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Identifier + display label for each home rail. Order here controls the order
 * they appear on the screen.
 */
enum class HomeRailKind(
    val title: String,
) {
    ContinueWatching("Continue watching"),
    RecentlyReleased("Recently released"),
    RecentlyAdded("Recently added"),
    MostPlayed("Most played"),
}

data class HomeRail(
    val kind: HomeRailKind,
    val loading: Boolean = true,
    val scenes: List<SceneSummary> = emptyList(),
    val error: String? = null,
)

data class HomeUiState(
    val rails: List<HomeRail>,
) {
    companion object {
        val Initial = HomeUiState(HomeRailKind.entries.map { HomeRail(it) })
    }
}

@HiltViewModel
class HomeViewModel
    @Inject
    constructor(
        private val sceneRepository: SceneRepository,
    ) : ViewModel() {
        private val _state = MutableStateFlow(HomeUiState.Initial)
        val state: StateFlow<HomeUiState> = _state.asStateFlow()

        init {
            load()
        }

        /** Refetch all rails in parallel. Individual failures show per-rail. */
        fun load() {
            _state.update { HomeUiState.Initial }
            viewModelScope.launch {
                HomeRailKind.entries
                    .map { kind ->
                        async { kind to sceneRepository.scenes(queryFor(kind), limit = 20) }
                    }.awaitAll()
                    .forEach { (kind, result) ->
                        _state.update { prev ->
                            val updated =
                                prev.rails.map { rail ->
                                    if (rail.kind != kind) {
                                        rail
                                    } else {
                                        when (result) {
                                            is AppResult.Success -> rail.copy(loading = false, scenes = result.data, error = null)
                                            is AppResult.Failure -> rail.copy(loading = false, error = result.error.message)
                                        }
                                    }
                                }
                            prev.copy(rails = updated)
                        }
                    }
            }
        }

        /**
         * SceneQuery for each rail. Matches Stash's web-UI front-page defaults:
         *  - Recently released → sort by `date` DESC
         *  - Recently added    → sort by `created_at` DESC
         *  - Most played       → sort by `play_count` DESC
         *  - Continue watching → scenes with resume_time > 0, sorted by last_played_at
         */
        private fun queryFor(kind: HomeRailKind): SceneQuery =
            when (kind) {
                HomeRailKind.ContinueWatching ->
                    SceneQuery(
                        sort = SceneSort.RecentlyPlayed,
                        filter = SceneFilter(hasResumeTime = true),
                    )
                HomeRailKind.RecentlyReleased -> SceneQuery(sort = SceneSort.DateDesc)
                HomeRailKind.RecentlyAdded -> SceneQuery(sort = SceneSort.CreatedDesc)
                HomeRailKind.MostPlayed -> SceneQuery(sort = SceneSort.PlayCount)
            }
    }
