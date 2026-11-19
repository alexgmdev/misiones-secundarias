package com.groovycow.misionessecundarias.domain.usecase

import com.groovycow.misionessecundarias.domain.model.AvatarId
import com.groovycow.misionessecundarias.domain.model.Gender
import com.groovycow.misionessecundarias.domain.model.Profile
import com.groovycow.misionessecundarias.domain.model.ProfileError
import com.groovycow.misionessecundarias.domain.model.ProfileException
import com.groovycow.misionessecundarias.domain.model.PublicProfile
import com.groovycow.misionessecundarias.domain.repository.PhotoCompressor
import com.groovycow.misionessecundarias.domain.repository.ProfileRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import kotlin.coroutines.cancellation.CancellationException

class ProfileSimpleUseCasesTest {
    private val repository = mockk<ProfileRepository>()
    private val compressor = mockk<PhotoCompressor>()
    private val profile = Profile("u1", "Ana", Gender.FEMENINO, null, AvatarId.entries.first(), "url", true)
    private val publicProfile = PublicProfile("u2", "Beto", Gender.MASCULINO, "hola", null, null)

    @Test
    fun isProfileCompleteReturnsRepositoryValue() =
        runTest {
            coEvery { repository.isProfileComplete() } returns false
            assertEquals(Result.success(false), IsProfileCompleteUseCase(repository)())
        }

    @Test
    fun isProfileCompleteWrapsFailureAndRethrowsCancellation() =
        runTest {
            val boom = ProfileException(ProfileError.NoConnection)
            coEvery { repository.isProfileComplete() } throws boom
            assertEquals(boom, IsProfileCompleteUseCase(repository)().exceptionOrNull())
            coEvery { repository.isProfileComplete() } throws CancellationException("c")
            try {
                IsProfileCompleteUseCase(repository)()
                fail("Debía relanzar la cancelación")
            } catch (e: CancellationException) {
                assertEquals("c", e.message)
            }
        }

    @Test
    fun getMyProfileSuccessAndFailure() =
        runTest {
            coEvery { repository.getMyProfile() } returns profile
            assertEquals(Result.success(profile), GetMyProfileUseCase(repository)())
            coEvery { repository.getMyProfile() } throws ProfileException(ProfileError.ServerError)
            val error = (GetMyProfileUseCase(repository)().exceptionOrNull() as ProfileException).error
            assertEquals(ProfileError.ServerError, error)
        }

    @Test
    fun getPublicProfileSuccessAndFailure() =
        runTest {
            coEvery { repository.getPublicProfile("u2") } returns publicProfile
            assertEquals(Result.success(publicProfile), GetPublicProfileUseCase(repository)("u2"))
            coEvery { repository.getPublicProfile("u2") } throws ProfileException(ProfileError.NoConnection)
            val error = (GetPublicProfileUseCase(repository)("u2").exceptionOrNull() as ProfileException).error
            assertEquals(ProfileError.NoConnection, error)
        }

    @Test
    fun uploadProfilePhotoReturnsUrl() =
        runTest {
            coEvery { repository.uploadPhoto(any()) } returns "https://x/avatars/u1/foto.jpg?v=1"
            val result = UploadProfilePhotoUseCase(repository)(byteArrayOf(1, 2))
            assertEquals("https://x/avatars/u1/foto.jpg?v=1", result.getOrNull())
        }

    @Test
    fun uploadProfilePhotoFailure() =
        runTest {
            coEvery { repository.uploadPhoto(any()) } throws ProfileException(ProfileError.NoConnection)
            assertTrue(UploadProfilePhotoUseCase(repository)(byteArrayOf(1)).isFailure)
        }

    @Test
    fun compressProfilePhotoDelegatesAndPropagatesErrors() =
        runTest {
            coEvery { compressor.compressToJpeg("content://a") } returns byteArrayOf(9)
            assertArrayEquals(byteArrayOf(9), CompressProfilePhotoUseCase(compressor)("content://a").getOrNull())
            coEvery { compressor.compressToJpeg("content://b") } throws ProfileException(ProfileError.UnsupportedPhotoFormat)
            val error = (CompressProfilePhotoUseCase(compressor)("content://b").exceptionOrNull() as ProfileException).error
            assertEquals(ProfileError.UnsupportedPhotoFormat, error)
        }
}
