package com.groovycow.misionessecundarias.domain.util

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class GeoPoint(val latitude: Double, val longitude: Double)

object GeoUtils {
    @Suppress("SwallowedException")
    fun parseWktPoint(wkt: String?): GeoPoint? {
        if (wkt == null || !wkt.contains("POINT")) return null
        return try {
            val coordsString = wkt.substringAfter("POINT(").substringBefore(")")
            val parts = coordsString.split(" ")
            val longitude = parts[0].toDouble()
            val latitude = parts[1].toDouble()
            GeoPoint(latitude, longitude)
        } catch (e: Exception) {
            null
        }
    }

    // Fórmula de Haversine (testeable en JVM)
    fun calculateDistanceInKm(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double,
    ): Double {
        val r = 6371.0
        val phi1 = Math.toRadians(lat1)
        val phi2 = Math.toRadians(lat2)
        val deltaPhi = Math.toRadians(lat2 - lat1)
        val deltaLambda = Math.toRadians(lon2 - lon1)

        val a =
            sin(deltaPhi / 2) * sin(deltaPhi / 2) +
                cos(phi1) * cos(phi2) * sin(deltaLambda / 2) * sin(deltaLambda / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return r * c
    }
}
