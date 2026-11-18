package com.groovycow.misionessecundarias.domain.repository

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
}
