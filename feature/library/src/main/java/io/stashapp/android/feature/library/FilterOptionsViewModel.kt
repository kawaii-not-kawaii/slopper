package io.stashapp.android.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apollographql.apollo.exception.ApolloException
import dagger.hilt.android.lifecycle.HiltViewModel
import io.stashapp.android.core.domain.BrowseRepository
import io.stashapp.android.core.domain.FilterEntityKind
import io.stashapp.android.core.domain.FilterEntityOption
import io.stashapp.android.core.domain.FilterOptionQuery
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FilterOptionsUiState(
    val kind: FilterEntityKind? = null,
    val search: String = "",
    val options: List<FilterEntityOption> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class FilterOptionsViewModel
    @Inject
    constructor(
        private val repository: BrowseRepository,
    ) : ViewModel() {
        private val mutableState = MutableStateFlow(FilterOptionsUiState())
        val state: StateFlow<FilterOptionsUiState> = mutableState.asStateFlow()
        private var searchJob: Job? = null

        fun search(
            kind: FilterEntityKind,
            search: String,
        ) {
            searchJob?.cancel()
            val previous = mutableState.value
            mutableState.value =
                previous.copy(
                    kind = kind,
                    search = search,
                    options = previous.options.takeIf { previous.kind == kind }.orEmpty(),
                    loading = true,
                    error = null,
                )
            searchJob =
                viewModelScope.launch {
                    try {
                        delay(200)
                        val options = repository.filterOptions(FilterOptionQuery(kind, search))
                        mutableState.value = mutableState.value.copy(options = options, loading = false)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: ApolloException) {
                        showError(e)
                    } catch (e: IllegalStateException) {
                        showError(e)
                    }
                }
        }

        private fun showError(error: Exception) {
            mutableState.value =
                mutableState.value.copy(
                    options = emptyList(),
                    loading = false,
                    error = error.message ?: "Unable to load options",
                )
        }
    }
