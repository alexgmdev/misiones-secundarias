package com.groovycow.misionessecundarias.domain.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationValidatorTest {
    @Test
    fun `isWithinRange returns true when distance is less than 50 meters`() {
        // Coordenadas base de Cali
        val userLat = 3.4516
        val userLon = -76.5320
        // Punto de misión a ~11 metros (0.0001 grados)
        val missionWkt = "SRID=4326;POINT(-76.5320 3.4517)"

        val result = LocationValidator.isWithinRange(userLat, userLon, missionWkt)

        assertTrue("Debería estar dentro del rango de 50m", result)
    }

    @Test
    fun `isWithinRange returns false when distance is greater than 50 meters`() {
        val userLat = 3.4516
        val userLon = -76.5320
        // Punto de misión a ~111 metros (0.001 grados)
        val missionWkt = "SRID=4326;POINT(-76.5320 3.4526)"

        val result = LocationValidator.isWithinRange(userLat, userLon, missionWkt)

        assertFalse("NO debería estar dentro del rango de 50m", result)
    }

    @Test
    fun `isWithinRange returns false when missionWkt is null`() {
        val result =
            LocationValidator.isWithinRange(
                3.4516,
                -76.5320,
                null,
            )

        assertFalse(
            "No se debe permitir validar la llegada sin coordenadas de misión",
            result,
        )
    }
}
