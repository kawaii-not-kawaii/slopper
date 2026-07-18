package io.stashapp.android.core.domain

import kotlinx.coroutines.flow.Flow

interface UiSettings {
    val bottomNavVisibleIds: Flow<List<String>>
    val defaultSceneFilter: Flow<SceneFilter?>
    val imageCacheSizeMb: Flow<Int>
    val gridColumns: Flow<String>

    suspend fun setBottomNavVisibleIds(ids: List<String>)

    suspend fun setDefaultSceneFilter(filter: SceneFilter?)

    suspend fun setImageCacheSizeMb(value: Int)

    suspend fun setGridColumns(value: String)

    // --- Accent palette (D-05)
    val accentPalette: Flow<String>

    suspend fun setAccentPalette(name: String)
}
