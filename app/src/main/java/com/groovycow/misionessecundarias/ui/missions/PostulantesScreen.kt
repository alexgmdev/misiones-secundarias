package com.groovycow.misionessecundarias.ui.missions

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.groovycow.misionessecundarias.domain.model.ApplicationDecision
import com.groovycow.misionessecundarias.domain.model.MissionApplicant
import org.koin.androidx.compose.koinViewModel
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostulantesScreen(
    missionId: String,
    missionTitle: String,
    onBack: () -> Unit,
    viewModel: ApplicantsViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var pendingDecision by remember {
        mutableStateOf<Pair<MissionApplicant, ApplicationDecision>?>(null)
    }

    LaunchedEffect(missionId) {
        viewModel.loadApplicants(missionId)
    }

    Scaffold(
        containerColor = ApplicantsSand,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Postulantes",
                        color = ApplicantsForest,
                        fontWeight = FontWeight.ExtraBold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = ApplicantsOrange,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ApplicantsSand),
            )
        },
    ) { paddingValues ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
        ) {
            Text(
                text = missionTitle,
                modifier = Modifier.fillMaxWidth(),
                color = ApplicantsForest,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(12.dp))

            ApplicantsGuide(
                message = "Revisa a quienes quieren unirse y decide quién participará en tu misión.",
            )
            Spacer(modifier = Modifier.height(14.dp))

            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        color = ApplicantsTeal,
                    )
                }

                uiState.error != null -> {
                    ApplicantsMessage(
                        message = uiState.error!!,
                        accentColor = ApplicantsCoral,
                    )
                }

                uiState.applicants.isEmpty() -> {
                    ApplicantsMessage(
                        message = "Todavía no hay postulantes. ¡Pronto podría llegar un nuevo compañero de aventura!",
                        accentColor = ApplicantsOrange,
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(uiState.applicants, key = { it.id }) { applicant ->
                            ApplicantCard(
                                applicant = applicant,
                                isUpdating = uiState.updatingApplicationId == applicant.id,
                                onAccept = {
                                    pendingDecision = applicant to ApplicationDecision.ACCEPT
                                },
                                onReject = {
                                    pendingDecision = applicant to ApplicationDecision.REJECT
                                },
                            )
                        }
                        item { Spacer(modifier = Modifier.height(12.dp)) }
                    }
                }
            }
        }
    }

    pendingDecision?.let { (applicant, decision) ->
        val verb = if (decision == ApplicationDecision.ACCEPT) "aceptar" else "rechazar"
        AlertDialog(
            onDismissRequest = { pendingDecision = null },
            containerColor = ApplicantsCream,
            title = {
                Text(
                    text = if (decision == ApplicationDecision.ACCEPT) "Aceptar postulación" else "Rechazar postulación",
                    color = ApplicantsForest,
                    fontWeight = FontWeight.ExtraBold,
                )
            },
            text = {
                Text(
                    text = "¿Confirmas que deseas $verb a ${applicant.profile.fullName}?",
                    color = ApplicantsBrown,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.decide(applicant.id, decision)
                        pendingDecision = null
                    },
                ) {
                    Text("Confirmar", color = ApplicantsOrange, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDecision = null }) {
                    Text("Volver", color = ApplicantsForest)
                }
            },
        )
    }

    uiState.actionMessage?.let { message ->
        AlertDialog(
            onDismissRequest = viewModel::dismissNotice,
            containerColor = ApplicantsCream,
            title = {
                Text(
                    text = if (uiState.isActionError) "No fue posible guardar" else "Decisión guardada",
                    color = if (uiState.isActionError) ApplicantsCoral else ApplicantsForest,
                    fontWeight = FontWeight.ExtraBold,
                )
            },
            text = { Text(message, color = ApplicantsBrown) },
            confirmButton = {
                TextButton(onClick = viewModel::dismissNotice) {
                    Text("Aceptar", color = ApplicantsForest, fontWeight = FontWeight.Bold)
                }
            },
        )
    }
}

@Composable
private fun ApplicantCard(
    applicant: MissionApplicant,
    isUpdating: Boolean,
    onAccept: () -> Unit,
    onReject: () -> Unit,
) {
    val accent =
        when (applicant.status) {
            "Aceptado" -> ApplicantsLeaf
            "Rechazado" -> ApplicantsCoral
            else -> ApplicantsOrange
        }
    val formattedDate =
        runCatching {
            OffsetDateTime.parse(applicant.appliedAt).format(
                DateTimeFormatter.ofPattern("d 'de' MMMM 'a las' HH:mm", Locale("es", "CO")),
            )
        }.getOrDefault(applicant.appliedAt)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(3.dp, accent),
        colors = CardDefaults.cardColors(containerColor = ApplicantsCream),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = ApplicantsForest,
                    modifier =
                        Modifier
                            .size(46.dp)
                            .background(ApplicantsLightLeaf, CircleShape)
                            .padding(8.dp),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = applicant.profile.fullName,
                        color = ApplicantsForest,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    Text(
                        text = "Se postuló el $formattedDate",
                        color = ApplicantsBrown,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Text(
                    text = applicant.status,
                    color = accent,
                    fontWeight = FontWeight.ExtraBold,
                    style = MaterialTheme.typography.labelLarge,
                )
            }

            if (applicant.status == "Postulado") {
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Button(
                        onClick = onAccept,
                        enabled = !isUpdating,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = ApplicantsLeaf,
                                contentColor = ApplicantsCream,
                            ),
                    ) {
                        if (isUpdating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = ApplicantsCream,
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Text("Aceptar", fontWeight = FontWeight.ExtraBold)
                        }
                    }
                    OutlinedButton(
                        onClick = onReject,
                        enabled = !isUpdating,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(2.dp, ApplicantsCoral),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ApplicantsCoral),
                    ) {
                        Text("Rechazar", fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun ApplicantsGuide(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(3.dp, ApplicantsTeal),
        colors = CardDefaults.cardColors(containerColor = ApplicantsLightRiver),
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(16.dp),
            color = ApplicantsBrown,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun ApplicantsMessage(
    message: String,
    accentColor: Color,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(3.dp, accentColor),
        colors = CardDefaults.cardColors(containerColor = ApplicantsCream),
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(20.dp),
            color = ApplicantsBrown,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

private val ApplicantsForest = Color(0xFF214E34)
private val ApplicantsLeaf = Color(0xFF64A95C)
private val ApplicantsLightLeaf = Color(0xFFDCEFD3)
private val ApplicantsTeal = Color(0xFF38A89D)
private val ApplicantsLightRiver = Color(0xFFCDEDEA)
private val ApplicantsOrange = Color(0xFFE58A3A)
private val ApplicantsSand = Color(0xFFF7F3E8)
private val ApplicantsCream = Color(0xFFFFFCF3)
private val ApplicantsBrown = Color(0xFF594A3C)
private val ApplicantsCoral = Color(0xFFE85D3F)
