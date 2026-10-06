package io.stashapp.android.core.network

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Attaches the `ApiKey` header only to requests whose URL shares the configured
 * Stash origin.
 *
 * Must be registered with `addNetworkInterceptor`, not `addInterceptor`: application
 * interceptors run once, before redirects are followed, and OkHttp only strips the
 * `Authorization` header (not custom ones) on a cross-origin redirect. A network
 * interceptor sees every hop, so a redirect to another origin gets no key, and any
 * key already on the request is removed.
 */
class StashAuthInterceptor(
    private val endpointProvider: StashEndpointProvider,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val endpoint = endpointProvider.current()
        val request = chain.request()
        val builder = request.newBuilder().removeHeader(HEADER)
        val apiKey = endpoint?.apiKey?.takeIf { it.isNotBlank() }
        if (apiKey != null && request.url.matchesOrigin(endpoint.baseUrl)) {
            builder.header(HEADER, apiKey)
        }
        return chain.proceed(builder.build())
    }

    private fun HttpUrl.matchesOrigin(baseUrl: String): Boolean {
        val base = baseUrl.toHttpUrlOrNull() ?: return false
        return scheme.equals(base.scheme, ignoreCase = true) &&
            host.equals(base.host, ignoreCase = true) &&
            port == base.port
    }

    private companion object {
        const val HEADER = "ApiKey"
    }
}
