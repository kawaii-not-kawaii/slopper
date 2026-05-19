package io.stashapp.android.feature.player

import io.stashapp.android.core.model.QueueState
import io.stashapp.android.core.model.RepeatMode

/**
 * Pure queue state machine — no Android dependencies. Keeps shuffle reversible
 * by preserving the original order so toggling off restores the author's
 * intended sequence.
 */
class PlayerQueue private constructor(
    private val originalOrder: List<String>,
    private var shuffledOrder: List<String>,
    private var currentIndex: Int,
    private var shuffled: Boolean,
    private var repeatMode: RepeatMode,
) {
    private val activeOrder: List<String> get() = if (shuffled) shuffledOrder else originalOrder

    fun snapshot() =
        QueueState(
            items = activeOrder,
            currentIndex = currentIndex,
            shuffled = shuffled,
            repeatMode = repeatMode,
        )

    fun currentId(): String? = activeOrder.getOrNull(currentIndex)

    fun setRepeat(mode: RepeatMode) {
        repeatMode = mode
    }

    fun setShuffled(enabled: Boolean) {
        if (enabled == shuffled) return
        val pivot = currentId()
        shuffled = enabled
        if (enabled) {
            // Put current first, then shuffle the rest
            val rest = (originalOrder - setOfNotNull(pivot)).shuffled()
            shuffledOrder = listOfNotNull(pivot) + rest
            currentIndex = 0
        } else {
            currentIndex = originalOrder.indexOf(pivot).coerceAtLeast(0)
        }
    }

    /** Returns the id to play next, or null if queue ended. */
    fun advance(): String? {
        if (repeatMode == RepeatMode.ONE) return activeOrder.getOrNull(currentIndex)
        val nextIdx = currentIndex + 1
        if (nextIdx > activeOrder.lastIndex) {
            return if (repeatMode == RepeatMode.ALL) {
                currentIndex = 0
                activeOrder.firstOrNull()
            } else {
                null
            }
        }
        currentIndex = nextIdx
        return activeOrder[nextIdx]
    }

    fun previous(): String? {
        val prevIdx = currentIndex - 1
        if (prevIdx < 0) {
            return if (repeatMode == RepeatMode.ALL) {
                currentIndex = activeOrder.lastIndex
                activeOrder.lastOrNull()
            } else {
                null
            }
        }
        currentIndex = prevIdx
        return activeOrder[prevIdx]
    }

    /** Jumps to a specific id — no-op if not in queue. */
    fun jumpTo(id: String): String? {
        val idx = activeOrder.indexOf(id)
        if (idx < 0) return null
        currentIndex = idx
        return id
    }

    companion object {
        fun from(
            ids: List<String>,
            startIndex: Int,
        ): PlayerQueue {
            require(ids.isNotEmpty()) { "Queue cannot be empty" }
            val safeIdx = startIndex.coerceIn(0, ids.lastIndex)
            return PlayerQueue(
                originalOrder = ids,
                shuffledOrder = ids,
                currentIndex = safeIdx,
                shuffled = false,
                repeatMode = RepeatMode.OFF,
            )
        }
    }
}
