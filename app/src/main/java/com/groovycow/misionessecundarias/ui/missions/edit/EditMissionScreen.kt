package com.groovycow.misionessecundarias.ui.missions.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.groovycow.misionessecundarias.domain.model.Mission
import com.groovycow.misionessecundarias.ui.missions.toMessage
import org.koin.androidx.compose.koinViewModel

/**
 * Edición de los datos descriptivos de una misión publicada sin participantes (HU-032).
 *
 * @param mission Misión seleccionada por el publicador.
 * @param onBack Acción para volver al detalle sin guardar.
 * @param onSaved Acción al guardar correctamente, con la misión actualizada.
 * @param viewModel ViewModel del formulario.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditMissionScreen(
    mission: Mission,
    onBack: () -> Unit,
    onSaved: (Mission) -> Unit,
    viewModel: EditMissionViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(mission.id) { viewModel.load(mission) }

    val savedMission = state.savedMission
    LaunchedEffect(savedMission) {
        savedMission?.let(onSaved)
    }

    Scaffold(
        containerColor = SandBackground,
        topBar = {
            TopAppBar(
                title = { Text("Editar misión", color = ForestGreen, fontWeight = FontWeight.ExtraBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = EarthOrange,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SandBackground),
            )
        },
    ) { paddingValues ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "Solo puedes editar mientras no haya participantes aceptados.",
                color = BarkBrown,
                style = MaterialTheme.typography.bodyMedium,
            )
            EditField(
                label = "Título",
                value = state.title,
                onValueChange = viewModel::onTitleChange,
                enabled = !state.isSaving,
            )
            EditField(
                label = "Descripción",
                value = state.description,
                onValueChange = viewModel::onDescriptionChange,
                singleLine = false,
                enabled = !state.isSaving,
            )
            EditField(
                label = "Requisitos",
                value = state.requirements,
                onValueChange = viewModel::onRequirementsChange,
                singleLine = false,
                enabled = !state.isSaving,
            )
            EditField(
                label = "Categoría (opcional)",
                value = state.category,
                onValueChange = viewModel::onCategoryChange,
                enabled = !state.isSaving,
            )
            EditField(
                label = "Recompensa informativa (opcional)",
                value = state.reward,
                onValueChange = viewModel::onRewardChange,
                singleLine = false,
                enabled = !state.isSaving,
            )
            Text(
                text = "Las recompensas son informativas: la plataforma no procesa pagos ni transacciones económicas.",
                color = BarkBrown,
                style = MaterialTheme.typography.bodySmall,
            )

            state.error?.let { error ->
                Text(error.toMessage(), color = AlertCoral)
            }

            Spacer(modifier = Modifier.height(4.dp))
            Button(
                onClick = viewModel::save,
                enabled = !state.isSaving,
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                } else {
                    Text("Guardar cambios", fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

@Composable
private fun EditField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    singleLine: Boolean = true,
    enabled: Boolean = true,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 3,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
    )
}

private val ForestGreen = Color(0xFF214E34)
private val EarthOrange = Color(0xFFE58A3A)
private val SandBackground = Color(0xFFF7F3E8)
private val BarkBrown = Color(0xFF594A3C)
private val AlertCoral = Color(0xFFE85D3F)
