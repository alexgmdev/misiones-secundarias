
package com.groovycow.misionessecundarias.domain.util

object LocationValidator {
    private const val MAX_DISTANCE_METERS = 50.0

    fun isWithinRange(
        userLat: Double,
        userLon: Double,
        missionWkt: String?,
        maxDistanceMeters: Double = MAX_DISTANCE_METERS,
    ): Boolean {
        return missionWkt?.let { wkt ->
            GeoUtils.parseWktPoint(wkt)?.let { missionPoint ->

                val distanceKm =
                    GeoUtils.calculateDistanceInKm(
                        userLat,
                        userLon,
                        missionPoint.latitude,
                        missionPoint.longitude,
                    )

                (distanceKm * 1000.0) <= maxDistanceMeters
            }
        } ?: false
    }
}
