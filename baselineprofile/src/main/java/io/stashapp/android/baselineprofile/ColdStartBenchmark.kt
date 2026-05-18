package io.stashapp.android.baselineprofile

import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Measures cold-start time with and without the baseline profile.
 *
 * Acceptance (PERF-06): p50 WITH_PROFILE / WITHOUT_PROFILE ratio >= 1.05 (5% improvement).
 *
 * Run via GMD:
 *   ./gradlew :baselineprofile:pixel6Api34BenchmarkAndroidTest \
 *     -Pandroid.testInstrumentationRunnerArguments.class=io.stashapp.android.baselineprofile.ColdStartBenchmark
 *
 * Capture output to .planning/benchmarks/perf-06-cold-start.txt (raw stdout, no trimming).
 *
 * IMPORTANT: startupWithProfile uses CompilationMode.Partial(BaselineProfileMode.Require) which
 * FAILS at runtime (not skips) if the installed APK contains no baseline profile. Run
 * :app:generateBaselineProfile (PERF-05, plan-3.1) before executing this benchmark.
 */
@RunWith(AndroidJUnit4::class)
class ColdStartBenchmark {
    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    /**
     * WITH baseline profile — measures the startup improvement from profile-guided AOT
     * compilation. Uses BaselineProfileMode.Require which will abort the benchmark (not
     * skip it) if the installed APK has no profile embedded.
     */
    @Test
    fun startupWithProfile() =
        benchmarkRule.measureRepeated(
            packageName = TARGET_PACKAGE,
            metrics = listOf(StartupTimingMetric()),
            compilationMode =
                CompilationMode.Partial(
                    baselineProfileMode = BaselineProfileMode.Require,
                    warmupIterations = 3,
                ),
            startupMode = StartupMode.COLD,
            iterations = 5,
        ) {
            pressHome()
            startActivityAndWait()
            // Wait for first meaningful frame so startup timing captures the full cold-start path
            device.wait(Until.hasObject(By.pkg(TARGET_PACKAGE).depth(0)), 5_000)
        }

    /**
     * WITHOUT any profile — JIT-only baseline representing a fresh install with no
     * pre-compilation. Compare p50 of this result against startupWithProfile to derive
     * the profile benefit ratio.
     */
    @Test
    fun startupWithoutProfile() =
        benchmarkRule.measureRepeated(
            packageName = TARGET_PACKAGE,
            metrics = listOf(StartupTimingMetric()),
            compilationMode = CompilationMode.None(),
            startupMode = StartupMode.COLD,
            iterations = 5,
        ) {
            pressHome()
            startActivityAndWait()
            device.wait(Until.hasObject(By.pkg(TARGET_PACKAGE).depth(0)), 5_000)
        }

    private companion object {
        // Match the applicationId used in app/build.gradle.kts (no debug suffix for
        // benchmarks — run against the release variant).
        const val TARGET_PACKAGE = "io.stashapp.android"
    }
}
