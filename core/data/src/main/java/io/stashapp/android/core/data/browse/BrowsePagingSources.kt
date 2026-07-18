package io.stashapp.android.core.data.browse

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.ApolloResponse
import com.apollographql.apollo.api.Optional
import io.stashapp.android.core.domain.EntitySort
import io.stashapp.android.core.model.PerformerBrowseItem
import io.stashapp.android.core.model.StudioBrowseItem
import io.stashapp.android.core.model.TagBrowseItem
import io.stashapp.android.core.network.StashEndpoint
import io.stashapp.android.core.network.StashEndpointProvider
import io.stashapp.android.graphql.FindPerformersListQuery
import io.stashapp.android.graphql.FindStudiosListQuery
import io.stashapp.android.graphql.FindTagsListQuery
import io.stashapp.android.graphql.type.FindFilterType
import io.stashapp.android.graphql.type.SortDirectionEnum
import kotlinx.coroutines.CancellationException

private fun buildFilter(
    page: Int,
    perPage: Int,
    search: String?,
    sort: EntitySort,
) = FindFilterType(
    q = Optional.presentIfNotNull(search),
    sort = Optional.present(sort.gqlSort),
    direction = Optional.present(SortDirectionEnum.valueOf(sort.gqlDir)),
    page = Optional.present(page),
    per_page = Optional.present(perPage),
)

/** Parsed query result — items + total page count. */
data class BrowsePage<out T>(
    val items: List<T>,
    val totalPages: Int,
)

/**
 * Generic paging source for browse entities (performers, studios, tags).
 * Collapses the three near-identical classes into one parameterized by
 * an [execute] query lambda and a [map] function.
 */
class BrowsePagingSource<T : Any>(
    private val apollo: ApolloClient,
    private val endpointProvider: StashEndpointProvider,
    private val search: String?,
    private val sort: EntitySort,
    private val execute: suspend ApolloClient.(FindFilterType) -> ApolloResponse<*>,
    private val map: (ApolloResponse<*>, StashEndpoint, Int) -> BrowsePage<T>,
) : PagingSource<Int, T>() {
    override fun getRefreshKey(state: PagingState<Int, T>): Int? {
        val anchor = state.anchorPosition ?: return null
        return state.closestPageToPosition(anchor)?.prevKey?.plus(1)
            ?: state.closestPageToPosition(anchor)?.nextKey?.minus(1)
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, T> {
        val page = params.key ?: 1
        val endpoint =
            endpointProvider.current()
                ?: return LoadResult.Error(IllegalStateException("No server connected"))
        return try {
            val resp = apollo.execute(buildFilter(page, params.loadSize, search, sort))
            if (resp.hasErrors()) {
                return LoadResult.Error<Int, T>(
                    IllegalStateException(resp.errors?.joinToString { it.message }),
                )
            }
            val result = map(resp, endpoint, params.loadSize)
            LoadResult.Page<Int, T>(
                data = result.items,
                prevKey = if (page <= 1) null else page - 1,
                nextKey = if (page >= result.totalPages) null else page + 1,
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            LoadResult.Error<Int, T>(e)
        }
    }
}

// ---- Entity-specific bindings ----------------------------------------------

private val performersBinding =
    Binding<PerformerBrowseItem>(
        execute = { filter ->
            query(FindPerformersListQuery(filter = Optional.present(filter))).execute()
        },
        map = { resp, endpoint, loadSize ->
            val result =
                (resp.data as FindPerformersListQuery.Data?)?.findPerformers
                    ?: throw IllegalStateException("Empty response")
            BrowsePage(
                items =
                    result.performers.map {
                        PerformerBrowseItem(
                            id = it.id,
                            name = it.name,
                            imageUrl = endpoint.resolve(it.image_path),
                            gender = it.gender?.rawValue,
                            sceneCount = it.scene_count,
                            favorite = it.favorite,
                        )
                    },
                totalPages = (result.count + loadSize - 1) / loadSize,
            )
        },
    )

private val studiosBinding =
    Binding<StudioBrowseItem>(
        execute = { filter ->
            query(FindStudiosListQuery(filter = Optional.present(filter))).execute()
        },
        map = { resp, endpoint, loadSize ->
            val result =
                (resp.data as FindStudiosListQuery.Data?)?.findStudios
                    ?: throw IllegalStateException("Empty response")
            BrowsePage(
                items =
                    result.studios.map {
                        StudioBrowseItem(
                            id = it.id,
                            name = it.name,
                            imageUrl = endpoint.resolve(it.image_path),
                            sceneCount = it.scene_count,
                        )
                    },
                totalPages = (result.count + loadSize - 1) / loadSize,
            )
        },
    )

private val tagsBinding =
    Binding<TagBrowseItem>(
        execute = { filter ->
            query(FindTagsListQuery(filter = Optional.present(filter))).execute()
        },
        map = { resp, endpoint, loadSize ->
            val result =
                (resp.data as FindTagsListQuery.Data?)?.findTags
                    ?: throw IllegalStateException("Empty response")
            BrowsePage(
                items =
                    result.tags.map {
                        TagBrowseItem(
                            id = it.id,
                            name = it.name,
                            imageUrl = endpoint.resolve(it.image_path),
                            sceneCount = it.scene_count,
                        )
                    },
                totalPages = (result.count + loadSize - 1) / loadSize,
            )
        },
    )

fun performersPagingSource(
    apollo: ApolloClient,
    endpointProvider: StashEndpointProvider,
    search: String?,
    sort: EntitySort,
) = BrowsePagingSource(apollo, endpointProvider, search, sort, performersBinding.execute, performersBinding.map)

fun studiosPagingSource(
    apollo: ApolloClient,
    endpointProvider: StashEndpointProvider,
    search: String?,
    sort: EntitySort,
) = BrowsePagingSource(apollo, endpointProvider, search, sort, studiosBinding.execute, studiosBinding.map)

fun tagsPagingSource(
    apollo: ApolloClient,
    endpointProvider: StashEndpointProvider,
    search: String?,
    sort: EntitySort,
) = BrowsePagingSource(apollo, endpointProvider, search, sort, tagsBinding.execute, tagsBinding.map)

private data class Binding<T>(
    val execute: suspend ApolloClient.(FindFilterType) -> ApolloResponse<*>,
    val map: (ApolloResponse<*>, StashEndpoint, Int) -> BrowsePage<T>,
)
