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

    // --- Accent palette (D-05)
    val accentPalette: Flow<String>

    suspend fun setAccentPalette(name: String)

    // --- Display (SETTINGS-08)
    val reduceMotion: Flow<Boolean>
    val cardDensity: Flow<String>
    val longPressBehavior: Flow<String>
    val showResumeBar: Flow<Boolean>
    val showStudioCaption: Flow<Boolean>
    val showChapterStrip: Flow<Boolean>
    val tapToPeekInfo: Flow<Boolean>

    // --- Library (SETTINGS-09)
    val syncRatings: Flow<Boolean>
    val syncOCounter: Flow<Boolean>
    val syncMarkers: Flow<Boolean>
    val cacheDuration: Flow<String>
    val keepWatchHistory: Flow<Boolean>
    val historyOnHome: Flow<Boolean>
    val smartRails: Flow<Boolean>

    suspend fun setReduceMotion(v: Boolean)

    suspend fun setCardDensity(v: String)

    suspend fun setLongPressBehavior(v: String)

    suspend fun setShowResumeBar(v: Boolean)

    suspend fun setShowStudioCaption(v: Boolean)

    suspend fun setShowChapterStrip(v: Boolean)

    suspend fun setTapToPeekInfo(v: Boolean)

    suspend fun setSyncRatings(v: Boolean)

    suspend fun setSyncOCounter(v: Boolean)

    suspend fun setSyncMarkers(v: Boolean)

    suspend fun setCacheDuration(v: String)

    suspend fun setKeepWatchHistory(v: Boolean)

    suspend fun setHistoryOnHome(v: Boolean)

    suspend fun setSmartRails(v: Boolean)
}
