package io.stashapp.android.core.data.browse

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.ApolloResponse
import com.apollographql.apollo.api.Optional
import io.stashapp.android.core.domain.BrowseRepository
import io.stashapp.android.core.domain.EntitySort
import io.stashapp.android.core.domain.FilterEntityKind
import io.stashapp.android.core.domain.FilterEntityOption
import io.stashapp.android.core.domain.FilterOptionQuery
import io.stashapp.android.core.model.PerformerBrowseItem
import io.stashapp.android.core.model.StudioBrowseItem
import io.stashapp.android.core.model.TagBrowseItem
import io.stashapp.android.core.network.StashEndpointProvider
import io.stashapp.android.graphql.FindFoldersListQuery
import io.stashapp.android.graphql.FindGalleriesListQuery
import io.stashapp.android.graphql.FindGroupsListQuery
import io.stashapp.android.graphql.FindPerformersListQuery
import io.stashapp.android.graphql.FindStudiosListQuery
import io.stashapp.android.graphql.FindTagsListQuery
import io.stashapp.android.graphql.type.FindFilterType
import io.stashapp.android.graphql.type.SortDirectionEnum
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

        override suspend fun filterOptions(query: FilterOptionQuery): List<FilterEntityOption> {
            val sort =
                when (query.kind) {
                    FilterEntityKind.Gallery -> "title"
                    FilterEntityKind.Folder -> "path"
                    else -> "name"
                }
            val filter =
                FindFilterType(
                    q = Optional.presentIfNotNull(query.search?.takeIf { it.isNotBlank() }),
                    sort = Optional.present(sort),
                    direction = Optional.present(SortDirectionEnum.ASC),
                    page = Optional.present(1),
                    per_page = Optional.present(50),
                )

            return when (query.kind) {
                FilterEntityKind.Performer -> {
                    val response =
                        apollo
                            .query(FindPerformersListQuery(Optional.present(filter)))
                            .execute()
                    response.throwOnErrors()
                    response.data?.findPerformers?.performers.orEmpty().map {
                        FilterEntityOption(it.id, it.name)
                    }
                }
                FilterEntityKind.Studio -> {
                    val response =
                        apollo
                            .query(FindStudiosListQuery(Optional.present(filter)))
                            .execute()
                    response.throwOnErrors()
                    response.data?.findStudios?.studios.orEmpty().map {
                        FilterEntityOption(it.id, it.name)
                    }
                }
                FilterEntityKind.Tag -> {
                    val response =
                        apollo
                            .query(FindTagsListQuery(Optional.present(filter)))
                            .execute()
                    response.throwOnErrors()
                    response.data?.findTags?.tags.orEmpty().map {
                        FilterEntityOption(it.id, it.name)
                    }
                }
                FilterEntityKind.Group -> {
                    val response =
                        apollo
                            .query(FindGroupsListQuery(Optional.present(filter)))
                            .execute()
                    response.throwOnErrors()
                    response.data?.findGroups?.groups.orEmpty().map {
                        FilterEntityOption(it.id, it.name)
                    }
                }
                FilterEntityKind.Gallery -> {
                    val response =
                        apollo
                            .query(FindGalleriesListQuery(Optional.present(filter)))
                            .execute()
                    response.throwOnErrors()
                    response.data?.findGalleries?.galleries.orEmpty().map {
                        FilterEntityOption(it.id, it.title ?: "Untitled")
                    }
                }
                FilterEntityKind.Folder -> {
                    val response =
                        apollo
                            .query(FindFoldersListQuery(Optional.present(filter)))
                            .execute()
                    response.throwOnErrors()
                    response.data?.findFolders?.folders.orEmpty().map {
                        FilterEntityOption(it.id, it.path)
                    }
                }
            }
        }
    }

private fun ApolloResponse<*>.throwOnErrors() {
    if (hasErrors()) {
        throw IllegalStateException(errors?.joinToString { it.message })
    }
}
