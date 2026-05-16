plugins {
    alias(libs.plugins.stash.android.library)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "io.stashapp.android.core.model"
}

dependencies {
    api(libs.kotlinx.serialization.json)
}
