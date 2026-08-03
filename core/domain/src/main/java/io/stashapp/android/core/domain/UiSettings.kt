package io.stashapp.android.core.domain

import kotlinx.coroutines.flow.Flow

interface UiSettings {
    val bottomNavVisibleIds: Flow<List<String>>
    val defaultSceneFilter: Flow<SceneFilter?>
    val imageCacheSizeMb: Flow<Int>
    val gridColumns: Flow<String>
    val savedFilterPresets: Flow<List<SavedFilterPreset>>
    val recentSearches: Flow<List<String>>
    val blurThumbnails: Flow<Boolean>

    suspend fun setBottomNavVisibleIds(ids: List<String>)

    suspend fun setDefaultSceneFilter(filter: SceneFilter?)

    suspend fun setImageCacheSizeMb(value: Int)

    suspend fun setGridColumns(value: String)

    suspend fun saveFilterPreset(
        name: String,
        filter: SceneFilter,
        sort: SceneSort,
    ): String

    suspend fun deleteFilterPreset(id: String)

    suspend fun renameFilterPreset(
        id: String,
        name: String,
    )

    suspend fun addRecentSearch(query: String)

    suspend fun setBlurThumbnails(enabled: Boolean)

    // --- Accent palette (D-05)
    val accentPalette: Flow<String>

    suspend fun setAccentPalette(name: String)
}
