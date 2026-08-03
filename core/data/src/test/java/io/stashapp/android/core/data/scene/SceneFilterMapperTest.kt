package io.stashapp.android.core.data.scene

import com.apollographql.apollo.api.Optional
import io.stashapp.android.core.domain.FilterEntityOption
import io.stashapp.android.core.domain.SceneFilter
import io.stashapp.android.core.domain.SceneFilterCriterion
import io.stashapp.android.core.domain.SceneFilterField
import io.stashapp.android.core.domain.SceneFilterModifier
import io.stashapp.android.graphql.type.CriterionModifier
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class SceneFilterMapperTest {
    @Test
    fun `repeated criteria are preserved as nested AND filters`() {
        val gql =
            SceneFilter(
                criteria =
                    listOf(
                        SceneFilterCriterion(SceneFilterField.Title, value = "first"),
                        SceneFilterCriterion(SceneFilterField.Title, value = "second"),
                    ),
            ).toGql()

        assertEquals("first", gql?.title.presentValue()?.value)
        assertEquals(
            "second",
            gql
                ?.AND
                .presentValue()
                ?.title
                .presentValue()
                ?.value,
        )
    }

    @Test
    fun `specialized Stash criteria retain their complete values`() {
        val gql =
            SceneFilter(
                criteria =
                    listOf(
                        SceneFilterCriterion(
                            field = SceneFilterField.Tags,
                            selected = listOf(FilterEntityOption("42", "Tag")),
                            depth = 2,
                        ),
                        SceneFilterCriterion(
                            field = SceneFilterField.Phash,
                            value = "abc123",
                            value2 = "8",
                        ),
                        SceneFilterCriterion(
                            field = SceneFilterField.Duplicated,
                            selected = listOf(FilterEntityOption("phash", "Perceptual hash")),
                            excluded = listOf(FilterEntityOption("url", "URL")),
                        ),
                        SceneFilterCriterion(
                            field = SceneFilterField.CustomField,
                            modifier = SceneFilterModifier.Includes,
                            value = "one, two",
                            auxiliary = "category",
                        ),
                    ),
            ).toGql()

        val tags = gql?.tags.presentValue()
        assertEquals(listOf("42"), tags?.value.presentValue())
        assertEquals(2, tags?.depth.presentValue())
        assertEquals(CriterionModifier.INCLUDES_ALL, tags?.modifier)

        val phash = gql?.phash_distance.presentValue()
        assertEquals("abc123", phash?.value)
        assertEquals(8, phash?.distance.presentValue())

        val duplicated = gql?.duplicated.presentValue()
        assertEquals(true, duplicated?.phash.presentValue())
        assertEquals(false, duplicated?.url.presentValue())

        val custom = gql?.custom_fields.presentValue()
        assertNotNull(custom)
        assertEquals("category", custom?.single()?.field)
        assertEquals(listOf("one", "two"), custom?.single()?.value.presentValue())
    }
}

private fun <T> Optional<T>?.presentValue(): T? = (this as? Optional.Present<T>)?.value
