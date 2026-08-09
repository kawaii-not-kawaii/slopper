package io.stashapp.android.feature.player

import android.content.Context
import android.os.Build
import android.os.PowerManager
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.media3.common.Format
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DecoderCounters
import androidx.media3.exoplayer.DecoderReuseEvaluation
import androidx.media3.exoplayer.ExoPlaybackException
import androidx.media3.exoplayer.analytics.AnalyticsListener

/**
 * Low-volume, machine-readable diagnostics for the playback A/B test.
 *
 * Filter with `adb logcat -s SlopperPerf:I`. No stream URLs or titles are
 * logged. All timestamps in event lines are elapsed realtime milliseconds so
 * they can be aligned with the ADB sampler without depending on wall-clock
 * synchronization.
 */
@androidx.annotation.OptIn(UnstableApi::class)
internal class PlaybackDiagnostics(
    context: Context,
    private val rendererMode: String,
) : AnalyticsListener {
    private val powerManager = context.getSystemService(PowerManager::class.java)
    private var transitionCount = 0
    private var droppedFrameTotal = 0
    private var videoDecoderInitCount = 0
    private var videoDecoderReleaseCount = 0
    private var audioDecoderInitCount = 0
    private var audioDecoderReleaseCount = 0
    private var thermalListener: PowerManager.OnThermalStatusChangedListener? = null

    init {
        log("session_start renderer_mode=$rendererMode sdk=${Build.VERSION.SDK_INT}")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            installThermalListener()
        } else {
            log("thermal_status value=unavailable reason=api_below_29")
        }
    }

    override fun onMediaItemTransition(
        eventTime: AnalyticsListener.EventTime,
        mediaItem: MediaItem?,
        reason: Int,
    ) {
        transitionCount += 1
        logEvent(
            eventTime,
            "media_transition count=$transitionCount reason=$reason media_id=${mediaItem?.mediaId.orEmpty()}",
        )
    }

    override fun onVideoInputFormatChanged(
        eventTime: AnalyticsListener.EventTime,
        format: Format,
        decoderReuseEvaluation: DecoderReuseEvaluation?,
    ) {
        logEvent(eventTime, "video_format ${format.videoDescription()}")
    }

    override fun onAudioInputFormatChanged(
        eventTime: AnalyticsListener.EventTime,
        format: Format,
        decoderReuseEvaluation: DecoderReuseEvaluation?,
    ) {
        logEvent(eventTime, "audio_format ${format.audioDescription()}")
    }

    override fun onVideoDecoderInitialized(
        eventTime: AnalyticsListener.EventTime,
        decoderName: String,
        initializedTimestampMs: Long,
        initializationDurationMs: Long,
    ) {
        videoDecoderInitCount += 1
        logEvent(
            eventTime,
            "video_decoder_init name=$decoderName renderer_mode=$rendererMode " +
                "duration_ms=$initializationDurationMs " +
                "init_count=$videoDecoderInitCount",
        )
    }

    override fun onVideoDecoderReleased(
        eventTime: AnalyticsListener.EventTime,
        decoderName: String,
    ) {
        videoDecoderReleaseCount += 1
        logEvent(
            eventTime,
            "video_decoder_release name=$decoderName release_count=$videoDecoderReleaseCount",
        )
    }

    override fun onAudioDecoderInitialized(
        eventTime: AnalyticsListener.EventTime,
        decoderName: String,
        initializedTimestampMs: Long,
        initializationDurationMs: Long,
    ) {
        audioDecoderInitCount += 1
        logEvent(
            eventTime,
            "audio_decoder_init name=$decoderName duration_ms=$initializationDurationMs " +
                "init_count=$audioDecoderInitCount",
        )
    }

    override fun onAudioDecoderReleased(
        eventTime: AnalyticsListener.EventTime,
        decoderName: String,
    ) {
        audioDecoderReleaseCount += 1
        logEvent(
            eventTime,
            "audio_decoder_release name=$decoderName release_count=$audioDecoderReleaseCount",
        )
    }

    override fun onDroppedVideoFrames(
        eventTime: AnalyticsListener.EventTime,
        droppedFrames: Int,
        elapsedMs: Long,
    ) {
        droppedFrameTotal += droppedFrames
        logEvent(
            eventTime,
            "dropped_video_frames count=$droppedFrames total=$droppedFrameTotal elapsed_ms=$elapsedMs",
        )
    }

    override fun onPlayerError(
        eventTime: AnalyticsListener.EventTime,
        error: PlaybackException,
    ) {
        val exoError = error as? ExoPlaybackException
        val format = exoError?.rendererFormat
        logEvent(
            eventTime,
            "player_error code=${error.errorCode} code_name=${error.errorCodeName.token()} " +
                "type=${exoError?.type ?: "unknown"} " +
                "renderer=${exoError?.rendererName.token()} " +
                "renderer_index=${exoError?.rendererIndex ?: "unknown"} " +
                "mime=${format?.sampleMimeType.token()} codecs=${format?.codecs.token()} " +
                "cause=${error.cause?.javaClass?.simpleName.token()}",
        )
    }

    override fun onVideoFrameProcessingOffset(
        eventTime: AnalyticsListener.EventTime,
        totalProcessingOffsetUs: Long,
        frameCount: Int,
    ) {
        val averageOffsetUs = if (frameCount > 0) totalProcessingOffsetUs / frameCount else 0
        logEvent(
            eventTime,
            "video_processing_offset average_us=$averageOffsetUs frames=$frameCount " +
                "total_us=$totalProcessingOffsetUs",
        )
    }

    override fun onVideoDisabled(
        eventTime: AnalyticsListener.EventTime,
        decoderCounters: DecoderCounters,
    ) {
        decoderCounters.ensureUpdated()
        logEvent(eventTime, "video_counters ${decoderCounters.description()}")
    }

    override fun onPlayerReleased(eventTime: AnalyticsListener.EventTime) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            removeThermalListener()
        }
        logEvent(
            eventTime,
            "session_end transitions=$transitionCount dropped_total=$droppedFrameTotal " +
                "video_decoder_inits=$videoDecoderInitCount " +
                "video_decoder_releases=$videoDecoderReleaseCount " +
                "audio_decoder_inits=$audioDecoderInitCount " +
                "audio_decoder_releases=$audioDecoderReleaseCount",
        )
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun installThermalListener() {
        logThermalStatus(powerManager.currentThermalStatus, "initial")
        val listener =
            PowerManager.OnThermalStatusChangedListener { status ->
                logThermalStatus(status, "callback")
            }
        thermalListener = listener
        powerManager.addThermalStatusListener(listener)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun removeThermalListener() {
        thermalListener?.let(powerManager::removeThermalStatusListener)
        thermalListener = null
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun logThermalStatus(
        status: Int,
        source: String,
    ) {
        val name =
            when (status) {
                PowerManager.THERMAL_STATUS_NONE -> "none"
                PowerManager.THERMAL_STATUS_LIGHT -> "light"
                PowerManager.THERMAL_STATUS_MODERATE -> "moderate"
                PowerManager.THERMAL_STATUS_SEVERE -> "severe"
                PowerManager.THERMAL_STATUS_CRITICAL -> "critical"
                PowerManager.THERMAL_STATUS_EMERGENCY -> "emergency"
                PowerManager.THERMAL_STATUS_SHUTDOWN -> "shutdown"
                else -> "unknown"
            }
        log("thermal_status value=$status name=$name source=$source")
    }

    private fun Format.videoDescription(): String =
        "mime=${sampleMimeType.orUnknown()} codecs=${codecs.orUnknown()} " +
            "width=${valueOrUnknown(width)} height=${valueOrUnknown(height)} " +
            "fps=${floatOrUnknown(frameRate)} bitrate=${valueOrUnknown(bitrate)} " +
            "color_transfer=${colorInfo?.colorTransfer ?: "unknown"} " +
            "luma_bits=${colorInfo?.lumaBitdepth ?: "unknown"}"

    private fun Format.audioDescription(): String =
        "mime=${sampleMimeType.orUnknown()} codecs=${codecs.orUnknown()} " +
            "channels=${valueOrUnknown(channelCount)} sample_rate=${valueOrUnknown(sampleRate)} " +
            "bitrate=${valueOrUnknown(bitrate)}"

    private fun DecoderCounters.description(): String =
        "rendered=$renderedOutputBufferCount dropped=$droppedBufferCount " +
            "max_consecutive_dropped=$maxConsecutiveDroppedBufferCount " +
            "dropped_to_keyframe=$droppedToKeyframeCount skipped=$skippedOutputBufferCount " +
            "decoder_inits=$decoderInitCount decoder_releases=$decoderReleaseCount " +
            "processing_offset_us=$totalVideoFrameProcessingOffsetUs " +
            "processing_offset_frames=$videoFrameProcessingOffsetCount"

    private fun valueOrUnknown(value: Int): String =
        if (value == Format.NO_VALUE) "unknown" else value.toString()

    private fun floatOrUnknown(value: Float): String =
        if (value == Format.NO_VALUE.toFloat() || !value.isFinite()) "unknown" else value.toString()

    private fun String?.orUnknown(): String = this?.replace(' ', '_') ?: "unknown"

    private fun String?.token(): String =
        this
            ?.replace(Regex("[^A-Za-z0-9._/-]"), "_")
            ?.take(120)
            ?: "unknown"

    private fun logEvent(
        eventTime: AnalyticsListener.EventTime,
        message: String,
    ) {
        log("event_realtime_ms=${eventTime.realtimeMs} position_ms=${eventTime.currentPlaybackPositionMs} $message")
    }

    private fun log(message: String) {
        Log.i(TAG, message)
    }

    private companion object {
        const val TAG = "SlopperPerf"
    }
}
