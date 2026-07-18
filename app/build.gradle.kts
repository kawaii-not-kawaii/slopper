import java.util.Properties

plugins {
    alias(libs.plugins.stash.android.application)
    alias(libs.plugins.stash.android.compose)
    alias(libs.plugins.stash.android.hilt)
    alias(libs.plugins.androidx.baselineprofile)
}

android {
    namespace = "io.stashapp.android"

    defaultConfig {
        applicationId = "io.stashapp.android"
        // SemVer: 0.x = pre-release; 1.0 will be the first public/stable release.
        // Bump versionCode on every APK push; versionName on milestones.
        versionCode = 5
        versionName = "0.2.0-alpha"
        vectorDrawables.useSupportLibrary = true
    }

    // COMPLY-06 (per D-06): AGP scans values-*/ directories at build time and
    // generates app/build/intermediates/generated_res/.../locale_config.xml,
    // auto-merging <application android:localeConfig="@xml/locale_config" />
    // into the final manifest. No source-tree xml/locale_config.xml needed.
    // (Pitfall E6 warns against manually adding android:localeConfig to the
    // source manifest — AGP's merger handles it.)
    androidResources {
        generateLocaleConfig = true
    }

    signingConfigs {
        // Release signing config reads credentials from either environment
        // variables (CI pattern) or a `keystore.properties` file at the repo
        // root (local release-build pattern). If neither is present, the
        // release build type falls back to the debug keystore — acceptable
        // for personal sideloads but NOT for Play Store distribution.
        val keystoreProps: Properties? =
            rootProject
                .file("keystore.properties")
                .takeIf { it.exists() }
                ?.let { file -> Properties().apply { file.inputStream().use { load(it) } } }

        fun prop(
            name: String,
            env: String,
        ): String? = keystoreProps?.getProperty(name) ?: System.getenv(env)

        val hasReleaseKeystore = prop("storeFile", "STASH_KEYSTORE_FILE") != null
        if (hasReleaseKeystore) {
            create("release") {
                storeFile = file(prop("storeFile", "STASH_KEYSTORE_FILE")!!)
                storePassword = prop("storePassword", "STASH_KEYSTORE_PASSWORD")
                keyAlias = prop("keyAlias", "STASH_KEY_ALIAS")
                keyPassword = prop("keyPassword", "STASH_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            isMinifyEnabled = false
            isDebuggable = true
        }
        // Benchmark variant — non-debuggable + minified, mirrors release, but
        // with `profileable` on so macrobenchmark can drive it.
        create("benchmark") {
            initWith(getByName("release"))
            signingConfig = signingConfigs.getByName("debug")
            isMinifyEnabled = true
            isDebuggable = false
            matchingFallbacks += listOf("release")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
            // JNI strip: smaller APK, strips debug symbols from .so files.
            ndk.debugSymbolLevel = "NONE"
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            // Prefer the release signing config if it was created above,
            // otherwise fall back to debug so `assembleRelease` still works
            // end-to-end on a fresh clone for smoke testing.
            signingConfig = signingConfigs.findByName("release")
                ?: signingConfigs.getByName("debug")
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    lint {
        // Treat lint errors as build failures but don't let every cosmetic
        // warning hold up a debug build. CI overrides to `warningsAsErrors`.
        abortOnError = true
        warningsAsErrors = false
        checkReleaseBuilds = true
        // Baseline of pre-existing issues; fail the build only on new ones.
        baseline = file("lint-baseline.xml")
        disable +=
            setOf(
                // Compose stability warnings tend to be noisy on LazyList item content
                "MutableCollectionMutableState",
            )
    }

    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "armeabi-v7a")
            isUniversalApk = false
        }
    }
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:model"))
    implementation(project(":core:domain"))
    implementation(project(":core:network"))
    implementation(project(":core:data"))
    implementation(project(":core:ui"))
    implementation(project(":core:designsystem"))

    implementation(project(":feature:connection"))
    implementation(project(":feature:library"))
    implementation(project(":feature:browse"))
    implementation(project(":feature:home"))
    implementation(project(":feature:detail"))
    implementation(project(":feature:player"))
    implementation(project(":feature:settings"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)

    // ProfileInstaller ships the compiled baseline profile at install time so
    // ART can use it before the app has been launched even once.
    implementation(libs.androidx.profileinstaller)

    // Producer of the baseline-prof.txt — macrobenchmark module.
    "baselineProfile"(project(":baselineprofile"))
}
