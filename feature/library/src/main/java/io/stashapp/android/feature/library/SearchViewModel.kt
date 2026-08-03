package io.stashapp.android.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apollographql.apollo.exception.ApolloException
import dagger.hilt.android.lifecycle.HiltViewModel
import io.stashapp.android.core.common.AppResult
import io.stashapp.android.core.domain.BrowseRepository
import io.stashapp.android.core.domain.FilterEntityKind
import io.stashapp.android.core.domain.FilterOptionQuery
import io.stashapp.android.core.domain.SceneQuery
import io.stashapp.android.core.domain.SceneRepository
import io.stashapp.android.core.domain.UiSettings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchUiState(
    val query: String = "",
    val results: SearchResults = SearchResults(),
    val loading: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class SearchViewModel
    @Inject
    constructor(
        private val sceneRepository: SceneRepository,
        private val browseRepository: BrowseRepository,
        private val uiSettings: UiSettings,
    ) : ViewModel() {
        private val mutableState = MutableStateFlow(SearchUiState())
        val state: StateFlow<SearchUiState> = mutableState.asStateFlow()
        val recentSearches: StateFlow<List<String>> =
            uiSettings.recentSearches.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
        private var searchJob: Job? = null

        fun setQuery(query: String) {
            searchJob?.cancel()
            mutableState.value =
                mutableState.value.copy(
                    query = query,
                    results = if (query.isBlank()) SearchResults() else mutableState.value.results,
                    loading = query.isNotBlank(),
                    error = null,
                )
            if (query.isBlank()) return

            searchJob =
                viewModelScope.launch {
                    try {
                        delay(200)
                        val cleanQuery = query.trim()
                        val fetched =
                            coroutineScope {
                                val scenes = async { sceneRepository.scenePage(SceneQuery(searchText = cleanQuery), limit = 20) }
                                val performers = async { options(FilterEntityKind.Performer, cleanQuery) }
                                val studios = async { options(FilterEntityKind.Studio, cleanQuery) }
                                val tags = async { options(FilterEntityKind.Tag, cleanQuery) }
                                SearchFetch(scenes.await(), performers.await(), studios.await(), tags.await())
                            }
                        val sceneResult = fetched.scenes
                        val scenePage = (sceneResult as? AppResult.Success)?.data
                        mutableState.value =
                            mutableState.value.copy(
                                results =
                                    SearchResults(
                                        scenes = scenePage?.scenes.orEmpty(),
                                        performers = fetched.performers.options,
                                        studios = fetched.studios.options,
                                        tags = fetched.tags.options,
                                        // Server match totals, not page sizes.
                                        sceneTotal = scenePage?.total ?: 0,
                                        performerTotal = fetched.performers.total,
                                        studioTotal = fetched.studios.total,
                                        tagTotal = fetched.tags.total,
                                    ),
                                loading = false,
                                error = (sceneResult as? AppResult.Failure)?.error?.message,
                            )
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: ApolloException) {
                        showError(e)
                    } catch (e: IllegalStateException) {
                        showError(e)
                    }
                }
        }

        fun rememberSearch(query: String = mutableState.value.query) {
            viewModelScope.launch { uiSettings.addRecentSearch(query) }
        }

        private suspend fun options(
            kind: FilterEntityKind,
            query: String,
        ) = browseRepository.filterOptionsPage(FilterOptionQuery(kind, query))

        private fun showError(error: Exception) {
            mutableState.value =
                mutableState.value.copy(
                    results = SearchResults(),
                    loading = false,
                    error = error.message ?: "Unable to search",
                )
        }
    }

private data class SearchFetch(
    val scenes: AppResult<io.stashapp.android.core.domain.ScenePage>,
    val performers: io.stashapp.android.core.domain.FilterOptionPage,
    val studios: io.stashapp.android.core.domain.FilterOptionPage,
    val tags: io.stashapp.android.core.domain.FilterOptionPage,
)
