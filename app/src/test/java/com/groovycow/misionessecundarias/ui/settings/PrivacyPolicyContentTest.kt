package com.groovycow.misionessecundarias.ui.settings

import org.junit.Assert.assertTrue
import org.junit.Test

/** Pruebas del contenido informativo de HU-122. */
class PrivacyPolicyContentTest {
    @Test
    fun includesLocationAndPrivacyTopics() {
        val sections = PRIVACY_POLICY_SECTIONS

        assertTrue(sections.isNotEmpty())
        assertTrue(sections.any { it.title.contains("Ubicación") })
        assertTrue(sections.any { it.title.contains("privado", ignoreCase = true) || it.title.contains("Visibilidad") })
        assertTrue(
            sections.joinToString(" ") { it.body }
                .contains("dirección exacta", ignoreCase = true),
        )
    }

    @Test
    fun everySectionHasTitleAndBody() {
        PRIVACY_POLICY_SECTIONS.forEach { section ->
            assertTrue(section.title.isNotBlank())
            assertTrue(section.body.isNotBlank())
        }
    }
}
