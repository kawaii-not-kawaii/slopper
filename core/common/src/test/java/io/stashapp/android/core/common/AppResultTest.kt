package io.stashapp.android.core.common

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AppResultTest {
    @Test
    fun `success wraps data`() {
        val result = AppResult.Success("hello")
        assertEquals("hello", result.data)
    }

    @Test
    fun `failure wraps error`() {
        val result: AppResult<String> = AppResult.Failure(AppError.Network("timeout"))
        assertTrue(result is AppResult.Failure)
        assertTrue((result as AppResult.Failure).error is AppError.Network)
    }

    @Test
    fun `network error carries message`() {
        val error = AppError.Network("timeout")
        assertEquals("timeout", error.message)
    }

    @Test
    fun `auth error carries message`() {
        val error = AppError.Auth("API key rejected")
        assertEquals("API key rejected", error.message)
    }

    @Test
    fun `unknown error preserves cause`() {
        val cause = RuntimeException("root cause")
        val error = AppError.Unknown("wrapper", cause = cause)
        assertEquals("wrapper", error.message)
        assertEquals(cause, error.cause)
    }
}
