plugins {
    alias(libs.plugins.stash.android.feature)
}

android {
    namespace = "io.stashapp.android.feature.browse"
}

dependencies {
    implementation(libs.androidx.paging.compose)
    implementation(libs.androidx.paging.runtime)
}
