package com.groovycow.misionessecundarias.ui.missions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.groovycow.misionessecundarias.domain.model.Mission
import org.koin.androidx.compose.koinViewModel

@Composable
fun MissionListScreen(
    onMissionClick: (String) -> Unit,
    viewModel: MissionViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Misiones Disponibles", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        if (uiState.isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(androidx.compose.ui.Alignment.CenterHorizontally))
        } else if (uiState.error != null) {
            Text("Error: ${uiState.error}", color = MaterialTheme.colorScheme.error)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(uiState.missions) { mission ->
                    MissionCard(mission = mission, onClick = { onMissionClick(mission.id) })
                }
            }
        }
    }
}

@Composable
fun MissionCard(mission: Mission, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = mission.title, style = MaterialTheme.typography.titleLarge)
            Text(text = "Modalidad: ${mission.modality}", style = MaterialTheme.typography.bodyMedium)
            if (mission.category != null) {
                Text(text = "Categoría: ${mission.category}", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}