package io.stashapp.android.core.data.connection

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.exception.ApolloHttpException
import com.apollographql.apollo.exception.ApolloNetworkException
import io.stashapp.android.core.data.prefs.ConnectionStore
import io.stashapp.android.core.domain.ConnectionRepository
import io.stashapp.android.core.model.ConnectionResult
import io.stashapp.android.core.model.ServerInfo
import io.stashapp.android.core.model.StashServer
import io.stashapp.android.core.network.StashEndpoint
import io.stashapp.android.graphql.ServerInfoQuery
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultConnectionRepository
    @Inject
    constructor(
        private val store: ConnectionStore,
        private val endpointState: EndpointStateHolder,
        private val apollo: ApolloClient,
    ) : ConnectionRepository {
        private val active = MutableStateFlow(store.currentServer())

        override fun activeServer(): Flow<StashServer?> = active.asStateFlow()

        override suspend fun test(server: StashServer): ConnectionResult {
            val url =
                normalizeUrl(server.baseUrl)
                    ?: return ConnectionResult.InvalidUrl("Enter a URL like http://192.168.1.10:9999")

            val priorEndpoint = endpointState.current()
            endpointState.set(StashEndpoint(url, server.apiKey))

            return try {
                val response = apollo.query(ServerInfoQuery()).execute()
                if (response.hasErrors()) {
                    endpointState.set(priorEndpoint)
                    val message = response.errors?.joinToString { it.message } ?: "Unknown GraphQL error"
                    ConnectionResult.ServerError(message)
                } else {
                    val data = response.data
                    if (data == null) {
                        endpointState.set(priorEndpoint)
                        ConnectionResult.ServerError("Empty response from server")
                    } else {
                        ConnectionResult.Success(
                            ServerInfo(
                                version = data.version.version ?: "unknown",
                                buildTime = data.version.build_time,
                                sceneCount = data.stats.scene_count,
                                performerCount = data.stats.performer_count,
                                studioCount = data.stats.studio_count,
                                tagCount = data.stats.tag_count,
                            ),
                        )
                    }
                }
            } catch (e: ApolloHttpException) {
                endpointState.set(priorEndpoint)
                when (e.statusCode) {
                    401, 403 -> ConnectionResult.AuthFailed("API key rejected (HTTP ${e.statusCode})")
                    else -> ConnectionResult.ServerError("HTTP ${e.statusCode}: ${e.message}")
                }
            } catch (e: ApolloNetworkException) {
                endpointState.set(priorEndpoint)
                ConnectionResult.NetworkError(e.message ?: "Could not reach server")
            } catch (e: Throwable) {
                endpointState.set(priorEndpoint)
                ConnectionResult.NetworkError(e.message ?: "Unexpected error")
            }
        }

        override suspend fun setActive(server: StashServer) {
            val url = normalizeUrl(server.baseUrl) ?: server.baseUrl
            val normalized = server.copy(baseUrl = url)
            store.save(normalized)
            endpointState.set(StashEndpoint(normalized.baseUrl, normalized.apiKey))
            active.value = normalized
        }

        override suspend fun disconnect() {
            store.clear()
            endpointState.set(null)
            active.value = null
        }

        private fun normalizeUrl(raw: String): String? {
            val trimmed = raw.trim().trimEnd('/')
            if (trimmed.isEmpty()) return null
            // Default missing scheme to https:// — users who need plaintext can type
            // it explicitly. Defaulting to http silently put LAN deployments on
            // cleartext without any signal (security M4).
            val withScheme =
                when {
                    trimmed.startsWith("http://") || trimmed.startsWith("https://") -> trimmed
                    else -> "https://$trimmed"
                }
            return runCatching { java.net.URI(withScheme) }
                .getOrNull()
                ?.takeIf { !it.host.isNullOrBlank() }
                ?.let { withScheme }
        }
    }
