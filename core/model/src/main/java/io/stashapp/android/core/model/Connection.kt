package io.stashapp.android.core.model

data class StashServer(
    val baseUrl: String,
    val apiKey: String?,
    val displayName: String,
)

data class ServerInfo(
    val version: String,
    val buildTime: String?,
    val sceneCount: Int,
    val performerCount: Int,
    val studioCount: Int,
    val tagCount: Int,
)
