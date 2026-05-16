package io.stashapp.android.core.data.scene

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Optional
import io.stashapp.android.core.domain.SceneQuery
import io.stashapp.android.core.model.SceneSummary
import io.stashapp.android.core.network.StashEndpointProvider
import io.stashapp.android.graphql.FindScenesQuery
import io.stashapp.android.graphql.type.FindFilterType
import io.stashapp.android.graphql.type.SortDirectionEnum

class ScenePagingSource(
    private val apollo: ApolloClient,
    private val endpointProvider: StashEndpointProvider,
    private val query: SceneQuery,
) : PagingSource<Int, SceneSummary>() {
    override fun getRefreshKey(state: PagingState<Int, SceneSummary>): Int? {
        val anchor = state.anchorPosition ?: return null
        return state.closestPageToPosition(anchor)?.prevKey?.plus(1)
            ?: state.closestPageToPosition(anchor)?.nextKey?.minus(1)
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, SceneSummary> {
        val page = params.key ?: 1
        val endpoint =
            endpointProvider.current()
                ?: return LoadResult.Error(IllegalStateException("No server connected"))

        return try {
            val filter =
                FindFilterType(
                    q = Optional.presentIfNotNull(query.searchText),
                    page = Optional.present(page),
                    per_page = Optional.present(params.loadSize),
                    sort = Optional.present(query.sort.gqlSort),
                    direction = Optional.present(SortDirectionEnum.valueOf(query.sort.gqlDir)),
                )
            val response =
                apollo
                    .query(
                        FindScenesQuery(
                            filter = Optional.present(filter),
                            scene_filter = Optional.presentIfNotNull(query.filter.toGql()),
                            ids = Optional.absent(),
                        ),
                    ).execute()

            if (response.hasErrors()) {
                return LoadResult.Error(IllegalStateException(response.errors?.joinToString { it.message }))
            }
            val result =
                response.data?.findScenes
                    ?: return LoadResult.Error(IllegalStateException("Empty response"))

            val items = result.scenes.map { it.sceneCard.toSummary(endpoint) }
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
