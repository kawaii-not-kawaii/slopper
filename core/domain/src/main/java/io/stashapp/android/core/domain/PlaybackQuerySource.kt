package io.stashapp.android.core.domain

/**
 * The query the user is currently browsing, published so the player can reconstruct it.
 *
 * The player is launched with a list of scene ids, which is only ever the page the
 * library had loaded. Shuffle needs the *whole* filtered set, and the filter is far too
 * large to serialise into a navigation route — so the library publishes its query here
 * and the player reads it when shuffle is switched on.
 *
 * Best-effort by design: if nothing was published (deep link, process death, playback
 * started from a rail rather than the library) the player falls back to the ids it was
 * given. Never let a missing query break playback.
 */
interface PlaybackQuerySource {
    fun current(): SceneQuery?

    fun set(query: SceneQuery?)
}
