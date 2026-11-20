package com.groovycow.misionessecundarias.data.image

import com.groovycow.misionessecundarias.domain.model.ProfileError
import com.groovycow.misionessecundarias.domain.model.ProfileException
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class PhotoCompressionPolicyTest {
    @Test
    fun qualityStepsGoFrom85To45InTens() {
        assertEquals(listOf(85, 75, 65, 55, 45), PhotoCompressionPolicy.QUALITY_STEPS)
    }

    @Test
    fun smallImageIsNotScaled() {
        assertEquals(800 to 600, PhotoCompressionPolicy.targetSize(800, 600))
    }

    @Test
    fun largeLandscapeScalesLongSideTo1080() {
        assertEquals(1080 to 810, PhotoCompressionPolicy.targetSize(4320, 3240))
    }

    @Test
    fun largePortraitScalesLongSideTo1080() {
        assertEquals(810 to 1080, PhotoCompressionPolicy.targetSize(3240, 4320))
    }

    @Test
    fun sampleSizeIsLargestPowerOfTwoKeepingAtLeastTargetResolution() {
        assertEquals(1, PhotoCompressionPolicy.sampleSize(1000, 800))
        assertEquals(1, PhotoCompressionPolicy.sampleSize(2000, 1500))
        assertEquals(2, PhotoCompressionPolicy.sampleSize(2200, 1500))
        assertEquals(4, PhotoCompressionPolicy.sampleSize(4500, 3400))
    }

    @Test
    fun firstQualityThatFitsIsReturned() {
        val tried = mutableListOf<Int>()
        val result =
            PhotoCompressionPolicy.encodeWithinLimit(maxBytes = 100) { quality ->
                tried += quality
                ByteArray(if (quality > 65) 200 else 90)
            }
        assertEquals(listOf(85, 75, 65), tried)
        assertEquals(90, result.size)
    }

    @Test
    fun highestQualityIsPreferredWhenItAlreadyFits() {
        val result = PhotoCompressionPolicy.encodeWithinLimit(maxBytes = 100) { ByteArray(10) { 7 } }
        assertArrayEquals(ByteArray(10) { 7 }, result)
    }

    @Test
    fun throwsPhotoTooLargeWhenNoQualityFits() {
        try {
            PhotoCompressionPolicy.encodeWithinLimit(maxBytes = 100) { ByteArray(500) }
            fail("Debía lanzar PhotoTooLarge")
        } catch (e: ProfileException) {
            assertEquals(ProfileError.PhotoTooLarge, e.error)
        }
    }

    @Test
    fun onlyJpegAndPngAreSupported() {
        assertTrue(PhotoCompressionPolicy.isSupportedMime("image/jpeg"))
        assertTrue(PhotoCompressionPolicy.isSupportedMime("image/png"))
        assertEquals(false, PhotoCompressionPolicy.isSupportedMime("image/gif"))
        assertEquals(false, PhotoCompressionPolicy.isSupportedMime("image/webp"))
        assertEquals(false, PhotoCompressionPolicy.isSupportedMime(null))
    }
}
