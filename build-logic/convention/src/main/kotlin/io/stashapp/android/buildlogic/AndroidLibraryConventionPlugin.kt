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
            }
            extensions.configure<LibraryExtension> {
                configureKotlinAndroid(this)
                defaultConfig.targetSdk = 35
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
