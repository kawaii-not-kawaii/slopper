import org.gradle.api.GradleException

plugins {
    alias(libs.plugins.stash.android.feature)
}

val ffmpegRendererMode =
    providers
        .gradleProperty("slopper.ffmpegRendererMode")
        .orElse("on")
        .map { it.lowercase() }
        .get()
        .also { mode ->
            if (mode !in setOf("on", "prefer")) {
                throw GradleException(
                    "slopper.ffmpegRendererMode must be 'on' or 'prefer' (was '$mode')",
                )
            }
        }

val playbackDiagnostics =
    providers
        .gradleProperty("slopper.playbackDiagnostics")
        .orElse("false")
        .map { value ->
            value.toBooleanStrictOrNull()
                ?: throw GradleException(
                    "slopper.playbackDiagnostics must be 'true' or 'false' (was '$value')",
                )
        }.get()

android {
    namespace = "io.stashapp.android.feature.player"

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        // Production uses platform MediaCodec decoders first and retains
        // FFmpeg as a fallback. The playback A/B tooling can still build both
        // renderer orders explicitly with diagnostics enabled.
        buildConfigField("String", "FFMPEG_RENDERER_MODE", "\"$ffmpegRendererMode\"")
        buildConfigField("boolean", "PLAYBACK_DIAGNOSTICS", playbackDiagnostics.toString())
    }
}

dependencies {
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.exoplayer.hls)
    implementation(libs.androidx.media3.exoplayer.dash)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.media3.session)
    implementation(libs.androidx.media3.datasource.okhttp)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.collections.immutable)

    // Prebuilt FFmpeg extension for Media3 — audio codecs (AC3/EAC3/DTS/TrueHD
    // etc.) plus H.264/HEVC/VP8/VP9 software decode fallback. Ships as a
    // Maven artifact so we don't need to maintain an NDK build.
    // https://github.com/anilbeesetti/nextlib
    implementation(libs.nextlib.media3ext)

    // Legacy escape hatch: if you build your own `media3-decoder-ffmpeg*.aar`
    // (e.g. via tools/ffmpeg-extension/build.sh) and drop it in libs/, we'll
    // pick it up too. Useful for custom decoder sets.
    val ffmpegAars = fileTree("libs") { include("media3-decoder-ffmpeg*.aar") }
    if (!ffmpegAars.isEmpty) implementation(ffmpegAars)
}
