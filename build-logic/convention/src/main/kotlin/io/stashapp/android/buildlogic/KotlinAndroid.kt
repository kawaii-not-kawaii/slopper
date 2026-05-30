package io.stashapp.android.buildlogic

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

internal fun Project.configureKotlinAndroid(
    commonExtension: CommonExtension,
) {
    commonExtension.apply {
        compileSdk = 35

        // AGP 9 removed the Action<T> lambda overloads from the CommonExtension
        // interface, so the block form (defaultConfig { } / compileOptions { } /
        // lint { }) no longer resolves on a statically-typed CommonExtension
        // reference. Use the property accessors directly (single-property set) and
        // .apply { } on the returned config objects.
        defaultConfig.minSdk = 26

        compileOptions.apply {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
            isCoreLibraryDesugaringEnabled = false
        }

        lint.apply {
            // Lint detectors shipped with old AndroidX libs crash with
            // IncompatibleClassChangeError under AGP 8.7.3 lint + Kotlin 2.2.20.
            // Re-enable when AndroidX Lifecycle/Compose are bumped (DEPS-07, deferred).
            disable.addAll(
                listOf(
                    "NullSafeMutableLiveData",      // lifecycle 2.8.7
                    "FrequentlyChangingValue",      // compose-runtime (Compose BOM 2026.05.00)
                    "RememberInComposition",        // compose-runtime (Compose BOM 2026.05.00)
                ),
            )
        }
    }

    extensions.configure<KotlinAndroidProjectExtension> {
        jvmToolchain(17)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
            freeCompilerArgs.addAll(
                "-opt-in=kotlin.RequiresOptIn",
                "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            )
        }
    }
}

internal fun Project.configureJava() {
    extensions.configure<JavaPluginExtension> {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
