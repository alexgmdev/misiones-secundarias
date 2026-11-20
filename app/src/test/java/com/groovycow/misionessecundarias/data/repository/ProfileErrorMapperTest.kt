package com.groovycow.misionessecundarias.data.repository

import com.groovycow.misionessecundarias.domain.model.ProfileError
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException
import java.net.SocketTimeoutException

class ProfileErrorMapperTest {
    private val mapper = ProfileErrorMapper()

    @Test
    fun uniqueViolationMapsToNicknameTaken() {
        val body = """{"code":"23505","message":"duplicate key value violates unique constraint"}"""
        assertEquals(ProfileError.NicknameTaken, mapper.mapRest(body, 409))
    }

    @Test
    fun payloadTooLargeMapsToPhotoTooLarge() {
        assertEquals(ProfileError.PhotoTooLarge, mapper.mapRest("{}", 413))
    }

    @Test
    fun unauthorizedMapsToSessionRequired() {
        assertEquals(ProfileError.SessionRequired, mapper.mapRest(null, 401))
    }

    @Test
    fun otherRestErrorsMapToServerError() {
        assertEquals(ProfileError.ServerError, mapper.mapRest("""{"code":"42501"}""", 403))
        assertEquals(ProfileError.ServerError, mapper.mapRest(null, 500))
    }

    @Test
    fun networkFailuresMapToNoConnection() {
        assertEquals(ProfileError.NoConnection, mapper.mapTransport(IOException("x")))
        assertEquals(ProfileError.NoConnection, mapper.mapTransport(RuntimeException(SocketTimeoutException())))
    }

    @Test
    fun unknownTransportFailureMapsToServerError() {
        assertEquals(ProfileError.ServerError, mapper.mapTransport(IllegalStateException("x")))
    }
}
