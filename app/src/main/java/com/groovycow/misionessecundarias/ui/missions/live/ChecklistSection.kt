package com.groovycow.misionessecundarias.ui.missions.live

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.groovycow.misionessecundarias.domain.model.ChecklistItem
import com.groovycow.misionessecundarias.domain.usecase.AddChecklistItemUseCase

/** Checklist de ejecución con la barra de progreso calculada a partir de las tareas cumplidas (HU-053). */
@Composable
fun ChecklistSection(
    state: ChecklistUiState,
    onAdd: (title: String, onAdded: () -> Unit) -> Unit,
    onToggle: (ChecklistItem) -> Unit,
    onDelete: (ChecklistItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    var newTitle by rememberSaveable { mutableStateOf("") }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(3.dp, ChecklistTeal),
        colors = CardDefaults.cardColors(containerColor = ChecklistCream),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "Progreso de la misión: ${state.progressPercent}%",
                color = ChecklistForest,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
            )
            LinearProgressIndicator(
                progress = { state.progressPercent / PERCENT_BASE },
                modifier = Modifier.fillMaxWidth().height(12.dp),
                color = ChecklistLeaf,
                trackColor = ChecklistLightLeaf,
            )

            ChecklistBody(state = state, onToggle = onToggle, onDelete = onDelete)

            state.actionError?.let {
                Text(text = it, color = ChecklistCoral, fontWeight = FontWeight.Bold)
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = newTitle,
                    onValueChange = { if (it.length <= AddChecklistItemUseCase.MAX_TITLE_LENGTH) newTitle = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Nueva tarea") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                )
                Button(
                    onClick = { onAdd(newTitle) { newTitle = "" } },
                    enabled = !state.isBusy && newTitle.isNotBlank(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ChecklistOrange, contentColor = ChecklistCream),
                ) {
                    Text("Agregar", fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

@Composable
private fun ChecklistBody(
    state: ChecklistUiState,
    onToggle: (ChecklistItem) -> Unit,
    onDelete: (ChecklistItem) -> Unit,
) {
    when {
        state.isLoading -> Text("Cargando checklist...", color = ChecklistBrown)

        state.error != null -> Text(state.error, color = ChecklistCoral, fontWeight = FontWeight.Bold)

        state.items.isEmpty() ->
            Text(
                text = "Define las tareas de la misión. El progreso se calcula con las que marques como cumplidas.",
                color = ChecklistBrown,
            )

        else ->
            state.items.forEach { item ->
                ChecklistRow(item = item, enabled = !state.isBusy, onToggle = { onToggle(item) }, onDelete = { onDelete(item) })
            }
    }
}

@Composable
private fun ChecklistRow(
    item: ChecklistItem,
    enabled: Boolean,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Checkbox(
            checked = item.isDone,
            onCheckedChange = { onToggle() },
            enabled = enabled,
            colors = CheckboxDefaults.colors(checkedColor = ChecklistLeaf, uncheckedColor = ChecklistForest),
        )
        Text(
            text = item.title,
            modifier = Modifier.weight(1f),
            color = ChecklistForest,
            fontWeight = if (item.isDone) FontWeight.Normal else FontWeight.Bold,
        )
        IconButton(onClick = onDelete, enabled = enabled) {
            Icon(Icons.Default.Delete, contentDescription = "Eliminar tarea ${item.title}", tint = ChecklistCoral)
        }
    }
}

private const val PERCENT_BASE = 100f
private val ChecklistForest = Color(0xFF214E34)
private val ChecklistLeaf = Color(0xFF64A95C)
private val ChecklistLightLeaf = Color(0xFFDCEFD3)
private val ChecklistTeal = Color(0xFF38A89D)
private val ChecklistOrange = Color(0xFFE58A3A)
private val ChecklistCream = Color(0xFFFFFCF3)
private val ChecklistBrown = Color(0xFF594A3C)
private val ChecklistCoral = Color(0xFFE85D3F)
