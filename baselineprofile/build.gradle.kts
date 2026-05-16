plugins {
    alias(libs.plugins.android.test)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.androidx.baselineprofile)
}

android {
    namespace = "io.stashapp.android.baselineprofile"
    compileSdk = 35

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    defaultConfig {
        minSdk = 28                 // baseline profiles are no-op below P
        targetSdk = 35
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Macrobenchmark runs against an installed APK, so declare the target.
    targetProjectPath = ":app"

    experimentalProperties["android.experimental.self-instrumenting"] = true
}

// Use whatever device is plugged in via adb when generation is requested.
// Declaring a gradle-managed-device (GMD) here would let CI spin up an
// emulator automatically — punted for now; profile generation is infrequent
// and a real 120Hz phone gives the most representative trace.
baselineProfile {
    useConnectedDevices = true
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
