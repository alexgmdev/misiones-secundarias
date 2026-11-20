
package com.groovycow.misionessecundarias.di

import com.groovycow.misionessecundarias.data.repository.ApplicationRepositoryImpl
import com.groovycow.misionessecundarias.data.repository.ChecklistRepositoryImpl
import com.groovycow.misionessecundarias.data.repository.LogbookRepositoryImpl
import com.groovycow.misionessecundarias.data.repository.MissionRepositoryImpl
import com.groovycow.misionessecundarias.data.repository.PauseRepositoryImpl
import com.groovycow.misionessecundarias.data.repository.RatingRepositoryImpl
import com.groovycow.misionessecundarias.domain.repository.ApplicationRepository
import com.groovycow.misionessecundarias.domain.repository.ChecklistRepository
import com.groovycow.misionessecundarias.domain.repository.LogbookRepository
import com.groovycow.misionessecundarias.domain.repository.MissionRepository
import com.groovycow.misionessecundarias.domain.repository.PauseRepository
import com.groovycow.misionessecundarias.domain.repository.RatingRepository
import com.groovycow.misionessecundarias.domain.usecase.AddChecklistItemUseCase
import com.groovycow.misionessecundarias.domain.usecase.ApplyToMissionUseCase
import com.groovycow.misionessecundarias.domain.usecase.CancelApplicationUseCase
import com.groovycow.misionessecundarias.domain.usecase.CheckMissionOwnershipUseCase
import com.groovycow.misionessecundarias.domain.usecase.CompleteMissionUseCase
import com.groovycow.misionessecundarias.domain.usecase.CreateMissionUseCase
import com.groovycow.misionessecundarias.domain.usecase.DeleteChecklistItemUseCase
import com.groovycow.misionessecundarias.domain.usecase.GenerateVerificationCodeUseCase
import com.groovycow.misionessecundarias.domain.usecase.GetActivePausesUseCase
import com.groovycow.misionessecundarias.domain.usecase.GetApplicantPublicProfileUseCase
import com.groovycow.misionessecundarias.domain.usecase.GetChecklistUseCase
import com.groovycow.misionessecundarias.domain.usecase.GetLivePanelParticipantsUseCase
import com.groovycow.misionessecundarias.domain.usecase.GetMissionApplicantsUseCase
import com.groovycow.misionessecundarias.domain.usecase.GetMissionsUseCase
import com.groovycow.misionessecundarias.domain.usecase.GetMyApplicationsUseCase
import com.groovycow.misionessecundarias.domain.usecase.GetMyMissionsUseCase
import com.groovycow.misionessecundarias.domain.usecase.GetPublisherReputationUseCase
import com.groovycow.misionessecundarias.domain.usecase.MarkArrivedUseCase
import com.groovycow.misionessecundarias.domain.usecase.MarkOnWayUseCase
import com.groovycow.misionessecundarias.domain.usecase.PublishMissionUseCase
import com.groovycow.misionessecundarias.domain.usecase.RatePublisherUseCase
import com.groovycow.misionessecundarias.domain.usecase.SetChecklistItemDoneUseCase
import com.groovycow.misionessecundarias.domain.usecase.SetMissionPauseUseCase
import com.groovycow.misionessecundarias.domain.usecase.StartMissionUseCase
import com.groovycow.misionessecundarias.domain.usecase.UpdateApplicationStatusUseCase
import com.groovycow.misionessecundarias.domain.usecase.UpdateMissionDetailsUseCase
import com.groovycow.misionessecundarias.domain.validation.MissionDraftValidator
import com.groovycow.misionessecundarias.ui.applications.ApplicationViewModel
import com.groovycow.misionessecundarias.ui.applications.MyApplicationsViewModel
import com.groovycow.misionessecundarias.ui.missions.ApplicantsViewModel
import com.groovycow.misionessecundarias.ui.missions.MissionViewModel
import com.groovycow.misionessecundarias.ui.missions.PublicProfileViewModel
import com.groovycow.misionessecundarias.ui.missions.create.CreateMissionViewModel
import com.groovycow.misionessecundarias.ui.missions.edit.EditMissionViewModel
import com.groovycow.misionessecundarias.ui.missions.live.ChecklistViewModel
import com.groovycow.misionessecundarias.ui.missions.live.CompleteMissionViewModel
import com.groovycow.misionessecundarias.ui.missions.live.GenerateCodeViewModel
import com.groovycow.misionessecundarias.ui.missions.live.LivePanelViewModel
import com.groovycow.misionessecundarias.ui.missions.live.PausesViewModel
import com.groovycow.misionessecundarias.ui.missions.live.StartMissionViewModel
import com.groovycow.misionessecundarias.ui.missions.publish.PublishMissionViewModel
import com.groovycow.misionessecundarias.ui.missions.rating.PublisherReputationViewModel
import com.groovycow.misionessecundarias.ui.missions.rating.RatePublisherViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import java.time.Clock
import java.time.ZoneId

val repositoryModule =
    module {
        // Repositorios
        single<MissionRepository> { MissionRepositoryImpl(get()) }
        single<ApplicationRepository> { ApplicationRepositoryImpl(get()) }
        single<ChecklistRepository> { ChecklistRepositoryImpl(get()) }
        single<PauseRepository> { PauseRepositoryImpl(get()) }
        single<LogbookRepository> { LogbookRepositoryImpl(get()) }
        single<RatingRepository> { RatingRepositoryImpl(get()) }

        // Casos de Uso
        factory { GetMissionsUseCase(get()) }
        factory { CreateMissionUseCase(get()) }
        factory { GetMyMissionsUseCase(get()) }
        factory { PublishMissionUseCase(get(), Clock.system(ZoneId.of("America/Bogota"))) }
        factory { ApplyToMissionUseCase(get()) }
        factory { CancelApplicationUseCase(get()) }
        factory { GetMyApplicationsUseCase(get()) }
        factory { CheckMissionOwnershipUseCase(get()) }
        factory { GetMissionApplicantsUseCase(get()) }
        factory { UpdateApplicationStatusUseCase(get()) }
        factory { GetLivePanelParticipantsUseCase(get()) }
        factory { GetApplicantPublicProfileUseCase(get()) }
        factory { GetChecklistUseCase(get()) }
        factory { GetActivePausesUseCase(get()) }
        factory { SetMissionPauseUseCase(get()) }
        factory { AddChecklistItemUseCase(get()) }
        factory { SetChecklistItemDoneUseCase(get()) }
        factory { DeleteChecklistItemUseCase(get()) }
        factory { StartMissionUseCase(get(), get(), Clock.system(ZoneId.of("America/Bogota"))) }
        factory { CompleteMissionUseCase(get(), get()) }
        factory { GenerateVerificationCodeUseCase(get()) }
        factory { com.groovycow.misionessecundarias.domain.usecase.SaveMissionLogbookUseCase(get()) }
        factory { MissionDraftValidator(Clock.system(ZoneId.of("America/Bogota"))) }
        factory { MarkOnWayUseCase(get()) }
        factory { MarkArrivedUseCase(get()) }
        factory { UpdateMissionDetailsUseCase(get(), get()) }
        factory { RatePublisherUseCase(get(), get(), get()) }
        factory { GetPublisherReputationUseCase(get()) }

        // ViewModels
        viewModel { MissionViewModel(get(), get()) }
        viewModel { CreateMissionViewModel(get(), get()) }
        viewModel { PublishMissionViewModel(get()) }
        viewModel { EditMissionViewModel(get()) }
        viewModel { RatePublisherViewModel(get()) }
        viewModel { PublisherReputationViewModel(get()) }
        viewModel { ApplicationViewModel(get(), get(), get(), get(), get()) }
        viewModel { MyApplicationsViewModel(get(), get(), get()) }
        viewModel { ApplicantsViewModel(get(), get()) }
        viewModel { PublicProfileViewModel(get()) }
        viewModel { LivePanelViewModel(get()) }
        viewModel { ChecklistViewModel(get(), get(), get(), get()) }
        viewModel { PausesViewModel(get(), get()) }
        viewModel { StartMissionViewModel(get()) }
        viewModel { CompleteMissionViewModel(get()) }
        viewModel { GenerateCodeViewModel(get()) }
        viewModel { com.groovycow.misionessecundarias.ui.missions.live.MissionLogbookViewModel(get()) }
    }
