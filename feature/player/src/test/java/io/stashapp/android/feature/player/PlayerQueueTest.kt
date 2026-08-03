package io.stashapp.android.feature.player

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.random.Random

class PlayerQueueTest {
    @Test
    fun `shuffle draws every next scene randomly without immediate repeats`() {
        val queue = PlayerQueue.from(listOf("a", "b", "c", "d"), startIndex = 0, random = Random(7))
        queue.setShuffled(true)

        repeat(100) {
            val previous = queue.currentId()
            val next = queue.advance()
            assertNotNull(next)
            assertNotEquals(previous, next)
        }
    }

    @Test
    fun `previous walks actual shuffled playback history`() {
        val queue = PlayerQueue.from(listOf("a", "b", "c", "d"), startIndex = 0, random = Random(11))
        queue.setShuffled(true)
        val first = queue.currentId()
        val second = queue.advance()
        val third = queue.advance()

        assertNotEquals(first, second)
        assertNotEquals(second, third)
        assertTrue(queue.snapshot().hasPrevious())
        assertEquals(second, queue.previous())
        assertEquals(first, queue.previous())
        assertNull(queue.previous())
        assertFalse(queue.snapshot().hasPrevious())
    }

    @Test
    fun `next after going back starts a new random history branch`() {
        val queue = PlayerQueue.from(listOf("a", "b", "c", "d"), startIndex = 0, random = Random(19))
        queue.setShuffled(true)
        queue.advance()
        queue.advance()
        val previous = queue.previous()

        val branched = queue.advance()
        assertNotEquals(previous, branched)
        assertEquals(previous, queue.previous())
    }

    @Test
    fun `shuffle cannot repeat a queue with one distinct scene`() {
        val queue = PlayerQueue.from(listOf("a", "a"), startIndex = 0, random = Random(3))
        queue.setShuffled(true)

        assertFalse(queue.snapshot().hasNext())
        assertNull(queue.advance())
    }
}
