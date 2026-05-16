package io.stashapp.android.core.network

/**
 * A resolved Stash server endpoint used to construct GraphQL + media URLs.
 */
data class StashEndpoint(
    val baseUrl: String,
    val apiKey: String?,
) {
    val graphqlUrl: String get() = baseUrl.trimEnd('/') + "/graphql"

    /**
     * Rewrite server-provided paths into absolute URLs, rejecting cross-origin
     * absolute URLs. A relative path (e.g. `/scene/1/screenshot`) is joined to
     * [baseUrl]; an absolute URL is returned only if its origin matches our
     * endpoint. A compromised or malicious Stash server could otherwise return
     * `https://evil.example/scene.mp4` in `paths.stream`, and our OkHttp-level
     * origin check on the API key wouldn't help if the request itself points
     * at the attacker.
     */
    fun resolve(path: String?): String? {
        if (path.isNullOrBlank()) return null
        if (!path.startsWith("http://") && !path.startsWith("https://")) {
            return baseUrl.trimEnd('/') + "/" + path.trimStart('/')
        }
        // Absolute URL — only accept if it's same-origin with the server.
        return if (sameOrigin(path, baseUrl)) path else null
    }

    private fun sameOrigin(
        absoluteUrl: String,
        baseUrl: String,
    ): Boolean {
        val a = runCatching { java.net.URI(absoluteUrl) }.getOrNull() ?: return false
        val b = runCatching { java.net.URI(baseUrl) }.getOrNull() ?: return false
        return a.scheme.equals(b.scheme, ignoreCase = true) &&
            a.host.equals(b.host, ignoreCase = true) &&
            normalizedPort(a) == normalizedPort(b)
    }

    private fun normalizedPort(uri: java.net.URI): Int {
        if (uri.port != -1) return uri.port
        return when (uri.scheme?.lowercase()) {
            "http" -> 80
            "https" -> 443
            else -> -1
        }
    }
}
