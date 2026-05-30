package io.stashapp.android.buildlogic

import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.android.library")
                apply("org.jetbrains.kotlin.android")
            }
            extensions.configure<LibraryExtension> {
                configureKotlinAndroid(this)
                // AGP 9 removed `targetSdk` from the library DSL (LibraryBaseFlavor) —
                // libraries have no runtime targetSdk; it is resolved from the consuming
                // app at manifest merge. The silent-opt-in guard (D-05a) only concerns
                // app + test modules, which still set targetSdk = 35 explicitly.
                defaultConfig.consumerProguardFiles("consumer-rules.pro")
                testOptions {
                    unitTests {
                        all { it.useJUnitPlatform() }
                    }
                }
            }
            dependencies {
                add("testImplementation", libs.findLibrary("junit5-api").get())
                add("testImplementation", libs.findLibrary("junit5-params").get())
                add("testRuntimeOnly",    libs.findLibrary("junit5-engine").get())
                add("testRuntimeOnly",    libs.findLibrary("junit5-platform-launcher").get())
                add("testImplementation", libs.findLibrary("mockk").get())
                add("testImplementation", libs.findLibrary("turbine").get())
                add("testImplementation", libs.findLibrary("robolectric").get())
            }
        }
    }
}
