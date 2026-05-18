plugins {
    alias(libs.plugins.stash.android.feature)
}

android {
    namespace = "io.stashapp.android.feature.player"
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

    // Player prefs (seek sensitivity, etc.) live in :core:data via DataStore.
    implementation(project(":core:data"))

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
