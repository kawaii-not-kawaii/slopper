plugins {
    alias(libs.plugins.stash.android.feature)
}

android {
    namespace = "io.stashapp.android.feature.settings"
}

dependencies {
    // Settings surfaces player / codec capability — depend on the player feature
    // for CodecCapabilities only. If this grows we should factor a shared
    // :core:player-capabilities module.
    implementation(project(":feature:player"))
    // PlayerPreferences read/write
    implementation(project(":core:data"))
}
