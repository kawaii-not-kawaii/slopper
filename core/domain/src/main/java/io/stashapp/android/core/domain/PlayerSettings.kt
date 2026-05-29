package io.stashapp.android.core.domain

import kotlinx.coroutines.flow.Flow

interface PlayerSettings {
    val seekMsPerPx: Flow<Float>
    val doubleTapSeekSeconds: Flow<Int>
    val defaultPlaybackSpeed: Flow<Float>
    val autoPlayNext: Flow<Boolean>
    val resumeThresholdSeconds: Flow<Int>
    val completionThresholdPercent: Flow<Int>
    val skipIntroSeconds: Flow<Int>
    val videoBufferPreset: Flow<String>
    val defaultAspectRatio: Flow<String>
    val decoderPreference: Flow<String>

    suspend fun setSeekMsPerPx(value: Float)

    suspend fun setDoubleTapSeekSeconds(value: Int)

    suspend fun setDefaultPlaybackSpeed(value: Float)

    suspend fun setAutoPlayNext(value: Boolean)

    suspend fun setResumeThresholdSeconds(value: Int)

    suspend fun setCompletionThresholdPercent(value: Int)

    suspend fun setSkipIntroSeconds(value: Int)

    suspend fun setVideoBufferPreset(value: String)

    suspend fun setDefaultAspectRatio(value: String)

    suspend fun setDecoderPreference(value: String)

    // --- Player chrome (SETTINGS-06)
    val showChapterThumbnails: Flow<Boolean>
    val lockControlsOnIdle: Flow<Boolean>
    val showCodecBadge: Flow<Boolean>
    val showQueuePosition: Flow<Boolean>
    val hapticsOnSeek: Flow<Boolean>

    // --- Codecs / HDR (SETTINGS-07)
    val hdrPassthrough: Flow<Boolean>
    val matchRefreshRate: Flow<Boolean>
    val matchResolution: Flow<Boolean>
    val fallbackOnDecoderError: Flow<Boolean>
    val tunneling: Flow<Boolean>
    val preBufferOnHover: Flow<Boolean>

    suspend fun setShowChapterThumbnails(v: Boolean)

    suspend fun setLockControlsOnIdle(v: Boolean)

    suspend fun setShowCodecBadge(v: Boolean)

    suspend fun setShowQueuePosition(v: Boolean)

    suspend fun setHapticsOnSeek(v: Boolean)

    suspend fun setHdrPassthrough(v: Boolean)

    suspend fun setMatchRefreshRate(v: Boolean)

    suspend fun setMatchResolution(v: Boolean)

    suspend fun setFallbackOnDecoderError(v: Boolean)

    suspend fun setTunneling(v: Boolean)

    suspend fun setPreBufferOnHover(v: Boolean)

    companion object {
        const val DEFAULT_SEEK_MS_PER_PX = 120f
        const val DEFAULT_DOUBLE_TAP_SEEK_SEC = 10
        const val DEFAULT_SPEED = 1.0f
    }
}
