package io.stashapp.android.core.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.stashapp.android.core.data.browse.DefaultBrowseRepository
import io.stashapp.android.core.data.connection.DefaultConnectionRepository
import io.stashapp.android.core.data.connection.EndpointStateHolder
import io.stashapp.android.core.data.scene.DefaultSceneRepository
import io.stashapp.android.core.domain.BrowseRepository
import io.stashapp.android.core.domain.ConnectionRepository
import io.stashapp.android.core.domain.SceneRepository
import io.stashapp.android.core.network.StashEndpointProvider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {
    @Binds
    @Singleton
    abstract fun bindConnectionRepository(impl: DefaultConnectionRepository): ConnectionRepository

    /**
     * Endpoint state is owned by [EndpointStateHolder] — kept separate from the
     * repository to avoid an Apollo ↔ repository dependency cycle (the Apollo
     * client needs the endpoint, but the repository needs the Apollo client).
     */
    @Binds
    @Singleton
    abstract fun bindEndpointProvider(impl: EndpointStateHolder): StashEndpointProvider

    @Binds
    @Singleton
    abstract fun bindSceneRepository(impl: DefaultSceneRepository): SceneRepository

    @Binds
    @Singleton
    abstract fun bindBrowseRepository(impl: DefaultBrowseRepository): BrowseRepository
}
