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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.groovycow.misionessecundarias.ui.missions.toMessage
import org.koin.androidx.compose.koinViewModel

/** Porcentaje a partir del cual se habilita la generación de código y cierre de misión. */
private const val FULL_PROGRESS = 100

/**
 * Sección de Evaluación y Cierre de Misión (HU-064 & Token de Cierre 'CALI2026').
 *
 * Flujo:
 * 1. Al llegar al 100 % de progreso, el publicador presiona "Generar Código de Verificación".
 * 2. Se muestra el token 'CALI2026' en la tarjeta de certificación.
 * 3. Dentro de esta vista del código se ubica el botón "Completar Misión", el cual
 *    finaliza formalmente la misión y notifica el éxito.
 */
@Composable
fun CompleteMissionSection(
    missionId: String,
    progressPercent: Int,
    onMissionCompleted: (Mission) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CompleteMissionViewModel = koinViewModel(),
    generateCodeViewModel: GenerateCodeViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val generateState by generateCodeViewModel.uiState.collectAsState()
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

        // Una vez generado el código, se muestra la tarjeta con la clave y el botón "Completar Misión"
        if (generateState.verificationCode != null || state.completedMission != null) {
            GenerateCodeCard(
                verificationCode = generateState.verificationCode ?: "CALI2026",
                isCompleting = state.isCompleting,
                isCompleted = state.completedMission != null,
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

    state.completedMission?.let { mission ->
        AlertDialog(
            onDismissRequest = { onMissionCompleted(mission) },
            containerColor = CompleteCream,
            title = { Text("¡Misión completada!", color = CompleteForest, fontWeight = FontWeight.ExtraBold) },
            text = { Text("La misión fue marcada como Completada exitosamente.", color = CompleteForest) },
            confirmButton = {
                TextButton(onClick = { onMissionCompleted(mission) }) {
                    Text("Continuar", color = CompleteForest, fontWeight = FontWeight.Bold)
                }
            },
        )
    }
}

/**
 * Tarjeta que visualiza el código único ('CALI2026') e incluye el botón para finalizar formalmente la misión.
 */
@Composable
fun GenerateCodeCard(
    verificationCode: String,
    isCompleting: Boolean,
    isCompleted: Boolean,
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
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
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

            Button(
                onClick = onCompleteMission,
                enabled = !isCompleting && !isCompleted,
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
