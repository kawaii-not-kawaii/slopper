package io.stashapp.android.core.data.scene

import io.stashapp.android.core.domain.PlaybackQuerySource
import io.stashapp.android.core.domain.SceneQuery
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory holder for the query the library is currently showing.
 *
 * Deliberately not persisted: it is a hint for the current session only, and a stale
 * value from a previous run would be worse than none.
 */
@Singleton
class PlaybackQueryHolder
    @Inject
    constructor() : PlaybackQuerySource {
        @Volatile
        private var query: SceneQuery? = null

        override fun current(): SceneQuery? = query

        override fun set(query: SceneQuery?) {
            this.query = query
        }
    }
