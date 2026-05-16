package io.stashapp.android

import android.app.Application
import coil3.ImageLoader
import coil3.SingletonImageLoader
import dagger.hilt.android.HiltAndroidApp
import io.stashapp.android.core.ui.image.StashImageLoaderFactory
import javax.inject.Inject

@HiltAndroidApp
class StashApp : Application(), SingletonImageLoader.Factory {

    @Inject
    lateinit var imageLoaderFactory: StashImageLoaderFactory

    override fun newImageLoader(context: coil3.PlatformContext): ImageLoader =
        imageLoaderFactory.newImageLoader(context)
}
