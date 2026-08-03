package io.stashapp.android.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.stashapp.android.core.common.AppResult
import io.stashapp.android.core.data.prefs.PlayerPreferences
import io.stashapp.android.core.data.prefs.UiPreferences
import io.stashapp.android.core.domain.ConnectionRepository
import io.stashapp.android.core.domain.SavedFilterPreset
import io.stashapp.android.core.model.ServerInfo
import io.stashapp.android.core.model.StashServer
import io.stashapp.android.core.ui.image.StashImageLoaderFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel
    @Inject
    constructor(
        private val connectionRepository: ConnectionRepository,
        val playerPrefs: PlayerPreferences,
        val uiPrefs: UiPreferences,
        private val imageLoaderFactory: StashImageLoaderFactory,
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

        private val _serverInfoFetchedAt = MutableStateFlow<Long?>(null)
        val serverInfoFetchedAt: StateFlow<Long?> = _serverInfoFetchedAt.asStateFlow()

        private val _serverLatencyMs = MutableStateFlow<Long?>(null)
        val serverLatencyMs: StateFlow<Long?> = _serverLatencyMs.asStateFlow()

        private val _cacheSizeBytes = MutableStateFlow(imageLoaderFactory.diskCacheSizeBytes())
        val cacheSizeBytes: StateFlow<Long> = _cacheSizeBytes.asStateFlow()

        val presets: StateFlow<List<SavedFilterPreset>> =
            uiPrefs.savedFilterPresets.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

        val accentPalette: StateFlow<String> =
            uiPrefs.accentPalette
                .stateIn(viewModelScope, SharingStarted.Eagerly, "sage")

        fun setAccentPalette(name: String) {
            viewModelScope.launch { uiPrefs.setAccentPalette(name) }
        }

        fun setImageCacheSizeMb(value: Int) {
            viewModelScope.launch {
                uiPrefs.setImageCacheSizeMb(value)
                withContext(Dispatchers.IO) { imageLoaderFactory.resizeDiskCache(value) }
                _cacheSizeBytes.value = imageLoaderFactory.diskCacheSizeBytes()
            }
        }

        fun clearImageCache() {
            viewModelScope.launch {
                withContext(Dispatchers.IO) { imageLoaderFactory.clearDiskCache() }
                _cacheSizeBytes.value = imageLoaderFactory.diskCacheSizeBytes()
            }
        }

        fun setBlurThumbnails(enabled: Boolean) {
            viewModelScope.launch { uiPrefs.setBlurThumbnails(enabled) }
        }

        fun deletePreset(id: String) {
            viewModelScope.launch { uiPrefs.deleteFilterPreset(id) }
        }

        fun renamePreset(
            id: String,
            name: String,
        ) {
            viewModelScope.launch { uiPrefs.renameFilterPreset(id, name) }
        }

        init {
            viewModelScope.launch {
                connectionRepository
                    .activeServer()
                    .distinctUntilChanged()
                    .collectLatest { server ->
                        _activeServer.value = server
                        if (server != null) {
                            _serverInfo.value = null // show loading/stub state in card
                            _serverInfoFetchedAt.value = null
                            _serverLatencyMs.value = null
                            val startedAt = System.nanoTime()
                            when (val result = connectionRepository.test(server)) {
                                is AppResult.Success -> {
                                    _serverInfo.value = result.data
                                    _serverLatencyMs.value = (System.nanoTime() - startedAt) / 1_000_000
                                    _serverInfoFetchedAt.value = System.currentTimeMillis()
                                }
                                is AppResult.Failure -> _serverInfo.value = null // card shows stub
                            }
                        } else {
                            _serverInfo.value = null
                            _serverInfoFetchedAt.value = null
                            _serverLatencyMs.value = null
                        }
                    }
            }
        }
    }
