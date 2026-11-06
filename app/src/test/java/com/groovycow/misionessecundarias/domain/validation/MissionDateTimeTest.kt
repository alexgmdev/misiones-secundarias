package com.groovycow.misionessecundarias.domain.validation

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class MissionDateTimeTest {
    @Test
    fun shouldSerializeDateAndTimeWithBogotaOffset() {
        val result =
            MissionDateTime.toIsoOffset(
                LocalDate.of(2026, 11, 15),
                LocalTime.of(10, 0),
            )

        assertEquals("2026-11-15T10:00:00-05:00", result)
    }
}
