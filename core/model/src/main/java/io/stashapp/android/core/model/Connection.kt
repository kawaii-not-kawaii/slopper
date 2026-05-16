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

sealed class ConnectionResult {
    data class Success(
        val info: ServerInfo,
    ) : ConnectionResult()

    data class InvalidUrl(
        val reason: String,
    ) : ConnectionResult()

    data class AuthFailed(
        val message: String,
    ) : ConnectionResult()

    data class NetworkError(
        val message: String,
    ) : ConnectionResult()

    data class ServerError(
        val message: String,
    ) : ConnectionResult()
}
