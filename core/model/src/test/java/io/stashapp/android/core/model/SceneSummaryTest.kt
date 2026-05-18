package io.stashapp.android.core.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class SceneSummaryTest {
    private fun minimalScene(
        id: String = "1",
        title: String = "Test Scene",
    ) = SceneSummary(
        id = id,
        title = title,
        basename = "test.mp4",
        details = null,
        date = null,
        rating100 = null,
        organized = false,
        oCounter = 0,
        playCount = 0,
        resumeTimeSeconds = null,
        durationSeconds = null,
        width = null,
        height = null,
        videoCodec = null,
        audioCodec = null,
        bitrate = null,
        frameRate = null,
        fileSize = null,
        interactive = false,
        screenshotUrl = null,
        previewUrl = null,
        streamUrl = "http://example.com/stream/1",
        spriteUrl = null,
        vttUrl = null,
        studio = null,
        performers = emptyList(),
        tags = emptyList(),
    )

    @Test
    fun `SceneSummary constructs with required fields`() {
        val scene = minimalScene()
        assertNotNull(scene.id)
        assertEquals("1", scene.id)
    }

    @Test
    fun `displayTitle prefers title when non-blank`() {
        val scene = minimalScene(title = "My Scene")
        assertEquals("My Scene", scene.displayTitle)
    }

    @Test
    fun `displayTitle falls back to basename without extension`() {
        val scene = minimalScene(title = "").copy(basename = "video_file.mp4")
        assertEquals("video_file", scene.displayTitle)
    }

    @Test
    fun `displayTitle falls back to scene id when both title and basename blank`() {
        val scene = minimalScene(id = "42", title = "").copy(basename = null)
        assertEquals("Scene 42", scene.displayTitle)
    }
}
