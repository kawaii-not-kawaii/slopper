plugins {
    alias(libs.plugins.stash.android.library)
    alias(libs.plugins.stash.android.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "io.stashapp.android.core.data"
}

dependencies {
    api(project(":core:domain"))
    api(project(":core:network"))
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.security.crypto)
    implementation(libs.androidx.paging.runtime)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
}
