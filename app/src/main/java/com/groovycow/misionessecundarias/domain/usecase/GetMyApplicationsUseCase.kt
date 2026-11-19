package com.groovycow.misionessecundarias.domain.usecase

import com.groovycow.misionessecundarias.domain.model.MyApplication
import com.groovycow.misionessecundarias.domain.repository.ApplicationRepository

class GetMyApplicationsUseCase(
    private val applicationRepository: ApplicationRepository,
) {
    suspend operator fun invoke(): Result<List<MyApplication>> = runCatching { applicationRepository.getMyApplications() }
}
