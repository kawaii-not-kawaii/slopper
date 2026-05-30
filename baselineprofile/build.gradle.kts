plugins {
    alias(libs.plugins.android.test)
    alias(libs.plugins.androidx.baselineprofile)
}

android {
    namespace = "io.stashapp.android.baselineprofile"
    compileSdk = 36

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    defaultConfig {
        minSdk = 28 // baseline profiles are no-op below P
        targetSdk = 35
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Macrobenchmark runs against an installed APK, so declare the target.
    targetProjectPath = ":app"

    experimentalProperties["android.experimental.self-instrumenting"] = true

    testOptions {
        managedDevices {
            localDevices {
                create("pixel6Api34") {
                    device = "Pixel 6"
                    apiLevel = 34
                    systemImageSource = "google_apis"
                }
            }
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

// Gradle Managed Device (PERF-01): replaced useConnectedDevices = true with GMD declaration.
// The pixel6Api34 device is declared above; CI can spin up the emulator automatically.
// Fallback: if google_apis system image is unavailable, see REVIEWS-C4 ACCEPT in 03-CONTEXT.md.
baselineProfile {
    managedDevices += "pixel6Api34"
    useConnectedDevices = false
}

androidComponents {
    onVariants { variant ->
        // Macrobenchmark variants are `benchmark` and `nonMinified` — both
        // need the test runner registered.
        variant.instrumentationRunnerArguments.put(
            "androidx.benchmark.suppressErrors",
            "EMULATOR,DEBUGGABLE,UNLOCKED",
        )
    }
}

dependencies {
    implementation(libs.androidx.benchmark.macro.junit4)
    implementation(libs.androidx.test.ext.junit)
    implementation(libs.androidx.test.uiautomator)
    implementation(libs.junit)
}
