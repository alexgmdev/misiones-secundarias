package com.groovycow.misionessecundarias.ui.missions

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Texto aprobado por producto para aclarar que las recompensas no son transacciones monetarias. */
const val REWARDS_NOTICE_TEXT =
    "Aviso: La plataforma no maneja pasarela de pago; las recompensas son meramente informativas"

/** Sección informativa y estática de recompensas, sin acciones ni navegación transaccional. */
@Composable
fun MissionRewardsNotice() {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Recompensas",
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.ExtraBold,
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(modifier = Modifier.height(7.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Text(
                text = REWARDS_NOTICE_TEXT,
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}
