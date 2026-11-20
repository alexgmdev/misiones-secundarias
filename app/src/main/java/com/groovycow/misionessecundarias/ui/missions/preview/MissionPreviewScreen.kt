package com.groovycow.misionessecundarias.ui.missions.preview

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.groovycow.misionessecundarias.domain.model.CreateMissionDraft
import com.groovycow.misionessecundarias.domain.model.MissionModality
import com.groovycow.misionessecundarias.domain.validation.MissionDateTime
import com.groovycow.misionessecundarias.ui.missions.MissionRewardsNotice
import com.groovycow.misionessecundarias.ui.missions.formatMissionDateTime

/**
 * Previsualización de la tarjeta y el detalle que verán los postulantes (HU-023).
 *
 * @param draft Datos validados del formulario.
 * @param isSaving Indica que el borrador se está guardando.
 * @param onBackToEdit Acción para regresar al formulario y ajustar datos.
 * @param onSave Acción para guardar el borrador desde la previsualización.
 * @param modifier Modificador aplicado por la pantalla contenedora.
 */
@Composable
fun MissionPreviewScreen(
    draft: CreateMissionDraft,
    isSaving: Boolean,
    onBackToEdit: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val date = draft.date
    val time = draft.time
    val formattedDate =
        if (date != null && time != null) {
            formatMissionDateTime(MissionDateTime.toIsoOffset(date, time))
        } else {
            "Fecha por definir"
        }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            text = "Así verán tu misión los postulantes",
            color = BarkBrown,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        Text(
            text = draft.title,
            style = MaterialTheme.typography.headlineSmall,
            color = ForestGreen,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        draft.category?.takeIf { it.isNotBlank() }?.let { category ->
            Text(
                text = category,
                color = CreamSurface,
                fontWeight = FontWeight.Bold,
                modifier =
                    Modifier
                        .background(RiverTeal, RoundedCornerShape(20.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp),
            )
        }

        InfoLine(
            title = "Inicio",
            value = formattedDate,
            accentColor = RiverTeal,
            backgroundColor = LightRiver,
        )
        InfoLine(
            title = "Tipo de misión",
            value = if (draft.modality == MissionModality.REMOTA) "Misión remota" else "Misión de campo",
            accentColor = RiverTeal,
            backgroundColor = LightRiver,
        )

        if (draft.modality == MissionModality.CAMPO) {
            PreviewSection(
                title = "Ubicación aproximada",
                text = draft.meetingPoint.ifBlank { "Punto de encuentro por definir" },
                accentColor = EarthOrange,
                backgroundColor = LightEarth,
            )
        }

        PreviewSection(
            title = "Para esta misión necesitarás",
            text = draft.requirements,
            accentColor = LeafGreen,
            backgroundColor = LightLeaf,
        )
        PreviewSection(
            title = "Descripción de la misión",
            text = draft.description,
            accentColor = EarthOrange,
            backgroundColor = LightEarth,
        )

        draft.reward.takeIf { it.isNotBlank() }?.let { reward ->
            PreviewSection(
                title = "Recompensa informativa",
                text = reward,
                accentColor = EarthOrange,
                backgroundColor = LightEarth,
            )
        }
        MissionRewardsNotice()

        PreviewSection(
            title = "Participantes",
            text = "Cupo máximo: ${draft.maxParticipants} participantes.",
            accentColor = RiverTeal,
            backgroundColor = LightRiver,
        )

        Button(
            onClick = onSave,
            enabled = !isSaving,
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) {
            if (isSaving) {
                CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
            } else {
                Text("Guardar borrador")
            }
        }
        OutlinedButton(
            onClick = onBackToEdit,
            enabled = !isSaving,
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) {
            Text("Volver a editar")
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun InfoLine(
    title: String,
    value: String,
    accentColor: Color,
    backgroundColor: Color,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(backgroundColor, RoundedCornerShape(14.dp))
                .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(title, color = BarkBrown, fontWeight = FontWeight.Bold)
        Text(value, color = accentColor, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
private fun PreviewSection(
    title: String,
    text: String,
    accentColor: Color,
    backgroundColor: Color,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(title, color = ForestGreen, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(7.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(2.dp, accentColor),
            colors = CardDefaults.cardColors(containerColor = backgroundColor),
        ) {
            Text(text, modifier = Modifier.padding(16.dp), color = BarkBrown, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

private val ForestGreen = Color(0xFF214E34)
private val LeafGreen = Color(0xFF64A95C)
private val LightLeaf = Color(0xFFDCEFD3)
private val RiverTeal = Color(0xFF38A89D)
private val LightRiver = Color(0xFFCDEDEA)
private val EarthOrange = Color(0xFFE58A3A)
private val LightEarth = Color(0xFFFFE2C2)
private val CreamSurface = Color(0xFFFFFCF3)
private val BarkBrown = Color(0xFF594A3C)
