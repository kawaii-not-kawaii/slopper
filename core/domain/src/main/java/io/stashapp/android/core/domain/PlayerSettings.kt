package io.stashapp.android.core.domain

import kotlinx.coroutines.flow.Flow

interface PlayerSettings {
    val seekMsPerPx: Flow<Float>
    val doubleTapSeekSeconds: Flow<Int>

    suspend fun setSeekMsPerPx(value: Float)

    suspend fun setDoubleTapSeekSeconds(value: Int)

    companion object {
        const val DEFAULT_SEEK_MS_PER_PX = 120f
        const val DEFAULT_DOUBLE_TAP_SEEK_SEC = 10
    }
}
