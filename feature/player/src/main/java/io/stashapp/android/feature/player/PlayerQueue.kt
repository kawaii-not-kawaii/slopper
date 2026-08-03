package io.stashapp.android.feature.player

import io.stashapp.android.core.model.QueueState
import io.stashapp.android.core.model.RepeatMode
import kotlin.random.Random

/**
 * Pure queue state machine — no Android dependencies. Keeps shuffle reversible
 * by preserving the original order so toggling off restores the author's
 * intended sequence.
 */
class PlayerQueue private constructor(
    private var originalOrder: List<String>,
    private var currentIndex: Int,
    private var shuffled: Boolean,
    private var repeatMode: RepeatMode,
    private val random: Random,
) {
    private val distinctItemCount get() = originalOrder.toSet().size
    private val history = mutableListOf(currentIndex)
    private var historyIndex = 0

    /**
     * Indices not yet played in the current shuffle cycle, in the order they will be
     * drawn. Drawing without replacement is what stops a scene reappearing before the
     * rest of the queue has been seen — picking uniformly at random each time would
     * only avoid the immediate repeat.
     */
    private val bag = mutableListOf<Int>()

    private fun refillBag(excluding: Int?) {
        bag.clear()
        originalOrder.indices.filterTo(bag) { it != excluding }
        bag.shuffle(random)
    }

    fun snapshot() =
        QueueState(
            items = originalOrder,
            currentIndex = currentIndex,
            shuffled = shuffled,
            repeatMode = repeatMode,
            hasPreviousInHistory = shuffled && historyIndex > 0,
            hasShuffleAlternative = distinctItemCount > 1,
        )

    fun currentId(): String? = originalOrder.getOrNull(currentIndex)

    /**
     * Swap in a wider pool — the whole filtered library rather than the page the
     * library had loaded — keeping the scene that is playing.
     *
     * Playback history is dropped because its indices refer to the old list. Ignored
     * if [ids] doesn't contain the current scene, so a stale or mismatched fetch can
     * never strand the player on the wrong video.
     */
    fun replacePool(ids: List<String>) {
        val playing = currentId() ?: return
        val index = ids.indexOf(playing)
        if (index < 0) return
        originalOrder = ids
        currentIndex = index
        history.clear()
        history += index
        historyIndex = 0
        if (shuffled) refillBag(excluding = index)
    }

    fun setRepeat(mode: RepeatMode) {
        repeatMode = mode
    }

    fun setShuffled(enabled: Boolean) {
        if (enabled == shuffled) return
        shuffled = enabled
        history.clear()
        history += currentIndex
        historyIndex = 0
        if (enabled) refillBag(excluding = currentIndex) else bag.clear()
    }

    /** Returns the id to play next, or null if the ordered queue ended. */
    fun advance(): String? {
        if (repeatMode == RepeatMode.ONE) return currentId()
        if (shuffled) return advanceRandomly()

        val nextIndex = currentIndex + 1
        if (nextIndex > originalOrder.lastIndex) {
            return if (repeatMode == RepeatMode.ALL) {
                currentIndex = 0
                currentId()
            } else {
                null
            }
        }
        currentIndex = nextIndex
        return currentId()
    }

    private fun advanceRandomly(): String? {
        if (distinctItemCount < 2) return null

        // Every scene has played — start a fresh cycle. Shuffle loops on its own;
        // Repeat governs ordered playback only.
        if (bag.isEmpty()) refillBag(excluding = currentIndex)
        val nextIndex = bag.removeAt(bag.lastIndex)

        while (history.lastIndex > historyIndex) history.removeAt(history.lastIndex)
        history += nextIndex
        historyIndex += 1
        currentIndex = nextIndex
        return currentId()
    }

    fun previous(): String? {
        if (shuffled) {
            if (historyIndex == 0) return null
            historyIndex -= 1
            currentIndex = history[historyIndex]
            return currentId()
        }

        val previousIndex = currentIndex - 1
        if (previousIndex < 0) {
            return if (repeatMode == RepeatMode.ALL) {
                currentIndex = originalOrder.lastIndex
                currentId()
            } else {
                null
            }
        }
        currentIndex = previousIndex
        return currentId()
    }

    companion object {
        fun from(
            ids: List<String>,
            startIndex: Int,
            random: Random = Random.Default,
        ): PlayerQueue {
            require(ids.isNotEmpty()) { "Queue cannot be empty" }
            return PlayerQueue(
                originalOrder = ids,
                currentIndex = startIndex.coerceIn(0, ids.lastIndex),
                shuffled = false,
                repeatMode = RepeatMode.OFF,
                random = random,
            )
        }
    }
}
