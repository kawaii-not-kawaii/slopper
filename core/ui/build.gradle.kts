plugins {
    alias(libs.plugins.stash.android.library)
    alias(libs.plugins.stash.android.compose)
    alias(libs.plugins.stash.android.hilt)
}

android {
    namespace = "io.stashapp.android.core.ui"
}

dependencies {
    api(project(":core:designsystem"))
    api(project(":core:model"))
    api(project(":core:domain"))
    api(project(":core:network"))
    api(libs.coil.compose)
    api(libs.coil.network.okhttp)
    api(libs.okhttp)
}
