package io.stashapp.android.core.domain

import io.stashapp.android.core.model.ConnectionResult
import io.stashapp.android.core.model.StashServer
import kotlinx.coroutines.flow.Flow

interface ConnectionRepository {
    fun activeServer(): Flow<StashServer?>

    /** Probe the server, returning success + server info on reachable + authed. */
    suspend fun test(server: StashServer): ConnectionResult

    /** Persist as the active server. Caller should [test] first. */
    suspend fun setActive(server: StashServer)

    suspend fun disconnect()
}
