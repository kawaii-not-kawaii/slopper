package io.stashapp.android.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Generates the baseline profile that ships with the release APK. Runs the
 * flows users hit immediately after cold start so the compiler sees the hot
 * Compose paths (NavHost route transitions, `AnimatedVisibility`,
 * `AnimatedContent`, `LazyVerticalGrid` measure/layout).
 *
 * Trigger with:
 *   ./gradlew :app:generateBaselineProfile
 *
 * The output lands in
 *   baselineprofile/build/outputs/managed_device_android_test_additional_output/.../BaselineProfileResults/
 * and the `androidx.baselineprofile` plugin copies it to
 *   app/src/release/generated/baselineProfiles/baseline-prof.txt
 * which is packaged into the release APK automatically.
 *
 * NOTE: the journey below requires a connected Stash server configured on the
 * device running the test. If the initial Connection screen is shown (no
 * persisted server), we short-circuit — the profile will still capture cold
 * start + connection screen interaction, which is itself valuable.
 */
@RunWith(AndroidJUnit4::class)
class StashBaselineProfileGenerator {

    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generate() = rule.collect(packageName = TARGET_PACKAGE) {
        pressHome()
        startActivityAndWait()

        // Wait for first draw — content description or class probe is more
        // reliable than a specific string since strings may change.
        device.wait(Until.hasObject(By.pkg(TARGET_PACKAGE).depth(0)), 5_000)

        // If the library / home grid is present, scroll through a few rows so
        // LazyVerticalGrid + SceneCard composition + image loading get
        // compiled into the profile.
        val grid = device.wait(
            Until.findObject(By.scrollable(true)),
            3_000,
        )
        grid?.let {
            repeat(3) {
                it.scroll(androidx.test.uiautomator.Direction.DOWN, 0.8f)
                device.waitForIdle()
            }
        }

        // Tap the first scene to open detail, then back out — captures the
        // detail screen + back-nav composition.
        val firstCard = device.findObjects(By.clickable(true)).firstOrNull()
        firstCard?.click()
        device.waitForIdle()
        device.pressBack()
        device.waitForIdle()
    }

    private companion object {
        // Match whichever applicationId variant is installed — debug suffix
        // makes this tricky, so the caller should set the `target-package`
        // argument when invoking from CI. Default picks the release id.
        const val TARGET_PACKAGE = "io.stashapp.android"
    }
}
