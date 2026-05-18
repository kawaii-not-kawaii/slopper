package io.stashapp.android.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
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

    /**
     * Journey 1: Cold start + library grid scroll + first scene tap.
     * Covers NavHost routing, LazyVerticalGrid composition, SceneCard image loading.
     */
    @Test
    fun generate() =
        rule.collect(packageName = TARGET_PACKAGE) {
            pressHome()
            startActivityAndWait()

            // Wait for first draw — content description or class probe is more
            // reliable than a specific string since strings may change.
            device.wait(Until.hasObject(By.pkg(TARGET_PACKAGE).depth(0)), 5_000)

            // If the library / home grid is present, scroll through a few rows so
            // LazyVerticalGrid + SceneCard composition + image loading get
            // compiled into the profile.
            val grid =
                device.wait(
                    Until.findObject(By.scrollable(true)),
                    3_000,
                )
            grid?.let { uiObj ->
                repeat(3) {
                    uiObj.scroll(Direction.DOWN, 0.8f)
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

    /**
     * Journey 2: Home rails scroll.
     * Covers horizontal LazyRow rail composition, thumbnail pre-fetch, rail navigation.
     */
    @Test
    fun homeRailsScroll() =
        rule.collect(packageName = TARGET_PACKAGE) {
            pressHome()
            startActivityAndWait()
            device.wait(Until.hasObject(By.pkg(TARGET_PACKAGE).depth(0)), 5_000)

            // Navigate to Home tab — try resource-id first, fall back to description
            val homeTab =
                device.wait(Until.findObject(By.res(TARGET_PACKAGE, "nav_home")), 2_000)
                    ?: device.wait(Until.findObject(By.desc("Home")), 2_000)
            homeTab?.click()
            device.waitForIdle()

            // Scroll each visible horizontal rail (LazyRow rendered as scrollable container)
            val rails = device.findObjects(By.scrollable(true))
            rails.take(4).forEach { rail ->
                rail.scroll(Direction.RIGHT, 0.8f)
                device.waitForIdle()
                rail.scroll(Direction.LEFT, 0.5f)
                device.waitForIdle()
            }
        }

    /**
     * Journey 3: Detail screen open.
     * Covers DetailScreen composition, image hero load, metadata layout.
     */
    @Test
    fun detailOpen() =
        rule.collect(packageName = TARGET_PACKAGE) {
            pressHome()
            startActivityAndWait()

            // Short-circuit if no scrollable grid appears (no server connection)
            val hasGrid = device.wait(Until.hasObject(By.scrollable(true)), 3_000)
            if (!hasGrid) return@collect
            device.waitForIdle()

            // Tap first clickable item that looks like a card view
            val firstCard =
                device
                    .findObjects(By.clickable(true))
                    .firstOrNull { it.className?.contains("View") == true }
            firstCard?.click()
            device.wait(Until.hasObject(By.pkg(TARGET_PACKAGE).depth(0)), 3_000)
            device.waitForIdle()

            device.pressBack()
            device.waitForIdle()
        }

    /**
     * Journey 4: Player first-frame.
     * Covers ExoPlayer surface composition, video decoder cold path, player UI overlay.
     */
    @Test
    fun playerFirstFrame() =
        rule.collect(packageName = TARGET_PACKAGE) {
            pressHome()
            startActivityAndWait()

            val hasGrid = device.wait(Until.hasObject(By.scrollable(true)), 3_000)
            if (!hasGrid) return@collect

            // Open a scene detail
            val firstCard = device.findObjects(By.clickable(true)).firstOrNull()
            firstCard?.click()
            device.wait(Until.hasObject(By.pkg(TARGET_PACKAGE).depth(0)), 3_000)
            device.waitForIdle()

            // Tap Play — try resource-id first, fall back to content description
            val playButton =
                device.wait(Until.findObject(By.res(TARGET_PACKAGE, "btn_play")), 2_000)
                    ?: device.wait(Until.findObject(By.descContains("Play").clickable(true)), 2_000)
            playButton?.click()

            // Wait for player surface — probes ExoPlayer + Surface composition path
            device.wait(Until.hasObject(By.pkg(TARGET_PACKAGE).depth(0)), 8_000)
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
