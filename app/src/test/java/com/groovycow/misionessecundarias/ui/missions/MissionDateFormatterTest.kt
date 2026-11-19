package com.groovycow.misionessecundarias.ui.missions

import org.junit.Assert.assertEquals
import org.junit.Test

class MissionDateFormatterTest {
    @Test
    fun shouldDisplayUtcTimestampInBogotaTime() {
        val formatted = formatMissionDateTime("2026-11-15T15:00:00Z")

        assertEquals("domingo 15 de noviembre a las 10:00", formatted)
    }
}
