package com.groovycow.misionessecundarias.ui.profile.history

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.groovycow.misionessecundarias.domain.model.HistoryRole
import com.groovycow.misionessecundarias.domain.model.MissionHistoryEntry
import com.groovycow.misionessecundarias.ui.profile.ProfileMessages
import com.groovycow.misionessecundarias.ui.theme.NaturePalette
import org.koin.androidx.compose.koinViewModel
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val EMPTY_SECTION = "Aún no hay registros"
private val DATE_FORMAT = DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", Locale("es", "CO"))

/**
 * Pantalla "Histórico de Misiones" (HU-011): línea temporal con tarjetas expandibles.
 *
 * @param onBack Acción al volver a Mi perfil.
 * @param viewModel ViewModel de la pantalla.
 */
@Composable
fun MissionHistoryScreen(
    onBack: () -> Unit,
    viewModel: MissionHistoryViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(Unit) { viewModel.load() }
    MissionHistoryContent(state, onBack, viewModel::retry, viewModel::toggle)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MissionHistoryContent(
    state: MissionHistoryUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onToggle: (String) -> Unit,
) {
    Scaffold(
        containerColor = NaturePalette.SandBackground,
        topBar = {
            TopAppBar(
                title = { Text("Histórico de Misiones", fontWeight = FontWeight.ExtraBold, color = NaturePalette.ForestGreen) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = NaturePalette.EarthOrange)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NaturePalette.SandBackground),
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            when {
                state.isLoading -> CircularProgressIndicator(color = NaturePalette.RiverTeal)
                state.error != null -> ErrorCard(ProfileMessages.error(state.error), onRetry)
                state.entries.isEmpty() ->
                    Text("Aún no tienes misiones en tu historial", color = NaturePalette.BarkBrown, textAlign = TextAlign.Center)
                else -> Timeline(state, onToggle)
            }
        }
    }
}

@Composable
private fun Timeline(
    state: MissionHistoryUiState,
    onToggle: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 8.dp),
    ) {
        items(state.entries, key = { it.missionId }) { entry ->
            Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(24.dp).fillMaxHeight()) {
                    Box(modifier = Modifier.padding(top = 18.dp).size(16.dp).background(statusColor(entry.status), CircleShape))
                    Box(modifier = Modifier.width(3.dp).weight(1f).background(NaturePalette.MapLine))
                }
                Box(modifier = Modifier.padding(start = 8.dp, bottom = 12.dp).weight(1f)) {
                    HistoryCard(entry, state, onToggle)
                }
            }
        }
    }
}

@Composable
private fun HistoryCard(
    entry: MissionHistoryEntry,
    state: MissionHistoryUiState,
    onToggle: (String) -> Unit,
) {
    val expanded = state.expandedId == entry.missionId
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onToggle(entry.missionId) },
        colors = CardDefaults.cardColors(containerColor = NaturePalette.CreamSurface),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(3.dp, statusColor(entry.status)),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(formatDate(entry.dateTime), color = NaturePalette.RiverTeal, fontWeight = FontWeight.Bold)
            Text(entry.title, color = NaturePalette.ForestGreen, fontWeight = FontWeight.ExtraBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                RoleChip(entry.role)
                Text(entry.status, color = NaturePalette.BarkBrown)
            }
            if (expanded) ExpandedDetail(entry, state.logbooks[entry.missionId])
        }
    }
}

@Composable
private fun ExpandedDetail(
    entry: MissionHistoryEntry,
    logbook: LogbookUiState?,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        entry.description?.let { Section("Descripción", it) }
        entry.requirements?.takeIf { it.isNotBlank() }?.let { Section("Requerimientos", it) }
        val sections = logbook?.sections
        if (logbook?.isLoading == true) {
            CircularProgressIndicator(color = NaturePalette.RiverTeal, modifier = Modifier.size(24.dp))
        }
        Section("Bitácora oficial", sections?.official ?: EMPTY_SECTION)
        Text("Palabras de participantes", color = NaturePalette.ForestGreen, fontWeight = FontWeight.Bold)
        val words = sections?.participants.orEmpty()
        if (words.isEmpty()) {
            Text(EMPTY_SECTION, color = NaturePalette.BarkBrown)
        } else {
            words.forEach { Text("${it.authorName}: ${it.text}", color = NaturePalette.BarkBrown) }
        }
    }
}

@Composable
private fun Section(
    title: String,
    body: String,
) {
    Column {
        Text(title, color = NaturePalette.ForestGreen, fontWeight = FontWeight.Bold)
        Text(body, color = NaturePalette.BarkBrown)
    }
}

@Composable
private fun RoleChip(role: HistoryRole) {
    val (label, color) =
        if (role == HistoryRole.PUBLISHER) "Publicador" to NaturePalette.LightEarth else "Solucionador" to NaturePalette.LightRiver
    Text(
        text = label,
        color = NaturePalette.BarkBrown,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.background(color, RoundedCornerShape(50)).padding(horizontal = 10.dp, vertical = 3.dp),
    )
}

@Composable
private fun ErrorCard(
    message: String,
    onRetry: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = NaturePalette.LightCoral),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(3.dp, NaturePalette.AlertCoral),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(message, color = NaturePalette.BarkBrown, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = NaturePalette.EarthOrange)) {
                Text("Reintentar", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun statusColor(status: String): Color =
    when (status) {
        "Completada" -> NaturePalette.LeafGreen
        "Cancelada" -> NaturePalette.AlertCoral
        "En progreso" -> NaturePalette.EarthOrange
        else -> NaturePalette.RiverTeal
    }

private fun formatDate(iso: String): String = runCatching { OffsetDateTime.parse(iso).format(DATE_FORMAT) }.getOrDefault(iso)
