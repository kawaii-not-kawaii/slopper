// Top-level build file
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.apollo) apply false
    alias(libs.plugins.android.test) apply false
    alias(libs.plugins.androidx.baselineprofile) apply false
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
    alias(libs.plugins.dependency.check)
}

// OWASP dependencyCheck — fails on HIGH+ findings when run in CI.
// Local: `./gradlew dependencyCheckAnalyze --no-configuration-cache`
// (plugin is incompatible with Gradle configuration cache as of 11.x).
dependencyCheck {
    formats = listOf("HTML", "JSON")
    failBuildOnCVSS = 7.0f // HIGH+ only — fail on CRITICAL / HIGH
    suppressionFile = "$rootDir/config/owasp-suppressions.xml"
    // Analyzers we don't need — speeds up the scan significantly.
    analyzers.apply {
        assemblyEnabled = false
        nuspecEnabled = false
        nodeAuditEnabled = false
        retirejs.enabled = false
    }
}

// Apply ktlint + detekt to every subproject automatically — avoids each
// feature module opting in by hand.
subprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
    apply(plugin = "io.gitlab.arturbosch.detekt")

    configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
        version.set("1.6.0")
        android.set(true)
        ignoreFailures.set(false)
        // Exclude generated Apollo sources + build output
        filter {
            exclude { it.file.path.contains("/build/") }
            exclude("**/generated/**")
        }
    }

    configure<io.gitlab.arturbosch.detekt.extensions.DetektExtension> {
        // Keep in sync with gradle/libs.versions.toml :: detekt
        toolVersion = "1.23.8"
        config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
        buildUponDefaultConfig = true
        ignoreFailures = false
        source.setFrom("src/main/java", "src/main/kotlin")
        // Per-module baseline: accepts pre-existing findings (mostly long
        // @Composable functions and preference setters). Regenerate after large
        // refactors via `./gradlew detektBaseline`. New findings beyond the
        // baseline still fail the build.
        baseline = file("detekt-baseline.xml")
    }
}
