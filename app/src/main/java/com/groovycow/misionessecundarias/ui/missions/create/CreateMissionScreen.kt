package com.groovycow.misionessecundarias.ui.missions.create

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.groovycow.misionessecundarias.domain.model.MissionError
import com.groovycow.misionessecundarias.domain.model.MissionField
import com.groovycow.misionessecundarias.domain.model.MissionModality
import com.groovycow.misionessecundarias.ui.missions.publish.PublishMissionViewModel
import com.groovycow.misionessecundarias.ui.missions.toMessage
import org.koin.androidx.compose.koinViewModel
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Selector de modalidad y formulario para guardar un borrador (HU-016).
 *
 * @param onBack Acción para volver al menú o al selector de modalidad.
 * @param onDraftSaved Acción al cerrar la confirmación de borrador guardado.
 * @param viewModel ViewModel del formulario, obtenido con Koin.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Suppress("CyclomaticComplexMethod")
@Composable
fun CreateMissionScreen(
    onBack: () -> Unit,
    onDraftSaved: () -> Unit,
    viewModel: CreateMissionViewModel = koinViewModel(),
    publishViewModel: PublishMissionViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val publishState by publishViewModel.uiState.collectAsState()
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.reset()
        publishViewModel.reset()
    }
    BackHandler(enabled = state.modality != null && state.savedMission == null) {
        viewModel.onChangeModality()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Crear borrador", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (state.modality == null || state.savedMission != null) onBack() else viewModel.onChangeModality()
                        },
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
            )
        },
    ) { padding ->
        if (state.modality == null) {
            ModalitySelector(
                modifier = Modifier.padding(padding),
                onSelect = viewModel::onModalitySelected,
            )
        } else {
            MissionDraftForm(
                state = state,
                viewModel = viewModel,
                modifier = Modifier.padding(padding),
                onChooseDate = { showDatePicker = true },
                onChooseTime = { showTimePicker = true },
            )
        }
    }

    if (showDatePicker) {
        val selectedDateMillis =
            state.date?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            viewModel.onDateChange(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
                        }
                        showDatePicker = false
                    },
                ) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") } },
        ) {
            DatePicker(state = pickerState)
        }
    }

    if (showTimePicker) {
        val pickerState =
            rememberTimePickerState(
                initialHour = state.time?.hour ?: 9,
                initialMinute = state.time?.minute ?: 0,
                is24Hour = true,
            )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Selecciona la hora") },
            text = { TimePicker(state = pickerState) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.onTimeChange(LocalTime.of(pickerState.hour, pickerState.minute))
                        showTimePicker = false
                    },
                ) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("Cancelar") } },
        )
    }

    val savedMission = state.savedMission
    if (savedMission != null) {
        val isPublished = publishState.publishedMission != null
        AlertDialog(
            onDismissRequest = onDraftSaved,
            title = { Text(if (isPublished) "Misión publicada" else "Borrador guardado") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        if (isPublished) {
                            "Misión publicada correctamente"
                        } else {
                            "La misión quedó guardada como borrador y aún no es visible en la búsqueda pública."
                        },
                    )
                    publishState.error?.let { error ->
                        Text(error.toMessage(), color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                if (isPublished) {
                    TextButton(onClick = onDraftSaved) { Text("Volver al menú") }
                } else {
                    TextButton(
                        onClick = { publishViewModel.publish(savedMission) },
                        enabled = !publishState.isPublishing,
                    ) {
                        if (publishState.isPublishing) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Publicar Misión")
                        }
                    }
                }
            },
            dismissButton = {
                if (!isPublished) {
                    TextButton(
                        onClick = {
                            viewModel.reset()
                            publishViewModel.reset()
                        },
                        enabled = !publishState.isPublishing,
                    ) { Text("Crear otro") }
                }
            },
        )
    }
}

@Composable
private fun ModalitySelector(
    modifier: Modifier,
    onSelect: (MissionModality) -> Unit,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            text = "¿Qué tipo de misión quieres crear?",
            style = MaterialTheme.typography.headlineSmall,
            color = ForestGreen,
            fontWeight = FontWeight.Bold,
        )
        Text("Elige una modalidad para ver los campos correspondientes.")
        ModalityCard(
            title = "Misión de Campo",
            description = "Actividad presencial con punto de encuentro y coordenadas.",
            onClick = { onSelect(MissionModality.CAMPO) },
        )
        ModalityCard(
            title = "Misión Remota",
            description = "Actividad que se realiza a distancia, sin ubicación física.",
            onClick = { onSelect(MissionModality.REMOTA) },
        )
    }
}

@Composable
private fun ModalityCard(
    title: String,
    description: String,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CreamSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(title, color = ForestGreen, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(4.dp))
            Text(description, color = BarkBrown)
        }
    }
}

@Composable
private fun MissionDraftForm(
    state: CreateMissionUiState,
    viewModel: CreateMissionViewModel,
    modifier: Modifier,
    onChooseDate: () -> Unit,
    onChooseTime: () -> Unit,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = if (state.modality == MissionModality.CAMPO) "Misión de Campo" else "Misión Remota",
            style = MaterialTheme.typography.titleLarge,
            color = ForestGreen,
            fontWeight = FontWeight.Bold,
        )
        TextButton(onClick = viewModel::onChangeModality, enabled = !state.isSaving) {
            Text("Cambiar modalidad")
        }

        MissionInputField(
            label = "Título",
            value = state.title,
            onValueChange = viewModel::onTitleChange,
            error = state.fieldErrors[MissionField.TITLE],
            enabled = !state.isSaving,
        )
        MissionInputField(
            label = "Descripción",
            value = state.description,
            onValueChange = viewModel::onDescriptionChange,
            error = state.fieldErrors[MissionField.DESCRIPTION],
            singleLine = false,
            enabled = !state.isSaving,
        )
        MissionInputField(
            label = "Requisitos (ropa, agua, materiales)",
            value = state.requirements,
            onValueChange = viewModel::onRequirementsChange,
            error = state.fieldErrors[MissionField.REQUIREMENTS],
            singleLine = false,
            enabled = !state.isSaving,
        )
        MissionInputField(
            label = "Categoría (opcional)",
            value = state.category,
            onValueChange = viewModel::onCategoryChange,
            enabled = !state.isSaving,
        )

        if (state.modality == MissionModality.CAMPO) {
            MissionInputField(
                label = "Punto de encuentro",
                value = state.meetingPoint,
                onValueChange = viewModel::onMeetingPointChange,
                error = state.fieldErrors[MissionField.MEETING_POINT],
                placeholder = "Parque Dog Cali",
                enabled = !state.isSaving,
            )
            Text(
                text = "Ubicación aproximada en Cali. Las coordenadas también se podrán elegir en un mapa en una versión futura.",
                style = MaterialTheme.typography.bodySmall,
                color = BarkBrown,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MissionInputField(
                    label = "Latitud",
                    value = state.latitude,
                    onValueChange = viewModel::onLatitudeChange,
                    error = state.fieldErrors[MissionField.LATITUDE],
                    keyboardType = KeyboardType.Decimal,
                    placeholder = "3.45",
                    modifier = Modifier.weight(1f),
                    enabled = !state.isSaving,
                )
                MissionInputField(
                    label = "Longitud",
                    value = state.longitude,
                    onValueChange = viewModel::onLongitudeChange,
                    error = state.fieldErrors[MissionField.LONGITUDE],
                    keyboardType = KeyboardType.Decimal,
                    placeholder = "-76.53",
                    modifier = Modifier.weight(1f),
                    enabled = !state.isSaving,
                )
            }
        }

        DateTimePickerButton(
            label = "Fecha de inicio",
            value = state.date?.format(DATE_FORMAT),
            error = state.fieldErrors[MissionField.DATE],
            onClick = onChooseDate,
            enabled = !state.isSaving,
        )
        DateTimePickerButton(
            label = "Hora de inicio",
            value = state.time?.format(TIME_FORMAT),
            error = state.fieldErrors[MissionField.TIME],
            onClick = onChooseTime,
            enabled = !state.isSaving,
        )
        MissionInputField(
            label = "Cupo máximo",
            value = state.maxParticipants,
            onValueChange = viewModel::onMaxParticipantsChange,
            error = state.fieldErrors[MissionField.MAX_PARTICIPANTS],
            keyboardType = KeyboardType.Number,
            enabled = !state.isSaving,
        )

        state.submissionError?.let { error ->
            Text(error.toMessage(), color = MaterialTheme.colorScheme.error)
        }
        Button(
            onClick = viewModel::saveDraft,
            enabled = !state.isSaving,
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) {
            if (state.isSaving) {
                CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
            } else {
                Text("Guardar borrador")
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun MissionInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    error: MissionError? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    placeholder: String? = null,
    modifier: Modifier = Modifier.fillMaxWidth(),
    enabled: Boolean = true,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = placeholder?.let { text -> { Text(text) } },
        isError = error != null,
        supportingText = { error?.let { Text(it.toMessage()) } },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 3,
        enabled = enabled,
        modifier = modifier,
    )
}

@Composable
private fun DateTimePickerButton(
    label: String,
    value: String?,
    error: MissionError?,
    onClick: () -> Unit,
    enabled: Boolean,
) {
    Column {
        OutlinedButton(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
            Text(value ?: label)
        }
        error?.let { Text(it.toMessage(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
    }
}

private val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", Locale("es", "CO"))
private val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

private val ForestGreen = androidx.compose.ui.graphics.Color(0xFF214E34)
private val CreamSurface = androidx.compose.ui.graphics.Color(0xFFFFFCF3)
private val BarkBrown = androidx.compose.ui.graphics.Color(0xFF594A3C)
