package io.stashapp.android.core.domain

import kotlinx.coroutines.flow.Flow

interface UiSettings {
    val bottomNavVisibleIds: Flow<List<String>>
    val defaultSceneFilter: Flow<SceneFilter?>
    val imageCacheSizeMb: Flow<Int>
    val gridColumns: Flow<String>
    val amoledBlackMode: Flow<Boolean>
    val showRatingOnCards: Flow<Boolean>
    val showPlayCountOnCards: Flow<Boolean>
    val showResolutionOnCards: Flow<Boolean>
    val activityTracking: Flow<Boolean>
    val autoRotatePlayer: Flow<Boolean>

    suspend fun setBottomNavVisibleIds(ids: List<String>)

    suspend fun setDefaultSceneFilter(filter: SceneFilter?)

    suspend fun setImageCacheSizeMb(value: Int)

    suspend fun setGridColumns(value: String)

    suspend fun setAmoledBlackMode(value: Boolean)

    suspend fun setShowRatingOnCards(value: Boolean)

    suspend fun setShowPlayCountOnCards(value: Boolean)

    suspend fun setShowResolutionOnCards(value: Boolean)

    suspend fun setActivityTracking(value: Boolean)

    suspend fun setAutoRotatePlayer(value: Boolean)
}
