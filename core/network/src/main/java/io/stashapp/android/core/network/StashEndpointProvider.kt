package io.stashapp.android.core.network

import kotlinx.coroutines.flow.Flow

/**
 * Abstraction so `:core:network` can read the active endpoint without depending
 * on `:core:data`. The data layer provides the real implementation.
 */
interface StashEndpointProvider {
    /** Current active endpoint; `null` if user has not connected yet. */
    fun current(): StashEndpoint?

    /** Observes endpoint changes. Emits `null` when disconnected. */
    fun observe(): Flow<StashEndpoint?>
}
