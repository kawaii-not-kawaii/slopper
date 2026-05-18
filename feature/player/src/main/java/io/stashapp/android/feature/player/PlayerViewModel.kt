package io.stashapp.android.feature.player

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlaybackException
import androidx.media3.exoplayer.ExoPlayer
import dagger.hilt.android.lifecycle.HiltViewModel
import io.stashapp.android.core.common.AppResult
import io.stashapp.android.core.domain.PlayerSettings
import io.stashapp.android.core.domain.SceneRepository
import io.stashapp.android.core.model.QueueState
import io.stashapp.android.core.model.RepeatMode
import io.stashapp.android.core.model.SceneDetail
import io.stashapp.android.core.network.StashEndpointProvider
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import javax.inject.Inject

/**
 * Slow-changing control state. Collected once at the top of the player screen;
 * most transitions flip a boolean or swap a string so recomposition cost is
 * low and bounded.
 */
data class PlayerUiState(
    val loading: Boolean = true,
    val current: SceneDetail? = null,
    val queue: QueueState? = null,
    val error: String? = null,
    val isPlaying: Boolean = false,
    val playbackSpeed: Float = 1f,
    /** Current video's native fps, once the first frame has been decoded.
     *  Emitted so the UI can call `Surface.setFrameRate()` for VRR panels. */
    val videoFrameRate: Float? = null,
    /** Transient banner text (e.g. "Shuffle on · next: …"). Shown for a few seconds. */
    val banner: String? = null,
)

/**
 * Fast-changing playhead state. Kept in its own flow so the 250ms ticker only
 * recomposes the timeline / time labels — not the full control overlay with
 * its gradient scrim, icon row, and large-button transport.
 */
data class PlayerPositionState(
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val bufferedMs: Long = 0L,
)

@HiltViewModel
class PlayerViewModel
    @Inject
    constructor(
        application: Application,
        savedState: SavedStateHandle,
        private val sceneRepository: SceneRepository,
        private val endpointProvider: StashEndpointProvider,
        private val okHttpClient: OkHttpClient,
        val preferences: PlayerSettings,
    ) : AndroidViewModel(application) {
        private val startSceneId: String = savedState["sceneId"] ?: error("sceneId required")
        private val queueIds: List<String> =
            savedState
                .get<String>("queueIds")
                .orEmpty()
                .split(",")
                .filter { it.isNotBlank() }
                .ifEmpty { listOf(startSceneId) }
        private val startIndex: Int = savedState["index"] ?: 0

        // startMs < 0 means "no explicit start — use resume_time if present"
        private val explicitStartMs: Long = savedState["startMs"] ?: -1L

        private val queue = PlayerQueue.from(queueIds, startIndex)

        private val _state = MutableStateFlow(PlayerUiState(queue = queue.snapshot()))
        val state: StateFlow<PlayerUiState> = _state.asStateFlow()

        // Position state is isolated from [state] so the 250ms ticker only wakes
        // the timeline / time labels, not the gradient scrim + big transport row.
        private val _position = MutableStateFlow(PlayerPositionState())
        val position: StateFlow<PlayerPositionState> = _position.asStateFlow()

        val player: ExoPlayer by lazy {
            StashPlayerFactory(
                context = getApplication(),
                okHttpClient = okHttpClient,
                endpointProvider = endpointProvider,
            ).build().also { p ->
                p.addListener(playerListener)
                p.playWhenReady = true
            }
        }

        // Activity sync bookkeeping — tracks when the current scene started being
        // actively watched, and how much of it has been watched so far, so we can
        // write accurate playDuration/resume_time back to Stash.
        private var activeSceneId: String? = null
        private var watchStartedAtMs: Long = 0L
        private var accumulatedPlaySeconds: Double = 0.0
        private var completionReported: Boolean = false
        private var periodicSync: Job? = null

        private val playerListener =
            object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_ENDED) onSceneEnded()
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _state.update { it.copy(isPlaying = isPlaying) }
                    if (isPlaying) startWatchInterval() else flushWatchInterval()
                }

                override fun onPlayerError(error: PlaybackException) {
                    _state.update { it.copy(error = humanize(error), isPlaying = false) }
                }

                override fun onVideoSizeChanged(videoSize: androidx.media3.common.VideoSize) {
                    // Pull the freshly-reported frame rate off the player — VideoSize
                    // itself doesn't carry fps in Media3 1.9.
                    val fps =
                        player.videoFormat
                            ?.frameRate
                            ?.takeIf { it.isFinite() && it > 0f }
                    if (fps != null && _state.value.videoFrameRate != fps) {
                        _state.update { it.copy(videoFrameRate = fps) }
                    }
                }
            }

        /**
         * Translates raw Media3 error codes into user-intelligible messages. Most
         * importantly, detect "no decoder found" and point at the FFmpeg extension
         * rather than showing an opaque error code.
         */
        private fun humanize(error: PlaybackException): String {
            val codeName = error.errorCodeName
            val cause = (error as? ExoPlaybackException)?.cause?.message
            return when (error.errorCode) {
                PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
                PlaybackException.ERROR_CODE_DECODER_QUERY_FAILED,
                PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED,
                -> {
                    val base = "No decoder available for this file."
                    if (!CodecCapabilities.ffmpegExtensionUsable) {
                        "$base Install the FFmpeg extension (see README) for broader codec support."
                    } else {
                        "$base Codec may be unsupported even with FFmpeg."
                    }
                }
                PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
                PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
                ->
                    "Network error reaching Stash — check the server is reachable."
                PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS ->
                    "Server rejected the stream (HTTP error). API key might be invalid."
                else -> cause ?: "Playback error ($codeName)"
            }
        }

        private var positionTicker: Job? = null

        init {
            loadAndPlay(
                sceneId = queue.currentId() ?: startSceneId,
                startAtMs = if (explicitStartMs >= 0) explicitStartMs else null,
                autoResume = explicitStartMs < 0,
            )
            startPositionTicker()
        }

        private fun startPositionTicker() {
            if (positionTicker?.isActive == true) return
            positionTicker =
                viewModelScope.launch {
                    while (true) {
                        val p = player
                        _position.value =
                            PlayerPositionState(
                                positionMs = p.currentPosition.coerceAtLeast(0L),
                                durationMs = p.duration.takeIf { d -> d > 0 } ?: 0L,
                                bufferedMs = p.bufferedPosition.coerceAtLeast(0L),
                            )
                        // 250ms gives a smooth-looking progress bar without burning too much CPU
                        delay(250)
                    }
                }
        }

        /** Direct seek request from the UI (e.g. progress-bar drag end). */
        fun seekTo(positionMs: Long) {
            player.seekTo(positionMs.coerceAtLeast(0L))
        }

        /** Nudge — e.g. for double-tap seek; returns the applied delta for UI feedback. */
        fun seekBy(deltaMs: Long): Long {
            val p = player
            val before = p.currentPosition
            val target = (before + deltaMs).coerceAtLeast(0L)
            p.seekTo(target)
            return target - before
        }

        /** Cycle through the speed presets. Returns the new speed. */
        fun cyclePlaybackSpeed(): Float {
            val presets = PLAYBACK_SPEEDS
            val current = _state.value.playbackSpeed
            val idx =
                presets
                    .indexOfFirst { kotlin.math.abs(it - current) < 0.01f }
                    .takeIf { it >= 0 } ?: 2 // default anchor index for 1.0x
            val next = presets[(idx + 1) % presets.size]
            player.setPlaybackSpeed(next)
            _state.update {
                it.copy(
                    playbackSpeed = next,
                    banner = "Speed ${formatSpeed(next)}",
                )
            }
            clearBannerLater()
            return next
        }

        companion object {
            val PLAYBACK_SPEEDS = floatArrayOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)

            fun formatSpeed(speed: Float): String =
                when {
                    speed == speed.toInt().toFloat() -> "${speed.toInt()}x"
                    else -> "${"%.2f".format(speed).trimEnd('0').trimEnd('.')}x"
                }
        }

        private fun loadAndPlay(
            sceneId: String,
            startAtMs: Long? = null,
            autoResume: Boolean,
        ) {
            _state.update { it.copy(loading = true, error = null) }
            viewModelScope.launch {
                when (val result = sceneRepository.scene(sceneId)) {
                    is AppResult.Success -> {
                        val detail = result.data
                        _state.update {
                            it.copy(
                                loading = false,
                                current = detail,
                                queue = queue.snapshot(),
                                error = null,
                            )
                        }
                        playScene(detail, startAtMs, autoResume)
                    }
                    is AppResult.Failure ->
                        _state.update {
                            it.copy(loading = false, error = result.error.message)
                        }
                }
            }
        }

        private fun startWatchInterval() {
            if (watchStartedAtMs == 0L) watchStartedAtMs = System.currentTimeMillis()
            if (periodicSync?.isActive != true) {
                periodicSync =
                    viewModelScope.launch {
                        // Periodic flush so long viewing sessions don't risk losing state
                        // on force-kill. 30s cadence balances server load vs. data loss.
                        while (true) {
                            delay(30_000)
                            flushActivityToServer(final = false)
                        }
                    }
            }
        }

        /** Roll elapsed time since last interval start into the accumulator. */
        private fun flushWatchInterval() {
            if (watchStartedAtMs > 0L) {
                val elapsed = (System.currentTimeMillis() - watchStartedAtMs) / 1000.0
                accumulatedPlaySeconds += elapsed
                watchStartedAtMs = 0L
            }
        }

        private fun flushActivityToServer(final: Boolean) {
            val id = activeSceneId ?: return
            flushWatchInterval()
            val position = (player.currentPosition / 1000.0).coerceAtLeast(0.0)
            val duration = (player.duration / 1000.0).takeIf { it > 0 } ?: 0.0

            // If we watched at least 85% of the scene, record a completed play
            // exactly once per session to keep play_count accurate.
            val shouldCompletePlay =
                !completionReported &&
                    duration > 0 &&
                    position >= duration * 0.85
            if (shouldCompletePlay) completionReported = true

            viewModelScope.launch {
                sceneRepository.saveActivity(
                    sceneId = id,
                    resumeTimeSeconds = position,
                    playDurationSeconds = accumulatedPlaySeconds,
                )
                if (shouldCompletePlay) sceneRepository.addPlay(id)
            }

            if (final) {
                periodicSync?.cancel()
                periodicSync = null
                activeSceneId = null
                accumulatedPlaySeconds = 0.0
                completionReported = false
            }
        }

        private fun playScene(
            detail: SceneDetail,
            startAtMs: Long?,
            autoResume: Boolean,
        ) {
            // Flush any previous scene's activity before switching
            flushActivityToServer(final = true)
            activeSceneId = detail.summary.id
            accumulatedPlaySeconds = 0.0
            completionReported = false
            val item =
                MediaItem
                    .Builder()
                    .setUri(detail.summary.streamUrl)
                    .setMediaId(detail.summary.id)
                    .setMediaMetadata(
                        MediaMetadata
                            .Builder()
                            .setTitle(detail.summary.displayTitle)
                            .setArtist(detail.summary.studio?.name)
                            .setArtworkUri(detail.summary.screenshotUrl?.let { android.net.Uri.parse(it) })
                            .build(),
                    ).build()
            player.setMediaItem(item)
            player.prepare()

            // Priority: explicit start (marker tap) > resume_time (if autoResume)
            val seekTo =
                when {
                    startAtMs != null -> startAtMs
                    autoResume ->
                        detail.summary.resumeTimeSeconds
                            ?.let { (it * 1000).toLong() }
                            ?.takeIf { it > 2_000 }
                    else -> null
                }
            seekTo?.let { player.seekTo(it) }
            player.play()
        }

        private fun onSceneEnded() {
            val next = queue.advance()
            if (next == null) {
                // Queue exhausted with RepeatMode.OFF — emit a banner so the user knows
                // playback stopped intentionally rather than silently "hanging".
                _state.update { it.copy(banner = "End of queue") }
                clearBannerLater()
                return
            }
            loadAndPlay(next, autoResume = false)
        }

        fun skipNext() {
            val next = queue.advance() ?: return
            _state.update { it.copy(queue = queue.snapshot()) }
            loadAndPlay(next, autoResume = false)
        }

        fun skipPrevious() {
            val prev = queue.previous() ?: return
            _state.update { it.copy(queue = queue.snapshot()) }
            loadAndPlay(prev, autoResume = false)
        }

        fun toggleShuffle() {
            val newState = !(_state.value.queue?.shuffled ?: false)
            queue.setShuffled(newState)
            val snapshot = queue.snapshot()
            // "next up" after the current item in the (possibly freshly shuffled)
            // order — gives the user immediate confirmation that shuffle worked.
            val nextId = snapshot.items.getOrNull(snapshot.currentIndex + 1)
            _state.update {
                it.copy(
                    queue = snapshot,
                    banner =
                        if (newState) {
                            "Shuffle on" + (nextId?.let { id -> " · next: #$id" } ?: "")
                        } else {
                            "Shuffle off"
                        },
                )
            }
            clearBannerLater()
        }

        fun cycleRepeat() {
            val next =
                when (_state.value.queue?.repeatMode ?: RepeatMode.OFF) {
                    RepeatMode.OFF -> RepeatMode.ALL
                    RepeatMode.ALL -> RepeatMode.ONE
                    RepeatMode.ONE -> RepeatMode.OFF
                }
            queue.setRepeat(next)
            _state.update {
                it.copy(
                    queue = queue.snapshot(),
                    banner = "Repeat: ${next.name.lowercase()}",
                )
            }
            clearBannerLater()
        }

        private var bannerJob: Job? = null

        private fun clearBannerLater() {
            bannerJob?.cancel()
            bannerJob =
                viewModelScope.launch {
                    delay(2500)
                    _state.update { it.copy(banner = null) }
                }
        }

        /** Show a transient banner from outside the VM (e.g. UI-only toggles). */
        fun flashBanner(text: String) {
            _state.update { it.copy(banner = text) }
            clearBannerLater()
        }

        override fun onCleared() {
            positionTicker?.cancel()
            // Fire one last activity write so the server has our final position.
            flushActivityToServer(final = true)
            player.removeListener(playerListener)
            player.release()
            super.onCleared()
        }
    }
