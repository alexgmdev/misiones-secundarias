package com.groovycow.misionessecundarias.domain.util

// Esta es una clase simple para no depender de MapLibre en el dominio
data class GeoPoint(val latitude: Double, val longitude: Double)

fun parseWktPoint(wkt: String?): GeoPoint? {
    if (wkt == null || !wkt.contains("POINT")) return null
    return try {
        // Ejemplo: "SRID=4326;POINT(-76.532 3.451)"
        val coordsString = wkt.substringAfter("POINT(").substringBefore(")")
        val parts = coordsString.split(" ")

        // ⚠️ IMPORTANTE: En PostGIS WKT, el orden es LONGITUD y luego LATITUD (X, Y)
        val longitude = parts[0].toDouble()
        val latitude = parts[1].toDouble()

        GeoPoint(latitude, longitude)
    } catch (e: Exception) {
        null // Si el formato falla, devolvemos null para no romper la app
    }
}