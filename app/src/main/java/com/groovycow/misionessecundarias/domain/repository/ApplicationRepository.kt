package com.groovycow.misionessecundarias.domain.repository

import com.groovycow.misionessecundarias.domain.model.Application
import com.groovycow.misionessecundarias.domain.model.MyApplication

interface ApplicationRepository {
    suspend fun applyToMission(missionId: String): Application
    suspend fun getMyApplications(): List<MyApplication>
    suspend fun cancelApplication(applicationId: String)
}
