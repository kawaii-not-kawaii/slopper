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
    fun `shuffle plays every scene once before any repeats`() {
        val ids = listOf("a", "b", "c", "d", "e", "f")
        val queue = PlayerQueue.from(ids, startIndex = 0, random = Random(7))
        queue.setShuffled(true)

        val played = mutableListOf(queue.currentId())
        repeat(ids.size - 1) {
            val next = queue.advance()
            assertNotNull(next)
            played += next
        }
        // Drawing without replacement: one full cycle covers the queue exactly.
        assertEquals(ids.toSet(), played.toSet())
        assertEquals(ids.size, played.distinct().size)
    }

    @Test
    fun `shuffle keeps going past a full cycle without needing repeat`() {
        val ids = listOf("a", "b", "c")
        val queue = PlayerQueue.from(ids, startIndex = 0, random = Random(7))
        queue.setShuffled(true)
        // Repeat stays OFF — shuffle loops on its own; Repeat governs ordered play.

        repeat(ids.size * 4) {
            val previous = queue.currentId()
            val next = queue.advance()
            assertNotNull(next)
            assertNotEquals(previous, next)
        }
    }

    @Test
    fun `each shuffle cycle covers every scene before repeating one`() {
        val ids = listOf("a", "b", "c", "d")
        val queue = PlayerQueue.from(ids, startIndex = 0, random = Random(5))
        queue.setShuffled(true)

        // First cycle: the 3 not currently playing, then the bag refills.
        val firstCycle = (1 until ids.size).map { queue.advance() }
        assertEquals(ids.size - 1, firstCycle.distinct().size)
        assertFalse(firstCycle.contains("a")) // the scene we started on
    }

    @Test
    fun `replacePool widens the queue and keeps the playing scene`() {
        val queue = PlayerQueue.from(listOf("a", "b"), startIndex = 1, random = Random(5))
        queue.setShuffled(true)
        assertEquals("b", queue.currentId())

        queue.replacePool(listOf("a", "b", "c", "d", "e"))

        assertEquals("b", queue.currentId())
        assertEquals(5, queue.snapshot().items.size)
        // The wider pool is drawn from immediately.
        val seen = (1..4).map { queue.advance() }
        assertEquals(4, seen.distinct().size)
        assertFalse(seen.contains("b"))
    }

    @Test
    fun `replacePool is ignored when it would drop the playing scene`() {
        val queue = PlayerQueue.from(listOf("a", "b"), startIndex = 1, random = Random(5))
        queue.replacePool(listOf("x", "y", "z"))

        assertEquals("b", queue.currentId())
        assertEquals(2, queue.snapshot().items.size)
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
