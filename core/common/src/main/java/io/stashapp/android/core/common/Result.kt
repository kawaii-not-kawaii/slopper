package io.stashapp.android.core.common

/**
 * Lightweight result type that preserves error context for the UI layer.
 * Prefer over raw exceptions in repository method signatures.
 */
sealed interface AppResult<out T> {
    data class Success<T>(val data: T) : AppResult<T>
    data class Failure(val error: AppError) : AppResult<Nothing>
}

sealed class AppError(open val message: String) {
    data class Network(override val message: String) : AppError(message)
    data class Auth(override val message: String) : AppError(message)
    data class NotFound(override val message: String) : AppError(message)
    data class Server(override val message: String) : AppError(message)
    data class Unknown(override val message: String, val cause: Throwable? = null) : AppError(message)
}
