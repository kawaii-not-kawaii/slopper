package io.stashapp.android.core.ui.nav

/**
 * Central route registry. Keeping routes typed here avoids divergence between
 * feature modules that need to cross-navigate (e.g. library → player).
 */
object Routes {
    const val Connection = "connection"
    const val Settings = "settings"
    const val Home = "home"

    // library?preset=tag:42 (optional filter preset; see LibraryViewModel).
    // The bare "library" form must match the pattern because "preset" has a
    // defaultValue set on the composable's navArgument.
    const val LibraryPattern = "library?preset={preset}"
    const val Library = "library"

    fun libraryWithPreset(preset: String?): String = if (preset.isNullOrBlank()) Library else "library?preset=$preset"

    const val DetailPattern = "scene/{sceneId}"

    fun sceneDetail(sceneId: String) = "scene/$sceneId"

    // browse/performers | browse/studios | browse/tags
    const val BrowsePattern = "browse/{kind}"

    fun browse(kind: String) = "browse/$kind"

    // player/{sceneId}?queueIds=1,2,3&index=0&startMs=0
    const val PlayerPattern =
        "player/{sceneId}?queueIds={queueIds}&index={index}&startMs={startMs}"

    fun player(
        sceneId: String,
        queueIds: List<String> = emptyList(),
        index: Int = 0,
        startSeconds: Double? = null,
    ): String {
        val ids = queueIds.joinToString(",")
        // -1 sentinel means "no explicit start, use resume_time if any"
        val startMs = startSeconds?.let { (it * 1000).toLong() } ?: -1L
        return "player/$sceneId?queueIds=$ids&index=$index&startMs=$startMs"
    }
}
