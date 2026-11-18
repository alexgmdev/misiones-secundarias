package com.groovycow.misionessecundarias.di

import com.groovycow.misionessecundarias.data.repository.ApplicationRepositoryImpl
import com.groovycow.misionessecundarias.data.repository.MissionRepositoryImpl
import com.groovycow.misionessecundarias.domain.repository.ApplicationRepository
import com.groovycow.misionessecundarias.domain.repository.MissionRepository
import com.groovycow.misionessecundarias.domain.usecase.ApplyToMissionUseCase
import com.groovycow.misionessecundarias.domain.usecase.CancelApplicationUseCase
import com.groovycow.misionessecundarias.domain.usecase.CheckMissionOwnershipUseCase
import com.groovycow.misionessecundarias.domain.usecase.CreateMissionUseCase
import com.groovycow.misionessecundarias.domain.usecase.GetMissionApplicantsUseCase
import com.groovycow.misionessecundarias.domain.usecase.GetMissionsUseCase
import com.groovycow.misionessecundarias.domain.usecase.GetMyApplicationsUseCase
import com.groovycow.misionessecundarias.domain.usecase.MarkArrivedUseCase
import com.groovycow.misionessecundarias.domain.usecase.MarkOnWayUseCase
import com.groovycow.misionessecundarias.domain.usecase.PublishMissionUseCase
import com.groovycow.misionessecundarias.domain.usecase.UpdateApplicationStatusUseCase
import com.groovycow.misionessecundarias.domain.validation.MissionDraftValidator
import com.groovycow.misionessecundarias.ui.applications.ApplicationViewModel
import com.groovycow.misionessecundarias.ui.applications.MyApplicationsViewModel
import com.groovycow.misionessecundarias.ui.missions.ApplicantsViewModel
import com.groovycow.misionessecundarias.ui.missions.MissionViewModel
import com.groovycow.misionessecundarias.ui.missions.create.CreateMissionViewModel
import com.groovycow.misionessecundarias.ui.missions.publish.PublishMissionViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import java.time.Clock
import java.time.ZoneId

val repositoryModule =
    module {
        // Repositorios
        single<MissionRepository> { MissionRepositoryImpl(get()) }
        single<ApplicationRepository> { ApplicationRepositoryImpl(get()) }

        // Casos de Uso
        factory { GetMissionsUseCase(get()) }
        factory { CreateMissionUseCase(get()) }
        factory { PublishMissionUseCase(get(), Clock.system(ZoneId.of("America/Bogota"))) }
        factory { ApplyToMissionUseCase(get()) }
        factory { CancelApplicationUseCase(get()) }
        factory { GetMyApplicationsUseCase(get()) }
        factory { CheckMissionOwnershipUseCase(get()) }
        factory { GetMissionApplicantsUseCase(get()) }
        factory { UpdateApplicationStatusUseCase(get()) }
        factory { MissionDraftValidator(Clock.system(ZoneId.of("America/Bogota"))) }
        factory { MarkOnWayUseCase(get()) }
        factory { MarkArrivedUseCase(get()) }

        viewModel { MissionViewModel(get()) }
        viewModel { CreateMissionViewModel(get(), get()) }
        viewModel { PublishMissionViewModel(get()) }
        viewModel { ApplicationViewModel(get(), get(), get(), get(), get()) }
        viewModel { MyApplicationsViewModel(get(), get()) }
        viewModel { ApplicantsViewModel(get(), get()) }
    }
