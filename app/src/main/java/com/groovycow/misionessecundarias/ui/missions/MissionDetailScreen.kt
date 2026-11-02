package com.groovycow.misionessecundarias.ui.missions

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MissionDetailScreen(
    missionTitle: String,
    missionDescription: String,
    modality: String,
    category: String?,
    requirements: String?,
    publisherName: String?,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Misión", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = Color.Red)
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = missionTitle,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(androidx.compose.ui.Alignment.CenterHorizontally)
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Publicado por
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Text("Publicado por: ", style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = publisherName ?: "Usuario Desconocido",
                    style = MaterialTheme.typography.bodyMedium,
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF5722) // Naranja como el prototipo
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tipo de misión
            Text("Esta es una misión tipo: $modality", style = MaterialTheme.typography.bodyLarge)
            if (category != null) {
                Text("Categoría: $category", style = MaterialTheme.typography.bodyLarge)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Requerimientos
            Text("Para esta misión necesitarás:", fontWeight = FontWeight.Bold)
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F0F0)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = requirements ?: "No hay requerimientos específicos.",
                    modifier = Modifier.padding(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Descripción
            Text("Descripción de la misión:", fontWeight = FontWeight.Bold)
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F0F0)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(text = missionDescription, modifier = Modifier.padding(16.dp))
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Botón Verde (Estilo prototipo)
            Button(
                onClick = { /* Jhojan integrará la postulación aquí */ },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8BC34A)), // Verde del prototipo
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Postularme a la Misión", color = Color.White, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}