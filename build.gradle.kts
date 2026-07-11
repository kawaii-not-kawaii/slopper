// Top-level build file
buildscript {
    // Phase 10 CI-SIGNING probe: AGP 9's apksigner throws an EdEC
    // NoClassDefFoundError on some CI runners during signing validation.
    // Providing BouncyCastle on the build classpath supplies the missing
    // Ed25519 provider. Opt-in via CI_SIGNING_BCPROV so the normal compile
    // gate and local builds are completely unaffected (empty deps otherwise).
    if (System.getenv("CI_SIGNING_BCPROV") != null) {
        repositories {
            mavenCentral()
            google()
        }
        dependencies {
            classpath("org.bouncycastle:bcprov-jdk18on:1.78.1")
        }
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
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
    apply(plugin = "dev.detekt")

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

    configure<dev.detekt.gradle.extensions.DetektExtension> {
        // Keep in sync with gradle/libs.versions.toml :: detekt
        toolVersion = "2.0.0-alpha.5"
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
