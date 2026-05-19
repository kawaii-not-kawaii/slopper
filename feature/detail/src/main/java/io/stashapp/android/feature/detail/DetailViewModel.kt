package io.stashapp.android.feature.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.stashapp.android.core.common.AppResult
import io.stashapp.android.core.domain.SceneRepository
import io.stashapp.android.core.model.SceneDetail
import io.stashapp.android.core.model.SceneSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DetailUiState(
    val loading: Boolean = true,
    val scene: SceneDetail? = null,
    val error: String? = null,
)

@HiltViewModel
class DetailViewModel
    @Inject
    constructor(
        savedState: SavedStateHandle,
        private val sceneRepository: SceneRepository,
    ) : ViewModel() {
        val sceneId: String = savedState["sceneId"] ?: error("sceneId required")

        private val _state = MutableStateFlow(DetailUiState())
        val state: StateFlow<DetailUiState> = _state.asStateFlow()

        init {
            load()
        }

        fun load() {
            _state.update { it.copy(loading = true, error = null) }
            viewModelScope.launch {
                when (val r = sceneRepository.scene(sceneId)) {
                    is AppResult.Success ->
                        _state.update {
                            it.copy(loading = false, scene = r.data, error = null)
                        }
                    is AppResult.Failure ->
                        _state.update {
                            it.copy(loading = false, error = r.error.message)
                        }
                }
            }
        }

        fun setRating(rating100: Int?) {
            // Optimistic update — revert if the server rejects.
            updateSummary { it.copy(rating100 = rating100) }
            viewModelScope.launch {
                val before =
                    state.value.scene
                        ?.summary
                        ?.rating100
                if (sceneRepository.setRating(sceneId, rating100) is AppResult.Failure) {
                    updateSummary { it.copy(rating100 = before) }
                }
            }
        }

        fun setOrganized(organized: Boolean) {
            updateSummary { it.copy(organized = organized) }
            viewModelScope.launch {
                if (sceneRepository.setOrganized(sceneId, organized) is AppResult.Failure) {
                    updateSummary { it.copy(organized = !organized) }
                }
            }
        }

        fun incrementO() {
            viewModelScope.launch {
                when (val r = sceneRepository.incrementO(sceneId)) {
                    is AppResult.Success -> updateSummary { it.copy(oCounter = r.data) }
                    is AppResult.Failure -> Unit
                }
            }
        }

        fun decrementO() {
            viewModelScope.launch {
                when (val r = sceneRepository.decrementO(sceneId)) {
                    is AppResult.Success -> updateSummary { it.copy(oCounter = r.data) }
                    is AppResult.Failure -> Unit
                }
            }
        }

        private inline fun updateSummary(transform: (SceneSummary) -> SceneSummary) {
            _state.update { prev ->
                val scene = prev.scene ?: return@update prev
                prev.copy(scene = scene.copy(summary = transform(scene.summary)))
            }
        }
    }
