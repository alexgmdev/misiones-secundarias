package com.groovycow.misionessecundarias.domain.repository

import com.groovycow.misionessecundarias.data.repository.ApplicationStatusDto
import com.groovycow.misionessecundarias.domain.model.Application
import com.groovycow.misionessecundarias.domain.model.ApplicationDecision
import com.groovycow.misionessecundarias.domain.model.MissionApplicant
import com.groovycow.misionessecundarias.domain.model.MyApplication

interface ApplicationRepository {
    suspend fun applyToMission(missionId: String): Application

    suspend fun getMyApplications(): List<MyApplication>

    suspend fun cancelApplication(applicationId: String)

    suspend fun isCurrentUser(userId: String): Boolean

    suspend fun getMissionApplicants(missionId: String): List<MissionApplicant>

    suspend fun updateApplicationStatus(
        applicationId: String,
        decision: ApplicationDecision,
    )

    suspend fun markOnWay(missionId: String): Result<Unit>

    suspend fun markArrived(
        missionId: String,
        latitude: Double?,
        longitude: Double?,
    ): Result<Unit>

    suspend fun getMyApplicationStatus(missionId: String): ApplicationStatusDto?
}
