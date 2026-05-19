package io.stashapp.android.feature.browse

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import io.stashapp.android.core.domain.BrowseRepository
import io.stashapp.android.core.domain.EntitySort
import io.stashapp.android.core.model.PerformerBrowseItem
import io.stashapp.android.core.model.StudioBrowseItem
import io.stashapp.android.core.model.TagBrowseItem
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import javax.inject.Inject

enum class BrowseKind { Performers, Studios, Tags }

data class BrowseUiState(
    val kind: BrowseKind,
    val search: String = "",
    val sort: EntitySort = EntitySort.Name,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class BrowseViewModel
    @Inject
    constructor(
        savedState: SavedStateHandle,
        private val browseRepository: BrowseRepository,
    ) : ViewModel() {
        private val kind: BrowseKind =
            when (savedState.get<String>("kind")?.lowercase()) {
                "studios" -> BrowseKind.Studios
                "tags" -> BrowseKind.Tags
                else -> BrowseKind.Performers
            }

        private val _ui = MutableStateFlow(BrowseUiState(kind = kind))
        val ui: StateFlow<BrowseUiState> = _ui.asStateFlow()

        private data class Q(
            val search: String?,
            val sort: EntitySort,
        )

        private val queryFlow = MutableStateFlow(Q(null, EntitySort.Name))

        val performers: Flow<PagingData<PerformerBrowseItem>> =
            queryFlow
                .flatMapLatest { browseRepository.performers(it.search, it.sort) }
                .cachedIn(viewModelScope)

        val studios: Flow<PagingData<StudioBrowseItem>> =
            queryFlow
                .flatMapLatest { browseRepository.studios(it.search, it.sort) }
                .cachedIn(viewModelScope)

        val tags: Flow<PagingData<TagBrowseItem>> =
            queryFlow
                .flatMapLatest { browseRepository.tags(it.search, it.sort) }
                .cachedIn(viewModelScope)

        fun setSearch(text: String) {
            _ui.value = _ui.value.copy(search = text)
            queryFlow.value = queryFlow.value.copy(search = text.ifBlank { null })
        }

        fun setSort(sort: EntitySort) {
            _ui.value = _ui.value.copy(sort = sort)
            queryFlow.value = queryFlow.value.copy(sort = sort)
        }
    }
