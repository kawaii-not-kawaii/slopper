package io.stashapp.android.feature.player

/**
 * Runtime detection of the Media3 FFmpeg decoder extension.
 *
 * The extension AAR ships a single `FfmpegLibrary` class — its presence on the
 * classpath means [StashPlayerFactory]'s `EXTENSION_RENDERER_MODE_PREFER` will
 * actually find software decoders for AC3/EAC3/DTS/Opus/TrueHD/etc.
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
