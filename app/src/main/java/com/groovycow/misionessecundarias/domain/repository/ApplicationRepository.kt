package com.groovycow.misionessecundarias.domain.repository

import com.groovycow.misionessecundarias.domain.model.Application
import com.groovycow.misionessecundarias.domain.model.MyApplication
import com.groovycow.misionessecundarias.domain.model.MissionApplicant


interface ApplicationRepository {
    suspend fun applyToMission(missionId: String): Application

    suspend fun getMyApplications(): List<MyApplication>

    suspend fun cancelApplication(applicationId: String)




    // HU-041
    suspend fun getMissionApplicants(
        missionId: String
    ): List<MissionApplicant>

    // HU-044
    suspend fun acceptApplication(
        applicationId: String
    )

    // HU-045
    suspend fun rejectApplication(
        applicationId: String
    )
}
