package io.stashapp.android.core.ui.image

import android.content.Context
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import dagger.hilt.android.qualifiers.ApplicationContext
import io.stashapp.android.core.data.prefs.UiPreferences
import io.stashapp.android.core.network.StashEndpointProvider
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okio.Path.Companion.toOkioPath
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StashImageLoaderFactory
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val endpointProvider: StashEndpointProvider,
        private val uiPreferences: UiPreferences,
    ) : SingletonImageLoader.Factory {
        override fun newImageLoader(context: PlatformContext): ImageLoader {
            val authClient =
                OkHttpClient
                    .Builder()
                    .addInterceptor(StashAuthImageInterceptor(endpointProvider))
                    .build()

            return ImageLoader
                .Builder(context)
                .crossfade(150)
                .memoryCache {
                    MemoryCache
                        .Builder()
                        .maxSizePercent(context, 0.25)
                        .build()
                }.diskCache {
                    // Use a reasonable default (256MB). The disk cache is lazily
                    // initialized on first use (background thread), avoiding
                    // runBlocking on the main thread during cold start.
                    val defaultCacheMb = 256
                    DiskCache
                        .Builder()
                        .directory(
                            this.context.cacheDir
                                .resolve("image_cache")
                                .toOkioPath(),
                        ).maxSizeBytes(defaultCacheMb.toLong() * 1024 * 1024)
                        .build()
                }.components {
                    add(OkHttpNetworkFetcherFactory(callFactory = { authClient }))
                }.build()
        }
    }

private class StashAuthImageInterceptor(
    private val endpointProvider: StashEndpointProvider,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val endpoint = endpointProvider.current()
        val request = chain.request()
        val apiKey = endpoint?.apiKey?.takeIf { it.isNotBlank() }

        val finalRequest =
            if (apiKey != null &&
                endpoint != null &&
                request.url.matchesOrigin(endpoint.baseUrl)
            ) {
                request.newBuilder().addHeader("ApiKey", apiKey).build()
            } else {
                request
            }

        return chain.proceed(finalRequest)
    }

    private fun okhttp3.HttpUrl.matchesOrigin(baseUrl: String): Boolean {
        val base = baseUrl.toHttpUrlOrNull() ?: return false
        return scheme.equals(base.scheme, ignoreCase = true) &&
            host.equals(base.host, ignoreCase = true) &&
            port == base.port
    }
}
