package com.groovycow.misionessecundarias.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AvatarIdTest {
    @Test
    fun catalogHasEightUniqueKeysMatchingDbFormat() {
        assertEquals(8, AvatarId.entries.size)
        assertEquals(8, AvatarId.entries.map { it.key }.toSet().size)
        assertEquals(true, AvatarId.entries.all { Regex("^[a-z_]{1,30}$").matches(it.key) })
    }

    @Test
    fun fromKeyRoundTripsAndIgnoresUnknown() {
        AvatarId.entries.forEach { assertEquals(it, AvatarId.fromKey(it.key)) }
        assertNull(AvatarId.fromKey("desconocido"))
        assertNull(AvatarId.fromKey(null))
    }

    @Test
    fun genderFromDbValue() {
        assertEquals(Gender.FEMENINO, Gender.fromDbValue("femenino"))
        assertEquals(Gender.MASCULINO, Gender.fromDbValue("masculino"))
        assertEquals(Gender.PREFIERO_NO_DECIR, Gender.fromDbValue("prefiero_no_decir"))
        assertNull(Gender.fromDbValue("otro"))
        assertNull(Gender.fromDbValue(null))
    }
}
