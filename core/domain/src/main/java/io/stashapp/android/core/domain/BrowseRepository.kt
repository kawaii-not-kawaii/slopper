package io.stashapp.android.core.domain

import androidx.paging.PagingData
import io.stashapp.android.core.model.PerformerBrowseItem
import io.stashapp.android.core.model.StudioBrowseItem
import io.stashapp.android.core.model.TagBrowseItem
import kotlinx.coroutines.flow.Flow

enum class EntitySort(
    val gqlSort: String,
    val gqlDir: String,
) {
    Name("name", "ASC"),
    SceneCount("scenes_count", "DESC"),
    RecentlyUpdated("updated_at", "DESC"),
    Random("random", "DESC"),
}

interface BrowseRepository {
    fun performers(
        search: String?,
        sort: EntitySort,
    ): Flow<PagingData<PerformerBrowseItem>>

    fun studios(
        search: String?,
        sort: EntitySort,
    ): Flow<PagingData<StudioBrowseItem>>

    fun tags(
        search: String?,
        sort: EntitySort,
    ): Flow<PagingData<TagBrowseItem>>
}
