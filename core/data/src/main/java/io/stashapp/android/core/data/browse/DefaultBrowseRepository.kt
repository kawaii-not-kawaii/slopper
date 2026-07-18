package io.stashapp.android.core.data.browse

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.apollographql.apollo.ApolloClient
import io.stashapp.android.core.domain.BrowseRepository
import io.stashapp.android.core.domain.EntitySort
import io.stashapp.android.core.model.PerformerBrowseItem
import io.stashapp.android.core.model.StudioBrowseItem
import io.stashapp.android.core.model.TagBrowseItem
import io.stashapp.android.core.network.StashEndpointProvider
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultBrowseRepository
    @Inject
    constructor(
        private val apollo: ApolloClient,
        private val endpointProvider: StashEndpointProvider,
    ) : BrowseRepository {
        private val config =
            PagingConfig(
                pageSize = 40,
                prefetchDistance = 20,
                initialLoadSize = 40,
                enablePlaceholders = false,
            )

        override fun performers(
            search: String?,
            sort: EntitySort,
        ): Flow<PagingData<PerformerBrowseItem>> = Pager(config) { performersPagingSource(apollo, endpointProvider, search, sort) }.flow

        override fun studios(
            search: String?,
            sort: EntitySort,
        ): Flow<PagingData<StudioBrowseItem>> = Pager(config) { studiosPagingSource(apollo, endpointProvider, search, sort) }.flow

        override fun tags(
            search: String?,
            sort: EntitySort,
        ): Flow<PagingData<TagBrowseItem>> = Pager(config) { tagsPagingSource(apollo, endpointProvider, search, sort) }.flow
    }
