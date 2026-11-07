
package com.groovycow.misionessecundarias.ui.applications

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class ApplicantUi(
    val applicationId: String,
    val applicantName: String,
    val appliedAt: String,
    val status: String = "Postulado"
)

@Composable
fun PostulantesScreen(
    applicants: List<ApplicantUi>,
    loading: Boolean,
    onAccept: (String) -> Unit,
    onReject: (String) -> Unit
) {
    var selectedApplicant by remember { mutableStateOf<ApplicantUi?>(null) }
    var selectedAction by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Postulantes a mi misión",
            style = MaterialTheme.typography.headlineSmall
        )

        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        } else if (applicants.isEmpty()) {
            Text(
                text = "Todavía no hay postulantes.",
                modifier = Modifier.padding(top = 16.dp)
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(top = 16.dp)
            ) {
                items(applicants.sortedByDescending { it.appliedAt }) { applicant ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = applicant.applicantName,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text("Fecha de postulación: ${applicant.appliedAt}")
                            Text("Estado: ${applicant.status}")

                            if (applicant.status == "Postulado") {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.padding(top = 12.dp)
                                ) {
                                    Button(onClick = {
                                        selectedApplicant = applicant
                                        selectedAction = "Aceptar"
                                    }) {
                                        Text("Aceptar")
                                    }

                                    OutlinedButton(onClick = {
                                        selectedApplicant = applicant
                                        selectedAction = "Rechazar"
                                    }) {
                                        Text("Rechazar")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    selectedApplicant?.let { applicant ->
        AlertDialog(
            onDismissRequest = {
                selectedApplicant = null
                selectedAction = ""
            },
            title = { Text("$selectedAction postulación") },
            text = {
                Text(
                    "¿Estás seguro de que deseas ${selectedAction.lowercase()} " +
                        "a ${applicant.applicantName}?"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (selectedAction == "Aceptar") {
                        onAccept(applicant.applicationId)
                    } else {
                        onReject(applicant.applicationId)
                    }
                    selectedApplicant = null
                    selectedAction = ""
                }) {
                    Text("Confirmar")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    selectedApplicant = null
                    selectedAction = ""
                }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
