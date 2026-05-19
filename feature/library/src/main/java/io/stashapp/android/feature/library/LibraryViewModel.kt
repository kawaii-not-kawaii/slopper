package io.stashapp.android.feature.library

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import io.stashapp.android.core.data.prefs.UiPreferences
import io.stashapp.android.core.domain.SceneFilter
import io.stashapp.android.core.domain.SceneQuery
import io.stashapp.android.core.domain.SceneRepository
import io.stashapp.android.core.domain.SceneSort
import io.stashapp.android.core.model.SceneSummary
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LibraryUiState(
    val query: SceneQuery = SceneQuery(),
    val searchText: String = "",
    val searchExpanded: Boolean = false,
    /** Whether the user has a persisted default filter — used to decide if
     *  "Reset to default" should be shown / labelled differently. */
    val hasSavedDefault: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class LibraryViewModel
    @Inject
    constructor(
        savedState: SavedStateHandle,
        private val sceneRepository: SceneRepository,
        private val uiPreferences: UiPreferences,
    ) : ViewModel() {
        /** Filter derived from an optional nav preset like "tag:42". */
        private val presetFilter: SceneFilter = parsePreset(savedState["preset"])

        private val queryFlow = MutableStateFlow(SceneQuery(filter = presetFilter))
        private val uiFlow = MutableStateFlow(LibraryUiState(query = queryFlow.value))
        val state: StateFlow<LibraryUiState> = uiFlow.asStateFlow()

        val scenes: Flow<PagingData<SceneSummary>> =
            queryFlow
                .flatMapLatest { sceneRepository.pagedScenes(it) }
                .cachedIn(viewModelScope)

        init {
            // If we weren't launched with a preset, apply the user's default filter
            // (if any). Deep-links win — a preset is a more specific intent.
            if (!presetFilter.isActive) {
                viewModelScope.launch {
                    val saved = uiPreferences.defaultSceneFilter.first()
                    if (saved != null) updateQuery { it.copy(filter = saved) }
                    uiFlow.value = uiFlow.value.copy(hasSavedDefault = saved != null)
                }
            }
        }

        fun setSearchExpanded(expanded: Boolean) {
            uiFlow.value = uiFlow.value.copy(searchExpanded = expanded)
            if (!expanded && uiFlow.value.searchText.isNotEmpty()) setSearchText("")
        }

        fun setSearchText(text: String) {
            uiFlow.value = uiFlow.value.copy(searchText = text)
            updateQuery { it.copy(searchText = text.ifBlank { null }) }
        }

        fun setSort(sort: SceneSort) = updateQuery { it.copy(sort = sort) }

        fun setFilter(filter: SceneFilter) = updateQuery { it.copy(filter = filter) }

        fun clearFilter() = updateQuery { it.copy(filter = SceneFilter()) }

        /** Persist the current filter as the user's default. */
        fun saveAsDefault() {
            val current = queryFlow.value.filter
            viewModelScope.launch {
                uiPreferences.setDefaultSceneFilter(current)
                uiFlow.value = uiFlow.value.copy(hasSavedDefault = current.isActive)
            }
        }

        /** Drop the saved default — next open will start empty. */
        fun clearDefault() {
            viewModelScope.launch {
                uiPreferences.setDefaultSceneFilter(null)
                uiFlow.value = uiFlow.value.copy(hasSavedDefault = false)
            }
        }

        private inline fun updateQuery(transform: (SceneQuery) -> SceneQuery) {
            val next = transform(queryFlow.value)
            queryFlow.value = next
            uiFlow.value = uiFlow.value.copy(query = next)
        }
    }

/**
 * Parse a nav preset token like "tag:42" or "performer:17" or "studio:3" into
 * a [SceneFilter] with that single criterion pre-selected. Unknown formats
 * yield an empty filter rather than throwing — keeps deep-linking forgiving.
 */
private fun parsePreset(raw: String?): SceneFilter {
    if (raw.isNullOrBlank()) return SceneFilter()
    val (kind, value) =
        raw.split(":", limit = 2).let {
            if (it.size != 2) return SceneFilter()
            it[0] to it[1]
        }
    return when (kind) {
        "tag" -> SceneFilter(tagIds = listOf(value))
        "performer" -> SceneFilter(performerIds = listOf(value))
        "studio" -> SceneFilter(studioIds = listOf(value))
        else -> SceneFilter()
    }
}
