package com.sza.fastmediasorter.domain.model

/**
 * A sealed class representing the result of an operation.
 * Used throughout the domain layer for error handling.
 *
 * @param T The type of data in case of success
 */
sealed class Result<out T> {

    /**
     * Represents a successful operation with data.
     */
    data class Success<T>(val data: T) : Result<T>()

    /**
     * Represents a failed operation with error information.
     */
    data class Error(
        val message: String,
        val throwable: Throwable? = null,
        val errorCode: ErrorCode = ErrorCode.UNKNOWN
    ) : Result<Nothing>()

    /**
     * Represents an ongoing operation.
     */
    data object Loading : Result<Nothing>()

    /**
     * Returns true if this is a Success result.
     */
    val isSuccess: Boolean get() = this is Success

    /**
     * Returns true if this is an Error result.
     */
    val isError: Boolean get() = this is Error

    /**
     * Returns the data if Success, or null otherwise.
     */
    fun getOrNull(): T? = when (this) {
        is Success -> data
        else -> null
    }

    /**
     * Returns the data if Success, or the default value otherwise.
     */
    fun getOrDefault(defaultValue: @UnsafeVariance T): T = when (this) {
        is Success -> data
        else -> defaultValue
    }

    /**
     * Executes the given action if this is a Success.
     */
    inline fun onSuccess(action: (T) -> Unit): Result<T> {
        if (this is Success) action(data)
        return this
    }

    /**
     * Executes the given action if this is an Error.
     */
    inline fun onError(action: (String, Throwable?, ErrorCode) -> Unit): Result<T> {
        if (this is Error) action(message, throwable, errorCode)
        return this
    }

    /**
     * Maps the success value to a new type.
     */
    inline fun <R> map(transform: (T) -> R): Result<R> = when (this) {
        is Success -> Success(transform(data))
        is Error -> this
        is Loading -> Loading
    }

    /**
     * Flat maps the success value to a new Result.
     */
    inline fun <R> flatMap(transform: (T) -> Result<R>): Result<R> = when (this) {
        is Success -> transform(data)
        is Error -> this
        is Loading -> Loading
    }
}

/**
 * Error codes for categorizing errors.
 */
enum class ErrorCode {
    UNKNOWN,
    NETWORK_ERROR,
    AUTHENTICATION_ERROR,
    PERMISSION_DENIED,
    FILE_NOT_FOUND,
    FILE_EXISTS,
    STORAGE_FULL,
    TIMEOUT,
    CANCELLED,
    INVALID_INPUT,
    INVALID_OPERATION,
    DATABASE_ERROR
}
