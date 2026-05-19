package io.stashapp.android.core.data.scene

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Optional
import io.stashapp.android.core.common.AppError
import io.stashapp.android.core.common.AppResult
import io.stashapp.android.core.domain.SceneQuery
import io.stashapp.android.core.domain.SceneRepository
import io.stashapp.android.core.model.SceneDetail
import io.stashapp.android.core.model.SceneSummary
import io.stashapp.android.core.network.StashEndpointProvider
import io.stashapp.android.graphql.FindSceneQuery
import io.stashapp.android.graphql.FindScenesQuery
import io.stashapp.android.graphql.SceneAddPlayMutation
import io.stashapp.android.graphql.SceneDecrementOMutation
import io.stashapp.android.graphql.SceneIncrementOMutation
import io.stashapp.android.graphql.SceneSaveActivityMutation
import io.stashapp.android.graphql.SceneUpdateMutation
import io.stashapp.android.graphql.type.FindFilterType
import io.stashapp.android.graphql.type.SceneUpdateInput
import io.stashapp.android.graphql.type.SortDirectionEnum
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultSceneRepository
    @Inject
    constructor(
        private val apollo: ApolloClient,
        private val endpointProvider: StashEndpointProvider,
    ) : SceneRepository {
        override fun pagedScenes(query: SceneQuery): Flow<PagingData<SceneSummary>> =
            Pager(
                config =
                    PagingConfig(
                        pageSize = 40,
                        // Trigger the next fetch a full page ahead so 120Hz flings don't
                        // stall at a loading spinner. Previous value of 20 left only
                        // ~200ms of buffer on a fast scroll.
                        prefetchDistance = 40,
                        initialLoadSize = 40,
                        enablePlaceholders = false,
                    ),
                pagingSourceFactory = { ScenePagingSource(apollo, endpointProvider, query) },
            ).flow

        override suspend fun scenes(
            query: io.stashapp.android.core.domain.SceneQuery,
            limit: Int,
        ): AppResult<List<SceneSummary>> {
            val endpoint =
                endpointProvider.current()
                    ?: return AppResult.Failure(AppError.Auth("No server connected"))
            return try {
                val findFilter =
                    FindFilterType(
                        q = Optional.presentIfNotNull(query.searchText),
                        page = Optional.present(1),
                        per_page = Optional.present(limit),
                        sort = Optional.present(query.sort.gqlSort),
                        direction = Optional.present(SortDirectionEnum.valueOf(query.sort.gqlDir)),
                    )
                val response =
                    apollo
                        .query(
                            FindScenesQuery(
                                filter = Optional.present(findFilter),
                                scene_filter = Optional.presentIfNotNull(query.filter.toGql()),
                                ids = Optional.absent(),
                            ),
                        ).execute()
                if (response.hasErrors()) {
                    AppResult.Failure(
                        AppError.Server(response.errors?.joinToString { it.message } ?: "Unknown error"),
                    )
                } else {
                    val result =
                        response.data?.findScenes
                            ?: return AppResult.Failure(AppError.Server("Empty response"))
                    AppResult.Success(result.scenes.map { it.sceneCard.toSummary(endpoint) })
                }
            } catch (e: Throwable) {
                AppResult.Failure(AppError.Network(e.message ?: "Network error"))
            }
        }

        override suspend fun scene(id: String): AppResult<SceneDetail> {
            val endpoint =
                endpointProvider.current()
                    ?: return AppResult.Failure(AppError.Auth("No server connected"))
            return try {
                val response = apollo.query(FindSceneQuery(id = id)).execute()
                if (response.hasErrors()) {
                    AppResult.Failure(
                        AppError.Server(response.errors?.joinToString { it.message } ?: "Unknown error"),
                    )
                } else {
                    val scene =
                        response.data?.findScene
                            ?: return AppResult.Failure(AppError.NotFound("Scene $id not found"))
                    AppResult.Success(scene.toDetail(endpoint))
                }
            } catch (e: Throwable) {
                AppResult.Failure(AppError.Network(e.message ?: "Network error"))
            }
        }

        override suspend fun saveActivity(
            sceneId: String,
            resumeTimeSeconds: Double,
            playDurationSeconds: Double,
        ): AppResult<Unit> =
            mutate {
                apollo
                    .mutation(
                        SceneSaveActivityMutation(
                            id = sceneId,
                            resumeTime = Optional.present(resumeTimeSeconds),
                            playDuration = Optional.present(playDurationSeconds),
                        ),
                    ).execute()
            }

        override suspend fun addPlay(sceneId: String): AppResult<Unit> =
            mutate {
                apollo.mutation(SceneAddPlayMutation(id = sceneId)).execute()
            }

        override suspend fun incrementO(sceneId: String): AppResult<Int> =
            mutateMapped {
                val resp = apollo.mutation(SceneIncrementOMutation(id = sceneId)).execute()
                resp to resp.data?.sceneAddO?.count
            }

        override suspend fun decrementO(sceneId: String): AppResult<Int> =
            mutateMapped {
                val resp = apollo.mutation(SceneDecrementOMutation(id = sceneId)).execute()
                resp to resp.data?.sceneDeleteO?.count
            }

        override suspend fun setRating(
            sceneId: String,
            rating100: Int?,
        ): AppResult<Unit> =
            mutate {
                apollo
                    .mutation(
                        SceneUpdateMutation(
                            input =
                                SceneUpdateInput(
                                    id = sceneId,
                                    rating100 = Optional.present(rating100),
                                ),
                        ),
                    ).execute()
            }

        override suspend fun setOrganized(
            sceneId: String,
            organized: Boolean,
        ): AppResult<Unit> =
            mutate {
                apollo
                    .mutation(
                        SceneUpdateMutation(
                            input =
                                SceneUpdateInput(
                                    id = sceneId,
                                    organized = Optional.present(organized),
                                ),
                        ),
                    ).execute()
            }

        // ---- helpers --------------------------------------------------------

        private suspend inline fun mutate(
            crossinline block: suspend () -> com.apollographql.apollo.api.ApolloResponse<*>,
        ): AppResult<Unit> =
            try {
                val resp = block()
                if (resp.hasErrors()) {
                    AppResult.Failure(
                        AppError.Server(resp.errors?.joinToString { it.message } ?: "Mutation failed"),
                    )
                } else {
                    AppResult.Success(Unit)
                }
            } catch (e: Throwable) {
                AppResult.Failure(AppError.Network(e.message ?: "Network error"))
            }

        private suspend inline fun <T> mutateMapped(
            crossinline block: suspend () -> Pair<com.apollographql.apollo.api.ApolloResponse<*>, T?>,
        ): AppResult<T> =
            try {
                val (resp, value) = block()
                when {
                    resp.hasErrors() ->
                        AppResult.Failure(
                            AppError.Server(resp.errors?.joinToString { it.message } ?: "Mutation failed"),
                        )
                    value == null -> AppResult.Failure(AppError.Server("Empty mutation response"))
                    else -> AppResult.Success(value)
                }
            } catch (e: Throwable) {
                AppResult.Failure(AppError.Network(e.message ?: "Network error"))
            }
    }
