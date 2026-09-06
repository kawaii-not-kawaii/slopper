package io.stashapp.android.feature.player

/**
 * Runtime detection of the Media3 FFmpeg decoder extension.
 *
 * The extension AAR ships a single `FfmpegLibrary` class — its presence on the
 * classpath means [StashPlayerFactory] can use software decoders as fallbacks
 * for AC3/EAC3/DTS/Opus/TrueHD/etc.
 *
 * We use reflection instead of a compile-time import so the app compiles and
 * runs with or without the extension present — which is exactly the contract
 * `feature/player/build.gradle.kts` expresses.
 */
object CodecCapabilities {
    /**
     * Classes to probe, in priority order:
     *  1. nextlib's repackaged FfmpegLibrary — what we ship with now.
     *  2. Upstream Media3 FfmpegLibrary — catches custom AARs built via our
     *     `tools/ffmpeg-extension/build.sh` escape hatch.
     */
    private val FFMPEG_CLASSES =
        listOf(
            "io.github.anilbeesetti.nextlib.media3ext.ffdecoder.FfmpegLibrary",
            "androidx.media3.decoder.ffmpeg.FfmpegLibrary",
        )

    private val ffmpegClass: Class<*>? by lazy {
        FFMPEG_CLASSES.firstNotNullOfOrNull {
            runCatching { Class.forName(it) }.getOrNull()
        }
    }

    /**
     * Best-effort check that the native libs actually loaded. The class can
     * exist on the classpath while JNI load fails (e.g. wrong ABI split), so
     * we call `isAvailable()` via reflection.
     */
    val ffmpegExtensionUsable: Boolean by lazy {
        val clazz = ffmpegClass ?: return@lazy false
        runCatching {
            val method = clazz.getMethod("isAvailable")
            method.invoke(null) as Boolean
        }.getOrDefault(false)
    }
}

/**
 * Classifies the video decoder Media3 actually chose as hardware or software.
 *
 * The decoder name from `AnalyticsListener.onVideoDecoderInitialized` is the only
 * signal available, but Android's naming convention makes it a reliable one:
 * Google's bundled *software* MediaCodec decoders are prefixed `c2.android.`
 * (Codec2) or `OMX.google.` (legacy), and the FFmpeg extension decoders carry
 * "ffmpeg" in their name. Everything else is a vendor decoder — `c2.qti.*`,
 * `OMX.qcom.*`, `c2.exynos.*`, `c2.mtk.*` — which is hardware-backed.
 *
 * Getting "HW" on a plain H.264 file is the on-device proof that the production
 * FFmpeg renderer ordering (`slopper.ffmpegRendererMode=on`) is in effect;
 * "SW·FF" there means either a `prefer` A/B build or a regression. Unlike
 * [PlaybackDiagnostics] this works in release builds with no adb attached, which
 * is why the badge is worth keeping alongside the logcat diagnostics.
 *
 * @param decoderName name reported by Media3, or null before the first video
 *   decoder has been initialised.
 */
fun decoderBadge(decoderName: String?): String =
    when {
        decoderName.isNullOrBlank() -> "…"
        decoderName.contains("ffmpeg", ignoreCase = true) -> "SW·FF"
        decoderName.startsWith("c2.android.", ignoreCase = true) -> "SW"
        decoderName.startsWith("OMX.google.", ignoreCase = true) -> "SW"
        else -> "HW"
    }
