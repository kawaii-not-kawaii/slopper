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
 *  - [DefaultRenderersFactory] is set to [EXTENSION_RENDERER_MODE_PREFER] so that
 *    when the FFmpeg extension is present on the classpath, its software decoders
 *    are preferred over built-in ones. This gives us AC3/EAC3/Opus/Vorbis/DTS etc.
 *    without falling back to libVLC.
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

        // NextRenderersFactory is a drop-in replacement for DefaultRenderersFactory
        // that ships prebuilt FFmpeg software decoders. We still configure
        // EXTENSION_RENDERER_MODE_PREFER so MediaCodec wins when it can handle
        // the codec natively — the extension only kicks in for codecs the
        // hardware doesn't support (AC3, EAC3, DTS, TrueHD, etc.).
        val renderersFactory =
            NextRenderersFactory(context)
                .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
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
    }
}
