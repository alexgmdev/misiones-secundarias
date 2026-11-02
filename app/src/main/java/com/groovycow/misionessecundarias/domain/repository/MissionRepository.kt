package com.groovycow.misionessecundarias.domain.repository

import com.groovycow.misionessecundarias.domain.model.Mission

interface MissionRepository {
    suspend fun getPublishedMissions(): List<Mission>
    suspend fun getMissionById(id: String): Mission?
}