package io.stashapp.android.feature.library

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.LocalDate

class StashCriteriaSectionTest {
    @Test
    fun `date prefix parses from timestamp value`() {
        val millis = "2026-10-01 13:45".toDateMillis()!!
        assertEquals(LocalDate.of(2026, 10, 1), millis.toUtcDate())
    }

    @Test
    fun `unparseable or invalid dates yield null`() {
        assertNull("".toDateMillis())
        assertNull("yesterday".toDateMillis())
        assertNull("2026-02-30".toDateMillis())
    }

    @Test
    fun `time suffix parses hours and minutes`() {
        assertEquals(13 to 45, "2026-10-01 13:45".timeParts())
    }

    @Test
    fun `missing or garbage time falls back and clamps into TimePicker range`() {
        assertEquals(0 to 0, "2026-10-01".timeParts())
        assertEquals(0 to 0, "".timeParts())
        assertEquals(23 to 59, "2026-10-01 99:99".timeParts())
    }
}
