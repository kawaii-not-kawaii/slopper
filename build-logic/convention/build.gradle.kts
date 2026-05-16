plugins {
    `kotlin-dsl`
}

group = "io.stashapp.android.buildlogic"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    compileOnly(libs.plugin.android)
    compileOnly(libs.plugin.kotlin)
    compileOnly(libs.plugin.compose)
    compileOnly(libs.plugin.ksp)
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "stash.android.application"
            implementationClass = "io.stashapp.android.buildlogic.AndroidApplicationConventionPlugin"
        }
        register("androidLibrary") {
            id = "stash.android.library"
            implementationClass = "io.stashapp.android.buildlogic.AndroidLibraryConventionPlugin"
        }
        register("androidFeature") {
            id = "stash.android.feature"
            implementationClass = "io.stashapp.android.buildlogic.AndroidFeatureConventionPlugin"
        }
        register("androidCompose") {
            id = "stash.android.compose"
            implementationClass = "io.stashapp.android.buildlogic.AndroidComposeConventionPlugin"
        }
        register("androidHilt") {
            id = "stash.android.hilt"
            implementationClass = "io.stashapp.android.buildlogic.AndroidHiltConventionPlugin"
        }
    }
}
