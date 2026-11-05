package com.groovycow.misionessecundarias.data.repository

import com.groovycow.misionessecundarias.domain.model.AuthError
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException
import java.net.SocketTimeoutException

class AuthErrorMapperTest {
    private val mapper = AuthErrorMapper()

    @Test
    fun mapsUserAlreadyExistsToEmailInUse() {
        assertEquals(AuthError.EmailInUse, mapper.mapRest("user_already_exists", 422, AuthOperation.OTHER))
    }

    @Test
    fun mapsEmailExistsToEmailInUse() {
        assertEquals(AuthError.EmailInUse, mapper.mapRest("email_exists", 422, AuthOperation.OTHER))
    }

    @Test
    fun mapsWeakPasswordToWeakPassword() {
        assertEquals(AuthError.WeakPassword, mapper.mapRest("weak_password", 422, AuthOperation.OTHER))
    }

    @Test
    fun mapsSamePasswordToSamePassword() {
        assertEquals(AuthError.SamePassword, mapper.mapRest("same_password", 422, AuthOperation.OTHER))
    }

    @Test
    fun mapsOtpExpiredToInvalidOrExpiredCodeInAnyOperation() {
        assertEquals(AuthError.InvalidOrExpiredCode, mapper.mapRest("otp_expired", 403, AuthOperation.VERIFY))
        assertEquals(AuthError.InvalidOrExpiredCode, mapper.mapRest("otp_expired", 403, AuthOperation.OTHER))
    }

    @Test
    fun mapsInvalidCredentialsDependingOnOperation() {
        assertEquals(AuthError.InvalidCredentials, mapper.mapRest("invalid_credentials", 400, AuthOperation.LOGIN))
        assertEquals(AuthError.InvalidOrExpiredCode, mapper.mapRest("invalid_credentials", 400, AuthOperation.VERIFY))
        assertEquals(AuthError.ServerError, mapper.mapRest("invalid_credentials", 400, AuthOperation.OTHER))
    }

    @Test
    fun mapsServerErrorsToServerErrorEvenInLogin() {
        assertEquals(AuthError.ServerError, mapper.mapRest(null, 500, AuthOperation.LOGIN))
        assertEquals(AuthError.ServerError, mapper.mapRest("unexpected_failure", 503, AuthOperation.OTHER))
    }

    @Test
    fun mapsOther4xxInLoginToInvalidCredentials() {
        assertEquals(AuthError.InvalidCredentials, mapper.mapRest(null, 429, AuthOperation.LOGIN))
        assertEquals(AuthError.InvalidCredentials, mapper.mapRest("over_request_rate_limit", 429, AuthOperation.LOGIN))
    }

    @Test
    fun mapsRateLimitOutsideLoginToServerError() {
        assertEquals(AuthError.ServerError, mapper.mapRest("over_request_rate_limit", 429, AuthOperation.OTHER))
    }

    @Test
    fun mapsIoExceptionAndTimeoutToNoConnection() {
        assertEquals(AuthError.NoConnection, mapper.mapTransport(IOException("sin red")))
        assertEquals(AuthError.NoConnection, mapper.mapTransport(SocketTimeoutException("timeout")))
    }

    @Test
    fun mapsWrappedIoExceptionToNoConnection() {
        assertEquals(AuthError.NoConnection, mapper.mapTransport(IllegalStateException("envuelta", IOException())))
    }

    @Test
    fun mapsUnknownThrowableToServerError() {
        assertEquals(AuthError.ServerError, mapper.mapTransport(IllegalArgumentException("otro")))
    }
}
