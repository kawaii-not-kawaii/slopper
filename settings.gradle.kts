pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "StashAndroid"

include(":app")

// Core modules
include(":core:common")
include(":core:model")
include(":core:network")
include(":core:data")
include(":core:domain")
include(":core:designsystem")
include(":core:ui")

// Feature modules
include(":baselineprofile")
include(":feature:connection")
include(":feature:library")
include(":feature:browse")
include(":feature:home")
include(":feature:detail")
include(":feature:player")
include(":feature:settings")

// Macrobenchmark module used to generate baseline profiles for the :app.
// Not part of a normal build; invoked explicitly via:
//   ./gradlew :app:generateBaselineProfile
include(":baselineprofile")
