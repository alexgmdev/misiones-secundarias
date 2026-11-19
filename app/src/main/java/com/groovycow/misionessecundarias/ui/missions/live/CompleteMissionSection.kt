package com.groovycow.misionessecundarias.ui.missions.live

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.groovycow.misionessecundarias.domain.model.Mission
import com.groovycow.misionessecundarias.ui.missions.toMessage
import org.koin.androidx.compose.koinViewModel

/** Porcentaje a partir del cual se habilita el botón de completar. */
private const val FULL_PROGRESS = 100

/**
 * Botón "Completar Misión" (HU-064): se habilita solo cuando el progreso llega al 100 %.
 *
 * Al completar muestra una confirmación y, al aceptarla, entrega la misión ya completada.
 */
@Composable
fun CompleteMissionSection(
    missionId: String,
    progressPercent: Int,
    onMissionCompleted: (Mission) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CompleteMissionViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val canComplete = progressPercent >= FULL_PROGRESS && !state.isCompleting && state.completedMission == null

    Column(modifier = modifier.fillMaxWidth()) {
        Button(
            onClick = { viewModel.complete(missionId) },
            enabled = canComplete,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(14.dp),
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = CompleteLeaf,
                    contentColor = CompleteCream,
                    disabledContainerColor = CompleteLightLeaf,
                    disabledContentColor = CompleteForest,
                ),
        ) {
            if (state.isCompleting) {
                CircularProgressIndicator(modifier = Modifier.size(22.dp), color = CompleteCream, strokeWidth = 2.dp)
            } else {
                Text("Completar Misión", fontWeight = FontWeight.ExtraBold)
            }
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

    state.completedMission?.let { mission ->
        AlertDialog(
            onDismissRequest = { onMissionCompleted(mission) },
            containerColor = CompleteCream,
            title = { Text("¡Misión completada!", color = CompleteForest, fontWeight = FontWeight.ExtraBold) },
            text = { Text("La misión llegó al 100 % y quedó en estado Completada.", color = CompleteForest) },
            confirmButton = {
                TextButton(onClick = { onMissionCompleted(mission) }) {
                    Text("Aceptar", color = CompleteForest, fontWeight = FontWeight.Bold)
                }
            },
        )
    }
}

private val CompleteForest = Color(0xFF214E34)
private val CompleteLeaf = Color(0xFF64A95C)
private val CompleteLightLeaf = Color(0xFFDCEFD3)
private val CompleteCream = Color(0xFFFFFCF3)
private val CompleteCoral = Color(0xFFE85D3F)
