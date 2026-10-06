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
import io.stashapp.android.core.domain.UiSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okio.Path.Companion.toOkioPath
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StashImageLoaderFactory
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val okHttpClient: OkHttpClient,
        private val uiSettings: UiSettings,
    ) : SingletonImageLoader.Factory {
        @Volatile
        private var activeImageLoader: ImageLoader? = null

        // Pass no explicit size so the configured value is read inside the disk-cache
        // initializer, which Coil runs lazily off the main thread. Reading it here would
        // block the first composition on a DataStore disk read during cold start.
        override fun newImageLoader(context: PlatformContext): ImageLoader = buildImageLoader(null).also { activeImageLoader = it }

        fun diskCacheSizeBytes(): Long = activeImageLoader?.diskCache?.size ?: 0L

        fun clearDiskCache() {
            activeImageLoader?.memoryCache?.clear()
            activeImageLoader?.diskCache?.clear()
        }

        @Synchronized
        fun resizeDiskCache(sizeMb: Int) {
            val previous = activeImageLoader
            previous?.shutdown()
            val replacement = buildImageLoader(sizeMb)
            activeImageLoader = replacement
            SingletonImageLoader.setUnsafe(replacement)
        }

        /** [cacheMb] null means "resolve from preferences lazily", inside the disk-cache initializer. */
        private fun buildImageLoader(cacheMb: Int?): ImageLoader {
            // Share the app client: same connection pool / TLS sessions, and its network
            // interceptor attaches the ApiKey only to the Stash origin on every hop.
            val authClient = okHttpClient

            return ImageLoader
                .Builder(context)
                .crossfade(150)
                .memoryCache {
                    MemoryCache
                        .Builder()
                        .maxSizePercent(context, 0.25)
                        .build()
                }.diskCache {
                    // Coil invokes this initializer lazily on a background thread, so blocking
                    // on the preference read here is safe — unlike doing it at construction.
                    val sizeMb = cacheMb ?: runBlocking(Dispatchers.IO) { uiSettings.imageCacheSizeMb.first() }
                    DiskCache
                        .Builder()
                        .directory(
                            this.context.cacheDir
                                .resolve("image_cache")
                                .toOkioPath(),
                        ).maxSizeBytes(sizeMb.coerceIn(64, 512).toLong() * 1024 * 1024)
                        .build()
                }.components {
                    add(OkHttpNetworkFetcherFactory(callFactory = { authClient }))
                }.build()
        }
    }
