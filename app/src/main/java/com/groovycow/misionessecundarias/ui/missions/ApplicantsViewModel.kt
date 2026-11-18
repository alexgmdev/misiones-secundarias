package com.groovycow.misionessecundarias.ui.missions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.groovycow.misionessecundarias.domain.model.ApplicationDecision
import com.groovycow.misionessecundarias.domain.model.MissionApplicant
import com.groovycow.misionessecundarias.domain.usecase.GetMissionApplicantsUseCase
import com.groovycow.misionessecundarias.domain.usecase.UpdateApplicationStatusUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Estado de la gestión de postulantes de una misión propia. */
data class ApplicantsUiState(
    val isLoading: Boolean = false,
    val applicants: List<MissionApplicant> = emptyList(),
    val error: String? = null,
    val updatingApplicationId: String? = null,
    val actionMessage: String? = null,
    val isActionError: Boolean = false,
)

/** Carga postulantes y registra la decisión del publicador. */
class ApplicantsViewModel(
    private val getMissionApplicants: GetMissionApplicantsUseCase,
    private val updateApplicationStatus: UpdateApplicationStatusUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ApplicantsUiState())
    val uiState: StateFlow<ApplicantsUiState> = _uiState.asStateFlow()

    fun loadApplicants(missionId: String) {
        viewModelScope.launch {
            _uiState.value = ApplicantsUiState(isLoading = true)
            getMissionApplicants(missionId).fold(
                onSuccess = { applicants ->
                    _uiState.value = ApplicantsUiState(applicants = applicants)
                },
                onFailure = { exception ->
                    _uiState.value =
                        ApplicantsUiState(
                            error = exception.toApplicantsErrorMessage(isUpdate = false),
                        )
                },
            )
        }
    }

    fun decide(
        applicationId: String,
        decision: ApplicationDecision,
    ) {
        if (_uiState.value.updatingApplicationId != null) return

        viewModelScope.launch {
            _uiState.value =
                _uiState.value.copy(
                    updatingApplicationId = applicationId,
                    actionMessage = null,
                    isActionError = false,
                )

            updateApplicationStatus(applicationId, decision).fold(
                onSuccess = {
                    _uiState.value =
                        _uiState.value.copy(
                            applicants =
                                _uiState.value.applicants.map { applicant ->
                                    if (applicant.id == applicationId) {
                                        applicant.copy(status = decision.databaseValue)
                                    } else {
                                        applicant
                                    }
                                },
                            updatingApplicationId = null,
                            actionMessage =
                                if (decision == ApplicationDecision.ACCEPT) {
                                    "La postulación fue aceptada."
                                } else {
                                    "La postulación fue rechazada."
                                },
                        )
                },
                onFailure = { exception ->
                    _uiState.value =
                        _uiState.value.copy(
                            updatingApplicationId = null,
                            actionMessage = exception.toApplicantsErrorMessage(isUpdate = true),
                            isActionError = true,
                        )
                },
            )
        }
    }

    fun dismissNotice() {
        _uiState.value =
            _uiState.value.copy(
                error = null,
                actionMessage = null,
                isActionError = false,
            )
    }
}

internal fun Throwable.toApplicantsErrorMessage(isUpdate: Boolean): String {
    val rawMessage = message.orEmpty()
    val fallback =
        if (isUpdate) {
            "No fue posible guardar la decisión. Inténtalo nuevamente."
        } else {
            "No fue posible consultar los postulantes. Inténtalo nuevamente."
        }

    return when {
        rawMessage.contains("permission denied for table applications", ignoreCase = true) ->
            "No fue posible guardar la decisión porque faltan permisos en la base de datos."

        rawMessage.contains("Headers:", ignoreCase = true) ||
            rawMessage.contains("Authorization=Bearer", ignoreCase = true) ||
            rawMessage.contains("URL:", ignoreCase = true) -> fallback

        rawMessage.isBlank() -> fallback
        else -> rawMessage
    }
}
