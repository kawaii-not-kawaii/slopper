package io.stashapp.android.feature.connection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.stashapp.android.core.domain.ConnectionRepository
import io.stashapp.android.core.model.ConnectionResult
import io.stashapp.android.core.model.ServerInfo
import io.stashapp.android.core.model.StashServer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ConnectionUiState(
    val baseUrl: String = "",
    val apiKey: String = "",
    val displayName: String = "",
    val testing: Boolean = false,
    val error: String? = null,
    val serverInfo: ServerInfo? = null,
    val connected: Boolean = false,
)

@HiltViewModel
class ConnectionViewModel @Inject constructor(
    private val connectionRepository: ConnectionRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ConnectionUiState())
    val state: StateFlow<ConnectionUiState> = _state.asStateFlow()

    fun setUrl(value: String) = _state.update { it.copy(baseUrl = value, error = null) }
    fun setApiKey(value: String) = _state.update { it.copy(apiKey = value, error = null) }
    fun setName(value: String) = _state.update { it.copy(displayName = value) }

    fun test() {
        val s = _state.value
        if (s.testing) return
        _state.update { it.copy(testing = true, error = null, serverInfo = null) }

        viewModelScope.launch {
            val server = StashServer(
                baseUrl = s.baseUrl,
                apiKey = s.apiKey.ifBlank { null },
                displayName = s.displayName.ifBlank { s.baseUrl },
            )
            when (val result = connectionRepository.test(server)) {
                is ConnectionResult.Success -> _state.update {
                    it.copy(testing = false, serverInfo = result.info, error = null)
                }
                is ConnectionResult.AuthFailed -> _state.update {
                    it.copy(testing = false, error = result.message)
                }
                is ConnectionResult.InvalidUrl -> _state.update {
                    it.copy(testing = false, error = result.reason)
                }
                is ConnectionResult.NetworkError -> _state.update {
                    it.copy(testing = false, error = "Can't reach server: ${result.message}")
                }
                is ConnectionResult.ServerError -> _state.update {
                    it.copy(testing = false, error = "Server error: ${result.message}")
                }
            }
        }
    }

    fun connect(onDone: () -> Unit) {
        val s = _state.value
        viewModelScope.launch {
            connectionRepository.setActive(
                StashServer(
                    baseUrl = s.baseUrl,
                    apiKey = s.apiKey.ifBlank { null },
                    displayName = s.displayName.ifBlank { s.baseUrl },
                ),
            )
            _state.update { it.copy(connected = true) }
            onDone()
        }
    }
}
