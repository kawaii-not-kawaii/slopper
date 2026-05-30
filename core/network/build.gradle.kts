plugins {
    alias(libs.plugins.stash.android.library)
    alias(libs.plugins.stash.android.hilt)
    alias(libs.plugins.apollo)
}

android {
    namespace = "io.stashapp.android.core.network"
}

apollo {
    service("stash") {
        packageName.set("io.stashapp.android.graphql")
        // Stash's custom scalars map to String on the wire — just pass through.
        mapScalarToKotlinString("Time")
        mapScalarToKotlinString("Timestamp")
        mapScalarToKotlinString("Map")
        mapScalarToKotlinString("BoolMap")
        mapScalarToKotlinString("PluginConfigMap")
        mapScalarToKotlinString("Any")
        mapScalarToKotlinString("Int64")
        mapScalarToKotlinString("Upload")
    }
}

dependencies {
    api(libs.apollo.runtime)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(project(":core:common"))
}
