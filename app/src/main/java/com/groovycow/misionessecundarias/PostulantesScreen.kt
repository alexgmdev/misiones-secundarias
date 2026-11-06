
package com.groovycow.misionessecundarias.ui.postulaciones

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.groovycow.misionessecundarias.domain.model.MissionApplicant

@Composable
fun PostulantesScreen(
    applicants: List<MissionApplicant>,
    loading: Boolean,
    onAccept: (String) -> Unit,
    onReject: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedApplicant by remember {
        mutableStateOf<MissionApplicant?>(null)
    }

    var action by remember {
        mutableStateOf("")
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Postulantes pendientes",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        } else if (applicants.isEmpty()) {
            Text("No hay postulantes pendientes.")
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items = applicants,
                    key = { it.id }
                ) { applicant ->

                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = applicant.profiles?.fullName
                                    ?: "Usuario sin nombre",
                                style = MaterialTheme.typography.titleMedium
                            )

                            Text(
                                text = "Fecha: ${applicant.appliedAt}",
                                style = MaterialTheme.typography.bodySmall
                            )

                            Spacer(
                                modifier = Modifier.height(12.dp)
                            )

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        selectedApplicant = applicant
                                        action = "Aceptar"
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Aceptar")
                                }

                                OutlinedButton(
                                    onClick = {
                                        selectedApplicant = applicant
                                        action = "Rechazar"
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Rechazar")
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
            },
            title = {
                Text("Confirmar $action")
            },
            text = {
                Text(
                    "¿Estás seguro de que deseas $action " +
                    "a ${applicant.profiles?.fullName ?: "este usuario"}?"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (action == "Aceptar") {
                            onAccept(applicant.id)
                        } else {
                            onReject(applicant.id)
                        }

                        selectedApplicant = null
                    }
                ) {
                    Text("Confirmar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        selectedApplicant = null
                    }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
}
