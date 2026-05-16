package io.stashapp.android.core.data.browse

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Optional
import io.stashapp.android.core.domain.EntitySort
import io.stashapp.android.core.model.PerformerBrowseItem
import io.stashapp.android.core.model.StudioBrowseItem
import io.stashapp.android.core.model.TagBrowseItem
import io.stashapp.android.core.network.StashEndpointProvider
import io.stashapp.android.graphql.FindPerformersListQuery
import io.stashapp.android.graphql.FindStudiosListQuery
import io.stashapp.android.graphql.FindTagsListQuery
import io.stashapp.android.graphql.type.FindFilterType
import io.stashapp.android.graphql.type.SortDirectionEnum

private fun buildFilter(page: Int, perPage: Int, search: String?, sort: EntitySort) =
    FindFilterType(
        q = Optional.presentIfNotNull(search?.ifBlank { null }),
        page = Optional.present(page),
        per_page = Optional.present(perPage),
        sort = Optional.present(sort.gqlSort),
        direction = Optional.present(SortDirectionEnum.valueOf(sort.gqlDir)),
    )

/**
 * Paging sources for the three browse entities. They share the same paging
 * machinery — keying, refresh key derivation, error mapping — so defining them
 * together in one file keeps the patterns honest and easy to spot-check.
 */

class PerformersPagingSource(
    private val apollo: ApolloClient,
    private val endpointProvider: StashEndpointProvider,
    private val search: String?,
    private val sort: EntitySort,
) : PagingSource<Int, PerformerBrowseItem>() {

    override fun getRefreshKey(state: PagingState<Int, PerformerBrowseItem>): Int? {
        val anchor = state.anchorPosition ?: return null
        return state.closestPageToPosition(anchor)?.prevKey?.plus(1)
            ?: state.closestPageToPosition(anchor)?.nextKey?.minus(1)
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, PerformerBrowseItem> {
        val page = params.key ?: 1
        val endpoint = endpointProvider.current()
            ?: return LoadResult.Error(IllegalStateException("No server connected"))
        return try {
            val resp = apollo.query(
                FindPerformersListQuery(
                    filter = Optional.present(buildFilter(page, params.loadSize, search, sort)),
                ),
            ).execute()
            if (resp.hasErrors()) return LoadResult.Error(
                IllegalStateException(resp.errors?.joinToString { it.message }),
            )
            val result = resp.data?.findPerformers
                ?: return LoadResult.Error(IllegalStateException("Empty response"))
            val items = result.performers.map {
                PerformerBrowseItem(
                    id = it.id,
                    name = it.name,
                    imageUrl = endpoint.resolve(it.image_path),
                    gender = it.gender?.rawValue,
                    sceneCount = it.scene_count,
                    favorite = it.favorite,
                )
            }
            val totalPages = (result.count + params.loadSize - 1) / params.loadSize
            LoadResult.Page(
                data = items,
                prevKey = if (page <= 1) null else page - 1,
                nextKey = if (page >= totalPages) null else page + 1,
            )
        } catch (e: Throwable) {
            LoadResult.Error(e)
        }
    }
}

class StudiosPagingSource(
    private val apollo: ApolloClient,
    private val endpointProvider: StashEndpointProvider,
    private val search: String?,
    private val sort: EntitySort,
) : PagingSource<Int, StudioBrowseItem>() {

    override fun getRefreshKey(state: PagingState<Int, StudioBrowseItem>): Int? {
        val anchor = state.anchorPosition ?: return null
        return state.closestPageToPosition(anchor)?.prevKey?.plus(1)
            ?: state.closestPageToPosition(anchor)?.nextKey?.minus(1)
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, StudioBrowseItem> {
        val page = params.key ?: 1
        val endpoint = endpointProvider.current()
            ?: return LoadResult.Error(IllegalStateException("No server connected"))
        return try {
            val resp = apollo.query(
                FindStudiosListQuery(
                    filter = Optional.present(buildFilter(page, params.loadSize, search, sort)),
                ),
            ).execute()
            if (resp.hasErrors()) return LoadResult.Error(
                IllegalStateException(resp.errors?.joinToString { it.message }),
            )
            val result = resp.data?.findStudios
                ?: return LoadResult.Error(IllegalStateException("Empty response"))
            val items = result.studios.map {
                StudioBrowseItem(
                    id = it.id,
                    name = it.name,
                    imageUrl = endpoint.resolve(it.image_path),
                    sceneCount = it.scene_count,
                )
            }
            val totalPages = (result.count + params.loadSize - 1) / params.loadSize
            LoadResult.Page(
                data = items,
                prevKey = if (page <= 1) null else page - 1,
                nextKey = if (page >= totalPages) null else page + 1,
            )
        } catch (e: Throwable) {
            LoadResult.Error(e)
        }
    }
}

class TagsPagingSource(
    private val apollo: ApolloClient,
    private val endpointProvider: StashEndpointProvider,
    private val search: String?,
    private val sort: EntitySort,
) : PagingSource<Int, TagBrowseItem>() {

    override fun getRefreshKey(state: PagingState<Int, TagBrowseItem>): Int? {
        val anchor = state.anchorPosition ?: return null
        return state.closestPageToPosition(anchor)?.prevKey?.plus(1)
            ?: state.closestPageToPosition(anchor)?.nextKey?.minus(1)
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, TagBrowseItem> {
        val page = params.key ?: 1
        val endpoint = endpointProvider.current()
            ?: return LoadResult.Error(IllegalStateException("No server connected"))
        return try {
            val resp = apollo.query(
                FindTagsListQuery(
                    filter = Optional.present(buildFilter(page, params.loadSize, search, sort)),
                ),
            ).execute()
            if (resp.hasErrors()) return LoadResult.Error(
                IllegalStateException(resp.errors?.joinToString { it.message }),
            )
            val result = resp.data?.findTags
                ?: return LoadResult.Error(IllegalStateException("Empty response"))
            val items = result.tags.map {
                TagBrowseItem(
                    id = it.id,
                    name = it.name,
                    imageUrl = endpoint.resolve(it.image_path),
                    sceneCount = it.scene_count,
                )
            }
            val totalPages = (result.count + params.loadSize - 1) / params.loadSize
            LoadResult.Page(
                data = items,
                prevKey = if (page <= 1) null else page - 1,
                nextKey = if (page >= totalPages) null else page + 1,
            )
        } catch (e: Throwable) {
            LoadResult.Error(e)
        }
    }
}
