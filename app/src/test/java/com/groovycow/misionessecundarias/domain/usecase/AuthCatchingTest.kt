package com.groovycow.misionessecundarias.domain.usecase

import com.groovycow.misionessecundarias.domain.model.AuthError
import com.groovycow.misionessecundarias.domain.model.AuthException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.coroutines.cancellation.CancellationException

class AuthCatchingTest {
    @Test
    fun returnsSuccessWithBlockValue() =
        runTest {
            val result = authCatching { 42 }

            assertTrue(result.isSuccess)
            assertEquals(42, result.getOrNull())
        }

    @Test
    fun returnsFailureWithAuthExceptionIntact() =
        runTest {
            val result = authCatching<Unit> { throw AuthException(AuthError.EmailInUse) }

            val error = (result.exceptionOrNull() as AuthException).error
            assertEquals(AuthError.EmailInUse, error)
        }

    @Test
    fun returnsFailureForOtherExceptions() =
        runTest {
            val boom = IllegalStateException("boom")

            val result = authCatching<Unit> { throw boom }

            assertEquals(boom, result.exceptionOrNull())
        }

    @Test(expected = CancellationException::class)
    fun rethrowsCancellationException() =
        runTest {
            authCatching<Unit> { throw CancellationException("cancelado") }
        }
}
