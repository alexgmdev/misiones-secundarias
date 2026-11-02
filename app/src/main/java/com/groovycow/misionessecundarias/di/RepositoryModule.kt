package com.groovycow.misionessecundarias.di

import com.groovycow.misionessecundarias.data.repository.MissionRepositoryImpl
import com.groovycow.misionessecundarias.domain.repository.MissionRepository
import com.groovycow.misionessecundarias.domain.usecase.GetMissionsUseCase
import org.koin.dsl.module

val repositoryModule = module {
    single<MissionRepository> { MissionRepositoryImpl(get()) }
    factory { GetMissionsUseCase(get()) }
}