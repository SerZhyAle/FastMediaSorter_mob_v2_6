package com.sza.fastmediasorter.domain.model

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for the Result sealed class and its helper methods.
 * Tests Success, Error, and Loading states along with all utility functions.
 */
class ResultTest {

    @Test
    fun `Success result stores data correctly`() {
        // Given
        val data = "test data"

        // When
        val result = Result.Success(data)

        // Then
        assertEquals("Expected data to match", data, result.data)
        assertTrue("Expected isSuccess to be true", result.isSuccess)
        assertFalse("Expected isError to be false", result.isError)
    }

    @Test
    fun `Error result stores message and throwable`() {
        // Given
        val message = "Test error"
        val throwable = RuntimeException("cause")
        val errorCode = ErrorCode.NETWORK_ERROR

        // When
        val result = Result.Error(message, throwable, errorCode)

        // Then
        assertEquals("Expected message to match", message, result.message)
        assertEquals("Expected throwable to match", throwable, result.throwable)
        assertEquals("Expected errorCode to match", errorCode, result.errorCode)
        assertFalse("Expected isSuccess to be false", result.isSuccess)
        assertTrue("Expected isError to be true", result.isError)
    }

    @Test
    fun `Error result with default errorCode`() {
        // Given
        val message = "Test error"

        // When
        val result = Result.Error(message)

        // Then
        assertEquals("Expected message to match", message, result.message)
        assertNull("Expected null throwable", result.throwable)
        assertEquals("Expected UNKNOWN errorCode", ErrorCode.UNKNOWN, result.errorCode)
    }

    @Test
    fun `Loading result is singleton`() {
        // When
        val result1 = Result.Loading
        val result2 = Result.Loading

        // Then
        assertSame("Expected same Loading instance", result1, result2)
        assertFalse("Expected isSuccess to be false", result1.isSuccess)
        assertFalse("Expected isError to be false", result1.isError)
    }

    @Test
    fun `getOrNull returns data for Success`() {
        // Given
        val data = 42
        val result: Result<Int> = Result.Success(data)

        // When
        val value = result.getOrNull()

        // Then
        assertEquals("Expected data value", data, value)
    }

    @Test
    fun `getOrNull returns null for Error`() {
        // Given
        val result: Result<Int> = Result.Error("error")

        // When
        val value = result.getOrNull()

        // Then
        assertNull("Expected null for Error", value)
    }

    @Test
    fun `getOrNull returns null for Loading`() {
        // Given
        val result: Result<Int> = Result.Loading

        // When
        val value = result.getOrNull()

        // Then
        assertNull("Expected null for Loading", value)
    }

    @Test
    fun `getOrDefault returns data for Success`() {
        // Given
        val data = 42
        val result: Result<Int> = Result.Success(data)

        // When
        val value = result.getOrDefault(0)

        // Then
        assertEquals("Expected data value", data, value)
    }

    @Test
    fun `getOrDefault returns default for Error`() {
        // Given
        val defaultValue = 0
        val result: Result<Int> = Result.Error("error")

        // When
        val value = result.getOrDefault(defaultValue)

        // Then
        assertEquals("Expected default value", defaultValue, value)
    }

    @Test
    fun `getOrDefault returns default for Loading`() {
        // Given
        val defaultValue = 0
        val result: Result<Int> = Result.Loading

        // When
        val value = result.getOrDefault(defaultValue)

        // Then
        assertEquals("Expected default value", defaultValue, value)
    }

    @Test
    fun `onSuccess executes action for Success`() {
        // Given
        var executed = false
        val result: Result<String> = Result.Success("data")

        // When
        result.onSuccess { executed = true }

        // Then
        assertTrue("Expected action to be executed", executed)
    }

    @Test
    fun `onSuccess does not execute for Error`() {
        // Given
        var executed = false
        val result: Result<String> = Result.Error("error")

        // When
        result.onSuccess { executed = true }

        // Then
        assertFalse("Expected action not to be executed", executed)
    }

    @Test
    fun `onSuccess does not execute for Loading`() {
        // Given
        var executed = false
        val result: Result<String> = Result.Loading

        // When
        result.onSuccess { executed = true }

        // Then
        assertFalse("Expected action not to be executed", executed)
    }

    @Test
    fun `onSuccess returns same Result`() {
        // Given
        val result: Result<String> = Result.Success("data")

        // When
        val returned = result.onSuccess { }

        // Then
        assertSame("Expected same result instance", result, returned)
    }

    @Test
    fun `onError executes action for Error`() {
        // Given
        var executed = false
        val result: Result<String> = Result.Error("error message", null, ErrorCode.NETWORK_ERROR)

        // When
        result.onError { msg, throwable, code ->
            assertEquals("error message", msg)
            assertNull(throwable)
            assertEquals(ErrorCode.NETWORK_ERROR, code)
            executed = true
        }

        // Then
        assertTrue("Expected action to be executed", executed)
    }

    @Test
    fun `onError does not execute for Success`() {
        // Given
        var executed = false
        val result: Result<String> = Result.Success("data")

        // When
        result.onError { _, _, _ -> executed = true }

        // Then
        assertFalse("Expected action not to be executed", executed)
    }

    @Test
    fun `onError does not execute for Loading`() {
        // Given
        var executed = false
        val result: Result<String> = Result.Loading

        // When
        result.onError { _, _, _ -> executed = true }

        // Then
        assertFalse("Expected action not to be executed", executed)
    }

    @Test
    fun `onError returns same Result`() {
        // Given
        val result: Result<String> = Result.Error("error")

        // When
        val returned = result.onError { _, _, _ -> }

        // Then
        assertSame("Expected same result instance", result, returned)
    }

    @Test
    fun `map transforms Success data`() {
        // Given
        val result: Result<Int> = Result.Success(5)

        // When
        val mapped = result.map { it * 2 }

        // Then
        assertTrue("Expected Success result", mapped is Result.Success)
        assertEquals("Expected transformed value", 10, (mapped as Result.Success).data)
    }

    @Test
    fun `map preserves Error`() {
        // Given
        val error = Result.Error("error", null, ErrorCode.NETWORK_ERROR)
        val result: Result<Int> = error

        // When
        val mapped = result.map { it * 2 }

        // Then
        assertSame("Expected same Error instance", error, mapped)
    }

    @Test
    fun `map preserves Loading`() {
        // Given
        val result: Result<Int> = Result.Loading

        // When
        val mapped = result.map { it * 2 }

        // Then
        assertSame("Expected same Loading instance", Result.Loading, mapped)
    }

    @Test
    fun `map changes type`() {
        // Given
        val result: Result<Int> = Result.Success(42)

        // When
        val mapped: Result<String> = result.map { it.toString() }

        // Then
        assertTrue("Expected Success result", mapped is Result.Success)
        assertEquals("Expected string value", "42", (mapped as Result.Success).data)
    }

    @Test
    fun `flatMap transforms Success to another Result`() {
        // Given
        val result: Result<Int> = Result.Success(5)

        // When
        val flatMapped = result.flatMap { value ->
            if (value > 0) Result.Success(value * 2)
            else Result.Error("negative")
        }

        // Then
        assertTrue("Expected Success result", flatMapped is Result.Success)
        assertEquals("Expected transformed value", 10, (flatMapped as Result.Success).data)
    }

    @Test
    fun `flatMap can return Error from Success`() {
        // Given
        val result: Result<Int> = Result.Success(-5)

        // When
        val flatMapped = result.flatMap { value ->
            if (value > 0) Result.Success(value * 2)
            else Result.Error("negative")
        }

        // Then
        assertTrue("Expected Error result", flatMapped is Result.Error)
        assertEquals("Expected error message", "negative", (flatMapped as Result.Error).message)
    }

    @Test
    fun `flatMap preserves Error`() {
        // Given
        val error = Result.Error("original error", null, ErrorCode.FILE_NOT_FOUND)
        val result: Result<Int> = error

        // When
        val flatMapped = result.flatMap { Result.Success(it * 2) }

        // Then
        assertSame("Expected same Error instance", error, flatMapped)
    }

    @Test
    fun `flatMap preserves Loading`() {
        // Given
        val result: Result<Int> = Result.Loading

        // When
        val flatMapped = result.flatMap { Result.Success(it * 2) }

        // Then
        assertSame("Expected same Loading instance", Result.Loading, flatMapped)
    }

    @Test
    fun `onSuccess and onError can be chained`() {
        // Given
        var successExecuted = false
        var errorExecuted = false
        val result: Result<String> = Result.Success("data")

        // When
        result
            .onSuccess { successExecuted = true }
            .onError { _, _, _ -> errorExecuted = true }

        // Then
        assertTrue("Expected onSuccess to execute", successExecuted)
        assertFalse("Expected onError not to execute", errorExecuted)
    }

    @Test
    fun `ErrorCode enum has all expected values`() {
        // Verify all error codes exist
        val codes = ErrorCode.values()
        assertTrue("Expected UNKNOWN", codes.contains(ErrorCode.UNKNOWN))
        assertTrue("Expected NETWORK_ERROR", codes.contains(ErrorCode.NETWORK_ERROR))
        assertTrue("Expected AUTHENTICATION_ERROR", codes.contains(ErrorCode.AUTHENTICATION_ERROR))
        assertTrue("Expected PERMISSION_DENIED", codes.contains(ErrorCode.PERMISSION_DENIED))
        assertTrue("Expected FILE_NOT_FOUND", codes.contains(ErrorCode.FILE_NOT_FOUND))
        assertTrue("Expected FILE_EXISTS", codes.contains(ErrorCode.FILE_EXISTS))
        assertTrue("Expected STORAGE_FULL", codes.contains(ErrorCode.STORAGE_FULL))
        assertTrue("Expected TIMEOUT", codes.contains(ErrorCode.TIMEOUT))
        assertTrue("Expected CANCELLED", codes.contains(ErrorCode.CANCELLED))
        assertTrue("Expected INVALID_INPUT", codes.contains(ErrorCode.INVALID_INPUT))
        assertTrue("Expected INVALID_OPERATION", codes.contains(ErrorCode.INVALID_OPERATION))
        assertTrue("Expected DATABASE_ERROR", codes.contains(ErrorCode.DATABASE_ERROR))
    }

    @Test
    fun `Success with null data is allowed`() {
        // Given
        val result: Result<String?> = Result.Success(null)

        // When
        val value = result.getOrNull()

        // Then
        assertNull("Expected null data", value)
        assertTrue("Expected isSuccess", result.isSuccess)
    }

    @Test
    fun `map and flatMap work together`() {
        // Given
        val result: Result<Int> = Result.Success(5)

        // When
        val transformed = result
            .map { it * 2 }  // 10
            .flatMap { value ->
                if (value > 5) Result.Success("High: $value")
                else Result.Success("Low: $value")
            }

        // Then
        assertTrue("Expected Success", transformed is Result.Success)
        assertEquals("Expected 'High: 10'", "High: 10", (transformed as Result.Success).data)
    }
}
