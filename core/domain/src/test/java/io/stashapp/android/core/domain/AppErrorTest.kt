package io.stashapp.android.core.domain

import io.stashapp.android.core.common.AppError
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class AppErrorTest {
    @Test
    fun `auth error carries message`() {
        val e = AppError.Auth("API key rejected")
        assertEquals("API key rejected", e.message)
    }

    @Test
    fun `unknown error preserves cause`() {
        val cause = RuntimeException("root cause")
        val e = AppError.Unknown("wrapper", cause = cause)
        assertEquals(cause, e.cause)
    }

    @Test
    fun `server error carries message`() {
        val e = AppError.Server("500 Internal Server Error")
        assertNotNull(e.message)
        assertEquals("500 Internal Server Error", e.message)
    }

    @Test
    fun `not found error carries message`() {
        val e = AppError.NotFound("Scene 42 not found")
        assertEquals("Scene 42 not found", e.message)
    }
}
