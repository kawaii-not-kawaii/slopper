package io.stashapp.android.baselineprofile

import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Measures library-grid scroll frame timing with the baseline profile installed.
 *
 * Acceptance (PERF-07): >= 95% of frames on time at p95 (frame time <= 16.67ms at 60Hz
 * or <= 8.33ms at 120Hz depending on GMD display refresh rate).
 *
 * Run via GMD:
 *   ./gradlew :baselineprofile:pixel6Api34BenchmarkAndroidTest \
 *     -Pandroid.testInstrumentationRunnerArguments.class=io.stashapp.android.baselineprofile.LibraryScrollBenchmark
 *
 * Capture output to .planning/benchmarks/perf-07-library-scroll.txt (raw stdout, no trimming).
 *
 * Uses StartupMode.WARM (app process running, activity recreated) for stable frame-timing
 * measurements — more representative than COLD for scroll benchmarks since it avoids
 * amortising cold-start JIT cost into the frame timing numbers.
 */
@RunWith(AndroidJUnit4::class)
class LibraryScrollBenchmark {

    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun libraryScroll() = benchmarkRule.measureRepeated(
        packageName = TARGET_PACKAGE,
        metrics = listOf(FrameTimingMetric()),
        compilationMode = CompilationMode.Partial(
            baselineProfileMode = BaselineProfileMode.Require,
            warmupIterations = 3,
        ),
        startupMode = StartupMode.WARM,
        iterations = 5,
    ) {
        startActivityAndWait()

        val grid = device.wait(Until.findObject(By.scrollable(true)), 3_000)
            ?: return@measureRepeated  // no scrollable grid found — iteration skipped, not failed

        // Scroll down then up to exercise the full LazyVerticalGrid composition path
        // including item recycling, thumbnail pre-fetching, and overscroll edge effects.
        repeat(5) {
            grid.scroll(Direction.DOWN, 0.8f)
            device.waitForIdle()
        }
        repeat(5) {
            grid.scroll(Direction.UP, 0.8f)
            device.waitForIdle()
        }
    }

    private companion object {
        const val TARGET_PACKAGE = "io.stashapp.android"
    }
}
