plugins {
    alias(libs.plugins.stash.android.library)
    alias(libs.plugins.stash.android.hilt)
}

android {
    namespace = "io.stashapp.android.core.domain"
}

dependencies {
    api(project(":core:model"))
    api(project(":core:common"))
    api(libs.androidx.paging.runtime)
}
