package io.stashapp.android.feature.connection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.stashapp.android.core.common.AppError
import io.stashapp.android.core.common.AppResult
import io.stashapp.android.core.domain.ConnectionRepository
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
class ConnectionViewModel
    @Inject
    constructor(
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
                val server =
                    StashServer(
                        baseUrl = s.baseUrl,
                        apiKey = s.apiKey.ifBlank { null },
                        displayName = s.displayName.ifBlank { s.baseUrl },
                    )
                when (val result = connectionRepository.test(server)) {
                    is AppResult.Success ->
                        _state.update {
                            it.copy(testing = false, serverInfo = result.data, error = null)
                        }
                    is AppResult.Failure -> {
                        val msg =
                            when (val err = result.error) {
                                is AppError.Auth -> err.message
                                is AppError.Network -> "Can't reach server: ${err.message}"
                                is AppError.Server -> "Server error: ${err.message}"
                                is AppError.NotFound -> err.message
                                is AppError.Unknown -> err.message
                            }
                        _state.update { it.copy(testing = false, error = msg) }
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
