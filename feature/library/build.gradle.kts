plugins {
    alias(libs.plugins.stash.android.feature)
}

android {
    namespace = "io.stashapp.android.feature.library"
}

dependencies {
    implementation(libs.androidx.paging.compose)
    implementation(libs.androidx.paging.runtime)
}
