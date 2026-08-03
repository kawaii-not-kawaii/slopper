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

data class FilterOptionQuery(
    val kind: FilterEntityKind,
    val search: String? = null,
)

/**
 * A page of filter options plus the server's **total** match count, which is usually
 * larger than [options] — use [total] for "N results" labels, not `options.size`.
 */
data class FilterOptionPage(
    val options: List<FilterEntityOption>,
    val total: Int,
)

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

    suspend fun filterOptions(query: FilterOptionQuery): List<FilterEntityOption>

    /** As [filterOptions], but also carries the total match count for result-count labels. */
    suspend fun filterOptionsPage(query: FilterOptionQuery): FilterOptionPage
}
