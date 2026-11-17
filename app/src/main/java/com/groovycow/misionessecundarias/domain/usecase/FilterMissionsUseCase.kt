package com.groovycow.misionessecundarias.domain.usecase

import com.groovycow.misionessecundarias.domain.model.Mission
import com.groovycow.misionessecundarias.domain.util.GeoUtils

class FilterMissionsUseCase {
    operator fun invoke(
        missions: List<Mission>,
        searchQuery: String,
        selectedModality: String?,
        selectedCategory: String?,
        selectedDistance: Int?,
        userLat: Double?,
        userLon: Double?,
    ): List<Mission> {
        return missions.filter { mission ->
            val matchesSearch =
                searchQuery.isBlank() ||
                    mission.title.contains(searchQuery, ignoreCase = true) ||
                    mission.description.contains(searchQuery, ignoreCase = true)

            val matchesModality =
                selectedModality == null ||
                    mission.modality.equals(selectedModality, ignoreCase = true)

            val matchesCategory =
                selectedCategory == null ||
                    mission.category?.trim().equals(selectedCategory, ignoreCase = true)

            var matchesDistance = true
            if (selectedDistance != null && userLat != null && userLon != null) {
                val point = GeoUtils.parseWktPoint(mission.location)
                if (point != null) {
                    val distance =
                        GeoUtils.calculateDistanceInKm(
                            userLat,
                            userLon,
                            point.latitude,
                            point.longitude,
                        )
                    matchesDistance = distance <= selectedDistance
                } else {
                    matchesDistance = false
                }
            }

            matchesSearch && matchesModality && matchesCategory && matchesDistance
        }
    }
}
