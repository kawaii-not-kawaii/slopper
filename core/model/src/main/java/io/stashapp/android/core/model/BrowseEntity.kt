package io.stashapp.android.core.model

data class PerformerBrowseItem(
    val id: String,
    val name: String,
    val imageUrl: String?,
    val gender: String?,
    val sceneCount: Int,
    val favorite: Boolean,
)

data class StudioBrowseItem(
    val id: String,
    val name: String,
    val imageUrl: String?,
    val sceneCount: Int,
)

data class TagBrowseItem(
    val id: String,
    val name: String,
    val imageUrl: String?,
    val sceneCount: Int,
)
