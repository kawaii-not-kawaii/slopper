plugins {
    alias(libs.plugins.stash.android.feature)
}

android {
    namespace = "io.stashapp.android.feature.home"
}

dependencies {
    implementation(libs.kotlinx.collections.immutable)
}
