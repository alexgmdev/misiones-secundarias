package com.groovycow.misionessecundarias.ui.missions.rating

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.groovycow.misionessecundarias.domain.model.PublisherReputation
import com.groovycow.misionessecundarias.domain.usecase.GetPublisherReputationUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Estado de la reputación pública del publicador mostrada en el detalle (HU-124). */
data class PublisherReputationUiState(
    /** Reputación acumulada; `null` mientras carga o si no hay calificaciones. */
    val reputation: PublisherReputation? = null,
    /** Indica que la consulta está en curso. */
    val isLoading: Boolean = false,
)

/** Carga la reputación pública de un publicador. */
class PublisherReputationViewModel(
    private val getPublisherReputationUseCase: GetPublisherReputationUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(PublisherReputationUiState())

    /** Estado actual de la reputación. */
    val uiState: StateFlow<PublisherReputationUiState> = _uiState.asStateFlow()

    /** Consulta la reputación de [publisherId]. */
    fun load(publisherId: String) {
        if (publisherId.isBlank()) return
        _uiState.value = PublisherReputationUiState(isLoading = true)
        viewModelScope.launch {
            getPublisherReputationUseCase(publisherId).fold(
                onSuccess = { reputation ->
                    _uiState.value = PublisherReputationUiState(reputation = reputation)
                },
                onFailure = {
                    _uiState.value = PublisherReputationUiState()
                },
            )
        }
    }
}
