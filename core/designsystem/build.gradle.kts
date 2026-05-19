plugins {
    alias(libs.plugins.stash.android.library)
    alias(libs.plugins.stash.android.compose)
}

android {
    namespace = "io.stashapp.android.core.designsystem"
}

dependencies {
    api(libs.androidx.core.ktx)
    api(libs.coil.compose)
    api(libs.coil.network.okhttp)
    implementation(libs.androidx.compose.ui.text.google.fonts)
}
