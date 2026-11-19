package com.groovycow.misionessecundarias.ui.missions.live

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.groovycow.misionessecundarias.domain.model.Mission
import com.groovycow.misionessecundarias.domain.model.MissionApplicant
import com.groovycow.misionessecundarias.ui.missions.toMessage
import org.koin.androidx.compose.koinViewModel

/** Porcentaje a partir del cual se habilita la generación de código y cierre de misión. */
private const val FULL_PROGRESS = 100

/**
 * Sección de Evaluación y Cierre de Misión (HU-064, Código de Cierre & Bitácora Obligatoria).
 *
 * Flujo:
 * 1. Al llegar al 100 % de progreso, el publicador presiona "Generar Código de Verificación".
 * 2. Se muestra el token dinámico de certificación de asistencia.
 * 3. Se solicita de forma obligatoria redactar y presionar "Guardar Bitácora", vinculando los participantes.
 * 4. Tras guardar la bitácora, se habilita el botón "Completar Misión" para finalizar la misión.
 */
@Composable
fun CompleteMissionSection(
    missionId: String,
    progressPercent: Int,
    onMissionCompleted: (Mission) -> Unit,
    modifier: Modifier = Modifier,
    participants: List<MissionApplicant> = emptyList(),
    viewModel: CompleteMissionViewModel = koinViewModel(),
    generateCodeViewModel: GenerateCodeViewModel = koinViewModel(),
    logbookViewModel: MissionLogbookViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val generateState by generateCodeViewModel.uiState.collectAsState()
    val logbookState by logbookViewModel.uiState.collectAsState()

    val canGenerateCode = progressPercent >= FULL_PROGRESS && generateState.verificationCode == null && state.completedMission == null

    Column(modifier = modifier.fillMaxWidth()) {
        // Botón inicial para generar el código cuando el progreso llega al 100 %
        if (generateState.verificationCode == null && state.completedMission == null) {
            Button(
                onClick = { generateCodeViewModel.generateCode(missionId) },
                enabled = canGenerateCode && !generateState.isGenerating,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(14.dp),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = CompleteForest,
                        contentColor = CompleteCream,
                        disabledContainerColor = CompleteLightLeaf,
                        disabledContentColor = CompleteForest,
                    ),
            ) {
                if (generateState.isGenerating) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), color = CompleteCream, strokeWidth = 2.dp)
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Generar Código de Verificación", fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }

        // Una vez generado el código, se muestra la tarjeta del código, el campo de bitácora y el botón de cierre
        if (generateState.verificationCode != null || state.completedMission != null) {
            GenerateCodeCard(
                verificationCode = generateState.verificationCode ?: "CALI-OK",
                isCompleting = state.isCompleting,
                isCompleted = state.completedMission != null,
                logbookState = logbookState,
                participants = participants,
                onEntryChanged = logbookViewModel::onEntryChanged,
                onSaveLogbook = { logbookViewModel.saveLogbook(missionId, participants) },
                onCompleteMission = { viewModel.complete(missionId) },
            )
        }
    }

    state.error?.let { error ->
        AlertDialog(
            onDismissRequest = viewModel::dismissError,
            containerColor = CompleteCream,
            title = { Text("No se pudo completar la misión", color = CompleteCoral, fontWeight = FontWeight.ExtraBold) },
            text = { Text(error.toMessage(), color = CompleteForest) },
            confirmButton = {
                TextButton(onClick = viewModel::dismissError) {
                    Text("Aceptar", color = CompleteForest, fontWeight = FontWeight.Bold)
                }
            },
        )
    }

    generateState.error?.let { error ->
        AlertDialog(
            onDismissRequest = generateCodeViewModel::dismissError,
            containerColor = CompleteCream,
            title = { Text("No se pudo generar el código", color = CompleteCoral, fontWeight = FontWeight.ExtraBold) },
            text = { Text(error.toMessage(), color = CompleteForest) },
            confirmButton = {
                TextButton(onClick = generateCodeViewModel::dismissError) {
                    Text("Aceptar", color = CompleteForest, fontWeight = FontWeight.Bold)
                }
            },
        )
    }

    logbookState.error?.let { error ->
        AlertDialog(
            onDismissRequest = logbookViewModel::dismissError,
            containerColor = CompleteCream,
            title = { Text("Bitácora Obligatoria", color = CompleteCoral, fontWeight = FontWeight.ExtraBold) },
            text = { Text(error.toMessage(), color = CompleteForest) },
            confirmButton = {
                TextButton(onClick = logbookViewModel::dismissError) {
                    Text("Aceptar", color = CompleteForest, fontWeight = FontWeight.Bold)
                }
            },
        )
    }

    state.completedMission?.let { mission ->
        AlertDialog(
            onDismissRequest = { onMissionCompleted(mission) },
            containerColor = CompleteCream,
            title = { Text("¡Misión completada!", color = CompleteForest, fontWeight = FontWeight.ExtraBold) },
            text = { Text("La misión y su bitácora fueron guardadas exitosamente.", color = CompleteForest) },
            confirmButton = {
                TextButton(onClick = { onMissionCompleted(mission) }) {
                    Text("Continuar", color = CompleteForest, fontWeight = FontWeight.Bold)
                }
            },
        )
    }
}

/**
 * Tarjeta que visualiza el código único, el formulario de bitácora obligatoria y los participantes asociados.
 */
@Composable
fun GenerateCodeCard(
    verificationCode: String,
    isCompleting: Boolean,
    isCompleted: Boolean,
    logbookState: LogbookUiState,
    participants: List<MissionApplicant>,
    onEntryChanged: (String) -> Unit,
    onSaveLogbook: () -> Unit,
    onCompleteMission: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(2.dp, CompleteLeaf),
        colors = CardDefaults.cardColors(containerColor = CompleteCream),
    ) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = CompleteForest,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Código de Verificación de Cierre",
                    color = CompleteForest,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                color = CompleteLightLeaf,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, CompleteLeaf),
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "CLAVE ÚNICA DE CERTIFICACIÓN",
                        color = CompleteForest,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = verificationCode,
                        color = CompleteForest,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            Text(
                text = "Comparte este código con los asistentes para validar su presencia post-misión.",
                color = CompleteForest,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Sección de Bitácora Obligatoria
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = CompleteForest,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Añadir Bitácora (Obligatoria)",
                        color = CompleteForest,
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.titleSmall,
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Muestra los participantes asociados
                if (participants.isNotEmpty()) {
                    Text(
                        text = "Participantes asociados:",
                        color = CompleteForest,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        participants.forEach { participant ->
                            Surface(
                                color = CompleteLightLeaf,
                                shape = CircleShape,
                                border = BorderStroke(1.dp, CompleteLeaf),
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = CompleteForest,
                                        modifier = Modifier.size(14.dp),
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = participant.profile.fullName.substringBefore(' '),
                                        color = CompleteForest,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (logbookState.savedLogbook == null) {
                    OutlinedTextField(
                        value = logbookState.entry,
                        onValueChange = onEntryChanged,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Relata qué y cómo se vivió la misión...") },
                        minLines = 3,
                        maxLines = 5,
                        colors =
                            OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CompleteForest,
                                unfocusedBorderColor = CompleteLeaf,
                                focusedContainerColor = CompleteCream,
                                unfocusedContainerColor = CompleteCream,
                            ),
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = onSaveLogbook,
                        enabled = !logbookState.isSaving && logbookState.entry.trim().isNotEmpty(),
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = CompleteForest,
                                contentColor = CompleteCream,
                            ),
                    ) {
                        if (logbookState.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = CompleteCream,
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Text("Guardar Bitácora", fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    Surface(
                        color = CompleteLightLeaf,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, CompleteLeaf),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = CompleteLeaf,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Bitácora guardada oficialmente",
                                color = CompleteForest,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // El botón de Completar Misión requiere haber guardado la bitácora previamente
            Button(
                onClick = onCompleteMission,
                enabled = !isCompleting && !isCompleted && logbookState.savedLogbook != null,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = CompleteLeaf,
                        contentColor = CompleteCream,
                        disabledContainerColor = CompleteLightLeaf,
                        disabledContentColor = CompleteForest,
                    ),
            ) {
                if (isCompleting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = CompleteCream,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(
                        text = if (isCompleted) "Misión Completada" else "Completar Misión",
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
            }
        }
    }
}

private val CompleteForest = Color(0xFF214E34)
private val CompleteLeaf = Color(0xFF64A95C)
private val CompleteLightLeaf = Color(0xFFDCEFD3)
private val CompleteCream = Color(0xFFFFFCF3)
private val CompleteCoral = Color(0xFFE85D3F)
