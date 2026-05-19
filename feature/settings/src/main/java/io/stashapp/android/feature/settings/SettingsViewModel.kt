package io.stashapp.android.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.stashapp.android.core.common.AppResult
import io.stashapp.android.core.data.prefs.PlayerPreferences
import io.stashapp.android.core.data.prefs.UiPreferences
import io.stashapp.android.core.domain.ConnectionRepository
import io.stashapp.android.core.model.ServerInfo
import io.stashapp.android.core.model.StashServer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel
    @Inject
    constructor(
        private val connectionRepository: ConnectionRepository,
        val playerPrefs: PlayerPreferences,
        val uiPrefs: UiPreferences,
    ) : ViewModel() {

        // --- Existing helpers (unchanged) ---

        fun disconnect(onDone: () -> Unit) {
            viewModelScope.launch {
                connectionRepository.disconnect()
                onDone()
            }
        }

        fun setPlayer(setter: suspend PlayerPreferences.() -> Unit) {
            viewModelScope.launch { playerPrefs.setter() }
        }

        fun setUi(setter: suspend UiPreferences.() -> Unit) {
            viewModelScope.launch { uiPrefs.setter() }
        }

        // --- D-01: server state StateFlows ---

        private val _activeServer = MutableStateFlow<StashServer?>(null)
        val activeServer: StateFlow<StashServer?> = _activeServer.asStateFlow()

        private val _serverInfo = MutableStateFlow<ServerInfo?>(null)
        val serverInfo: StateFlow<ServerInfo?> = _serverInfo.asStateFlow()

        val accentPalette: StateFlow<String> = uiPrefs.accentPalette
            .stateIn(viewModelScope, SharingStarted.Eagerly, "sage")

        fun setAccentPalette(name: String) {
            viewModelScope.launch { uiPrefs.setAccentPalette(name) }
        }

        // --- Search (SETTINGS-11, wired by Plan 6.3) ---
        val searchQuery = MutableStateFlow("")

        val searchResults: StateFlow<List<SettingsSearchEntry>> = searchQuery
            .map { q ->
                if (q.isBlank()) emptyList()
                else SettingsSearchIndex.filter { entry ->
                    (entry.label + " " + entry.hint).contains(q, ignoreCase = true)
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

        fun updateSearchQuery(query: String) {
            searchQuery.value = query
        }

        init {
            viewModelScope.launch {
                connectionRepository.activeServer().collectLatest { server ->
                    _activeServer.value = server
                    if (server != null) {
                        _serverInfo.value = null // show loading/stub state in card
                        when (val result = connectionRepository.test(server)) {
                            is AppResult.Success -> _serverInfo.value = result.data
                            is AppResult.Failure -> _serverInfo.value = null // card shows stub
                        }
                    } else {
                        _serverInfo.value = null
                    }
                }
            }
        }
    }
