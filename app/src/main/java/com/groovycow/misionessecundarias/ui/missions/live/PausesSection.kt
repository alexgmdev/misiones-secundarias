package com.groovycow.misionessecundarias.ui.missions.live

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.groovycow.misionessecundarias.domain.model.PauseKind

/**
 * Apartado de pausas del publicador (HU-054 y HU-055): una casilla por pausa.
 *
 * Al marcar una, los postulantes aceptados ven que la misión está en pausa.
 */
@Composable
fun PausesSection(
    state: PausesUiState,
    onToggle: (PauseKind) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(3.dp, PauseOrange),
        colors = CardDefaults.cardColors(containerColor = PauseDark),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            PauseKind.entries.forEach { kind ->
                PauseRow(
                    kind = kind,
                    checked = kind in state.active,
                    enabled = !state.isBusy,
                    onToggle = { onToggle(kind) },
                )
            }
            state.error?.let { Text(text = it, color = PauseCoral, fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun PauseRow(
    kind: PauseKind,
    checked: Boolean,
    enabled: Boolean,
    onToggle: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = kind.label + ":",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f, fill = false),
        )
        Spacer(modifier = Modifier.width(8.dp))
        val shape = RoundedCornerShape(10.dp)
        Box(
            modifier =
                Modifier
                    .size(32.dp)
                    .background(if (checked) PauseGreen else Color.White, shape)
                    .border(3.dp, Color.White, shape)
                    .clickable(enabled = enabled, role = Role.Checkbox, onClick = onToggle)
                    .semantics { contentDescription = kind.label + if (checked) ", activada" else ", desactivada" },
        )
    }
}

private val PauseDark = Color(0xFF1F1F1F)
private val PauseOrange = Color(0xFFE58A3A)
private val PauseGreen = Color(0xFF6FDB8F)
private val PauseCoral = Color(0xFFFF7A5C)
