package io.stashapp.android.core.model

enum class RepeatMode { OFF, ALL, ONE }

data class QueueState(
    val items: List<String>,              // scene IDs in play order
    val currentIndex: Int,
    val shuffled: Boolean,
    val repeatMode: RepeatMode,
) {
    val currentId: String? get() = items.getOrNull(currentIndex)
    fun hasNext(): Boolean = when (repeatMode) {
        RepeatMode.OFF -> currentIndex < items.lastIndex
        RepeatMode.ALL, RepeatMode.ONE -> items.isNotEmpty()
    }
    fun hasPrevious(): Boolean = currentIndex > 0 || repeatMode == RepeatMode.ALL
}
