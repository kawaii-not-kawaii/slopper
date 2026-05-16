package io.stashapp.android.core.data.scene

import io.stashapp.android.core.model.Caption
import io.stashapp.android.core.model.Marker
import io.stashapp.android.core.model.PerformerRef
import io.stashapp.android.core.model.SceneDetail
import io.stashapp.android.core.model.SceneStream
import io.stashapp.android.core.model.SceneSummary
import io.stashapp.android.core.model.StudioRef
import io.stashapp.android.core.model.TagRef
import io.stashapp.android.core.network.StashEndpoint
import io.stashapp.android.graphql.FindSceneQuery
import io.stashapp.android.graphql.fragment.SceneCard

internal fun SceneCard.toSummary(endpoint: StashEndpoint): SceneSummary {
    val file = files.firstOrNull()
    return SceneSummary(
        id = id,
        title = title.orEmpty(),
        basename = file?.basename,
        details = details,
        date = date,
        rating100 = rating100,
        organized = organized,
        oCounter = o_counter ?: 0,
        playCount = play_count ?: 0,
        resumeTimeSeconds = resume_time,
        durationSeconds = file?.duration,
        width = file?.width,
        height = file?.height,
        videoCodec = file?.video_codec,
        audioCodec = file?.audio_codec,
        bitrate = file?.bit_rate,
        frameRate = file?.frame_rate,
        fileSize = file?.size?.toLongOrNull(),
        interactive = interactive,
        screenshotUrl = endpoint.resolve(paths.screenshot),
        previewUrl = endpoint.resolve(paths.preview),
        streamUrl = endpoint.resolve(paths.stream)
            ?: error("Scene $id missing stream URL"),
        spriteUrl = endpoint.resolve(paths.sprite),
        vttUrl = endpoint.resolve(paths.vtt),
        studio = studio?.let {
            StudioRef(
                id = it.id,
                name = it.name,
                imageUrl = endpoint.resolve(it.image_path),
            )
        },
        performers = performers.map { p ->
            PerformerRef(
                id = p.id,
                name = p.name,
                imageUrl = endpoint.resolve(p.image_path),
                gender = p.gender?.rawValue,
            )
        },
        tags = tags.map { TagRef(id = it.id, name = it.name) },
    )
}

internal fun FindSceneQuery.FindScene.toDetail(endpoint: StashEndpoint): SceneDetail {
    val summary = sceneCard.toSummary(endpoint)
    return SceneDetail(
        summary = summary,
        captions = captions.orEmpty().map { Caption(it.language_code, it.caption_type) },
        markers = scene_markers.map { m ->
            Marker(
                id = m.id,
                title = m.title,
                seconds = m.seconds,
                primaryTagName = m.primary_tag.name,
            )
        },
        streams = sceneStreams.map { s ->
            SceneStream(
                url = s.url,
                mimeType = s.mime_type,
                label = s.label,
            )
        },
    )
}
