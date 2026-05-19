package io.stashapp.android.core.model

/**
 * Domain model for a scene, independent of the GraphQL generated types.
 * This is what the UI consumes — the data layer maps Apollo types into this.
 */
data class SceneSummary(
    val id: String,
    val title: String,
    /** The scene's primary file name (e.g. "my_video.mp4"), used as a display
     *  fallback when [title] is blank. */
    val basename: String?,
    val details: String?,
    val date: String?,
    val rating100: Int?,
    val organized: Boolean,
    val oCounter: Int,
    val playCount: Int,
    val resumeTimeSeconds: Double?,
    val durationSeconds: Double?,
    val width: Int?,
    val height: Int?,
    val videoCodec: String?,
    val audioCodec: String?,
    val bitrate: Int?,
    val frameRate: Double?,
    val fileSize: Long?,
    val interactive: Boolean,
    val screenshotUrl: String?,
    val previewUrl: String?,
    val streamUrl: String,
    val spriteUrl: String?,
    val vttUrl: String?,
    val studio: StudioRef?,
    val performers: List<PerformerRef>,
    val tags: List<TagRef>,
) {
    /**
     * The best label to show the user when we need to reference this scene.
     * Prefers the curated title; falls back to file name without extension; last
     * resort is the raw scene id.
     */
    val displayTitle: String
        get() =
            title.takeIf { it.isNotBlank() }
                ?: basename?.substringBeforeLast('.')?.takeIf { it.isNotBlank() }
                ?: "Scene $id"
}

data class StudioRef(
    val id: String,
    val name: String,
    val imageUrl: String?,
)

data class PerformerRef(
    val id: String,
    val name: String,
    val imageUrl: String?,
    val gender: String?,
)

data class TagRef(
    val id: String,
    val name: String,
)

data class SceneStream(
    val url: String,
    val mimeType: String?,
    val label: String?,
)

data class SceneDetail(
    val summary: SceneSummary,
    val captions: List<Caption>,
    val markers: List<Marker>,
    val streams: List<SceneStream>,
)

data class Caption(
    val languageCode: String,
    val captionType: String,
)

data class Marker(
    val id: String,
    val title: String,
    val seconds: Double,
    val primaryTagName: String,
)

data class ScenesPage(
    val items: List<SceneSummary>,
    val totalCount: Int,
    val totalDurationSeconds: Double,
    val totalFileSizeBytes: Long,
)
