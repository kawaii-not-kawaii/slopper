package io.stashapp.android.feature.player

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import io.github.anilbeesetti.nextlib.media3ext.ffdecoder.NextRenderersFactory
import okhttp3.OkHttpClient

/**
 * Builds the Stash ExoPlayer instance.
 *
 * Key decisions:
 *  - FFmpeg renderer ordering is selected at build time. Production defaults
 *    to `on`, which places FFmpeg after MediaCodec so software decoding is only
 *    used as a fallback. `prefer` remains available for controlled A/B tests.
 *  - [OkHttpDataSource] reuses our OkHttp client and adds the `ApiKey` header so
 *    the stream URL can be fetched from a private Stash server.
 *  - [DefaultTrackSelector] is configured to prefer HDR + highest bitrate by
 *    default; user can override via the settings sheet later.
 */
@OptIn(UnstableApi::class)
class StashPlayerFactory(
    private val context: Context,
    private val okHttpClient: OkHttpClient,
) {
    fun build(): ExoPlayer {
        val rendererMode = FfmpegRendererMode.fromBuildConfig(BuildConfig.FFMPEG_RENDERER_MODE)
        val trackSelector =
            DefaultTrackSelector(context).apply {
                setParameters(
                    buildUponParameters()
                        .setTunnelingEnabled(true)
                        .setAllowVideoMixedMimeTypeAdaptiveness(true),
                )
            }

        // [okHttpClient] already carries the origin-scoped ApiKey network interceptor
        // (see StashAuthInterceptor), which is re-evaluated on every redirect hop.
        val dataSourceFactory: DataSource.Factory =
            DataSource.Factory {
                val delegate = OkHttpDataSource.Factory(okHttpClient)
                DefaultDataSource.Factory(context, delegate).createDataSource()
            }

        // Media3's names are easy to misread: ON appends extension renderers
        // after the platform renderers, while PREFER inserts them before the
        // platform renderers. Keeping this mapping explicit makes the A/B test
        // auditable and prevents the comment from drifting from actual behavior.
        val renderersFactory =
            NextRenderersFactory(context)
                .setExtensionRendererMode(rendererMode.media3Value)
                .setEnableDecoderFallback(true)

        return ExoPlayer
            .Builder(context, renderersFactory)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .setTrackSelector(trackSelector)
            .setAudioAttributes(
                androidx.media3.common.AudioAttributes
                    .Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                // handleAudioFocus =
                true,
            ).setHandleAudioBecomingNoisy(true)
            // Frame-rate matching: ask Android to switch the display refresh
            // rate to match the video's fps whenever the transition would be
            // seamless (no visible black flash). This eliminates 3:2-pulldown
            // judder for 24/25 fps content on 60/120 Hz displays and makes
            // 60 fps sources play smoothly on VRR panels.
            .setVideoChangeFrameRateStrategy(C.VIDEO_CHANGE_FRAME_RATE_STRATEGY_ONLY_IF_SEAMLESS)
            // Media3 calls Surface.setFrameRate() under the hood with the
            // declared video fps, which cues compositor-side VRR scheduling.
            .build()
            .also { player ->
                if (BuildConfig.PLAYBACK_DIAGNOSTICS) {
                    player.addAnalyticsListener(
                        PlaybackDiagnostics(
                            context = context.applicationContext,
                            rendererMode = rendererMode.buildValue,
                        ),
                    )
                }
            }
    }
}

@OptIn(UnstableApi::class)
private enum class FfmpegRendererMode(
    val buildValue: String,
    val media3Value: Int,
) {
    ON("on", DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON),
    PREFER("prefer", DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER),
    ;

    companion object {
        fun fromBuildConfig(value: String): FfmpegRendererMode =
            entries.firstOrNull { it.buildValue == value }
                ?: error("Unsupported FFmpeg renderer mode: $value")
    }
}
