package io.stashapp.android.feature.player

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Guards the one piece of logic that tells us, on-device, whether
 * [StashPlayerFactory]'s `EXTENSION_RENDERER_MODE_ON` is still doing its job.
 * If a real Qualcomm/Exynos decoder ever starts reading "SW", the badge stops
 * being evidence of anything.
 */
class CodecCapabilitiesTest {
    @Test
    fun `vendor MediaCodec decoders are reported as hardware`() {
        assertEquals("HW", decoderBadge("c2.qti.avc.decoder"))
        assertEquals("HW", decoderBadge("c2.exynos.h264.decoder"))
        assertEquals("HW", decoderBadge("c2.mtk.hevc.decoder"))
        assertEquals("HW", decoderBadge("OMX.qcom.video.decoder.avc"))
    }

    @Test
    fun `Google bundled software decoders are reported as software`() {
        assertEquals("SW", decoderBadge("c2.android.avc.decoder"))
        assertEquals("SW", decoderBadge("OMX.google.h264.decoder"))
    }

    @Test
    fun `FFmpeg extension decoders are called out separately`() {
        assertEquals("SW·FF", decoderBadge("ffmpeg"))
        assertEquals("SW·FF", decoderBadge("FfmpegVideoDecoder"))
    }

    @Test
    fun `no decoder yet renders a placeholder rather than claiming hardware`() {
        assertEquals("…", decoderBadge(null))
        assertEquals("…", decoderBadge(""))
    }
}
