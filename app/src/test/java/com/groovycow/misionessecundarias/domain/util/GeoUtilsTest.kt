package com.groovycow.misionessecundarias.domain.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GeoUtilsTest {
    @Test
    fun `parseWktPoint returns valid GeoPoint for correct WKT string`() {
        val wkt = "SRID=4326;POINT(-76.5320 3.4516)"
        val result = GeoUtils.parseWktPoint(wkt)

        assertEquals(3.4516, result?.latitude ?: 0.0, 0.0001)
        assertEquals(-76.5320, result?.longitude ?: 0.0, 0.0001)
    }

    @Test
    fun `parseWktPoint returns null for null input`() {
        assertNull(GeoUtils.parseWktPoint(null))
    }

    @Test
    fun `parseWktPoint returns null for malformed WKT string`() {
        assertNull(GeoUtils.parseWktPoint("INVALID_STRING"))
    }

    @Test
    fun `calculateDistanceInKm returns 0 for the same point`() {
        val distance = GeoUtils.calculateDistanceInKm(3.4516, -76.5320, 3.4516, -76.5320)
        assertEquals(0.0, distance, 0.0001)
    }

    @Test
    fun `calculateDistanceInKm returns correct distance for known coordinates`() {
        // Distancia aproximada entre Cali y Bogotá: ~ 300 km
        val caliLat = 3.4516
        val caliLon = -76.5320
        val bogotaLat = 4.7110
        val bogotaLon = -74.0721

        val distance = GeoUtils.calculateDistanceInKm(caliLat, caliLon, bogotaLat, bogotaLon)

        // Aceptamos un margen de error de 5 km por la fórmula de Haversine
        assertEquals(303.0, distance, 5.0)
    }
}
