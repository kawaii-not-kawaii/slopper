package io.stashapp.android.core.data.connection

import io.stashapp.android.core.data.prefs.ConnectionStore
import io.stashapp.android.core.network.StashEndpoint
import io.stashapp.android.core.network.StashEndpointProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns the current Stash endpoint. Split out of [DefaultConnectionRepository]
 * so the Apollo client can depend on the endpoint without pulling in the
 * repository (which itself depends on the Apollo client → cycle).
 *
 * Only the repository should mutate this; everyone else reads.
 */
@Singleton
class EndpointStateHolder @Inject constructor(
    private val store: ConnectionStore,
) : StashEndpointProvider {

    private val state = MutableStateFlow(
        store.currentServer()?.let { StashEndpoint(it.baseUrl, it.apiKey) },
    )

    val stateFlow = state.asStateFlow()

    override fun current(): StashEndpoint? = state.value

    override fun observe(): Flow<StashEndpoint?> = state.asStateFlow()

    /** Called by the repository to point at a different server. */
    internal fun set(endpoint: StashEndpoint?) {
        state.value = endpoint
    }

    fun serverFlow() = store.observe().map {
        it?.let { s -> StashEndpoint(s.baseUrl, s.apiKey) }
    }
}
