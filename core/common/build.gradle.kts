plugins {
    alias(libs.plugins.stash.android.library)
}

android {
    namespace = "io.stashapp.android.core.common"
}

dependencies {
    api(libs.kotlinx.coroutines.android)
    api("javax.inject:javax.inject:1")
}
