package io.stashapp.android.core.model

enum class RepeatMode { OFF, ALL, ONE }

data class QueueState(
    val items: List<String>, // source queue; random playback history is not exposed
    val currentIndex: Int,
    val shuffled: Boolean,
    val repeatMode: RepeatMode,
    val hasPreviousInHistory: Boolean = false,
    val hasShuffleAlternative: Boolean = items.size > 1,
) {
    val currentId: String? get() = items.getOrNull(currentIndex)

    fun hasNext(): Boolean =
        when {
            repeatMode == RepeatMode.ONE -> items.isNotEmpty()
            shuffled -> hasShuffleAlternative
            repeatMode == RepeatMode.ALL -> items.isNotEmpty()
            else -> currentIndex < items.lastIndex
        }

    fun hasPrevious(): Boolean =
        if (shuffled) {
            hasPreviousInHistory
        } else {
            currentIndex > 0 || repeatMode == RepeatMode.ALL
        }
}
