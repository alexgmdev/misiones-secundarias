package com.groovycow.misionessecundarias.ui.applications

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import com.groovycow.misionessecundarias.domain.model.MyApplication
import org.koin.androidx.compose.koinViewModel
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyApplicationsScreen(
    onBack: () -> Unit,
    onExploreMissions: () -> Unit = {},
    viewModel: MyApplicationsViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var applicationToCancel by remember { mutableStateOf<MyApplication?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadApplications()
    }

    Scaffold(
        containerColor = SandBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Estado de las misiones",
                        color = ForestGreen,
                        fontWeight = FontWeight.ExtraBold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = EarthOrange,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SandBackground),
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
            when {
                uiState.isLoading -> {
                    GameStateMessage(
                        message = "Estoy buscando tus misiones...",
                        showProgress = true,
                    )
                }

                uiState.error != null -> {
                    GameStateMessage(
                        message = uiState.error!!,
                        accentColor = AlertCoral,
                    )
                }

                uiState.applications.isEmpty() -> {
                    EmptyApplicationsState(onExploreMissions = onExploreMissions)
                }

                else -> {
                    MissionGuide(
                        message = "Estas son las misiones en las que participas o esperas respuesta.",
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(uiState.applications, key = { it.id }) { application ->
                            ApplicationStatusCard(
                                application = application,
                                isCancelling = uiState.cancellingApplicationId == application.id,
                                onCancel = { applicationToCancel = application },
                            )
                        }
                        item { Spacer(modifier = Modifier.height(12.dp)) }
                    }
                }
            }
        }
    }

    applicationToCancel?.let { application ->
        AlertDialog(
            onDismissRequest = { applicationToCancel = null },
            containerColor = CreamSurface,
            title = {
                Text("Confirmar retiro", color = ForestGreen, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    if (application.status == "Aceptado") {
                        "¿Deseas salir de la misión ${application.mission.title}?"
                    } else {
                        "¿Deseas cancelar tu postulación a ${application.mission.title}?"
                    },
                    color = BarkBrown,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.cancelApplication(application.id)
                        applicationToCancel = null
                    },
                ) {
                    Text("Confirmar", color = EarthOrange, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { applicationToCancel = null }) {
                    Text("Volver", color = ForestGreen)
                }
            },
        )
    }

    val noticeMessage = uiState.actionMessage ?: uiState.error
    noticeMessage?.let { message ->
        val isError = uiState.error != null || uiState.isActionError
        AlertDialog(
            onDismissRequest = viewModel::dismissNotice,
            containerColor = CreamSurface,
            title = {
                Text(
                    text = if (isError) "Aviso" else "Operación exitosa",
                    color = if (isError) AlertCoral else ForestGreen,
                    fontWeight = FontWeight.ExtraBold,
                )
            },
            text = { Text(message, color = BarkBrown) },
            confirmButton = {
                TextButton(onClick = viewModel::dismissNotice) {
                    Text("Aceptar", color = ForestGreen, fontWeight = FontWeight.Bold)
                }
            },
        )
    }
}

@Composable
private fun EmptyApplicationsState(onExploreMissions: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        MissionGuide(
            message = "Tu lista está vacía. ¡Encuentra una misión y comienza tu aventura!",
        )
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = "Todavía no tienes misiones",
            style = MaterialTheme.typography.headlineSmall,
            color = ForestGreen,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Explora actividades comunitarias en Cali y postúlate a la que más te inspire.",
            color = BarkBrown,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onExploreMissions,
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = LeafGreen,
                    contentColor = CreamSurface,
                ),
            shape = RoundedCornerShape(14.dp),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(58.dp),
        ) {
            Text("Explorar misiones", fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
private fun GameStateMessage(
    message: String,
    accentColor: Color = RiverTeal,
    showProgress: Boolean = false,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        MissionGuide(message = message, accentColor = accentColor)
        if (showProgress) {
            Spacer(modifier = Modifier.height(36.dp))
            CircularProgressIndicator(color = RiverTeal)
        }
    }
}

@Composable
private fun MissionGuide(
    message: String,
    accentColor: Color = RiverTeal,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GuideAvatar(accentColor = accentColor)
        Spacer(modifier = Modifier.width(10.dp))
        Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(3.dp, accentColor),
            colors = CardDefaults.cardColors(containerColor = CreamSurface),
        ) {
            Text(
                text = message,
                modifier = Modifier.padding(16.dp),
                color = BarkBrown,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun GuideAvatar(accentColor: Color) {
    Box(
        modifier = Modifier.size(76.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = size.minDimension / 2
            drawCircle(color = SunYellow, radius = radius)
            drawCircle(color = accentColor, radius = radius * 0.8f)
            drawCircle(color = CreamSurface, radius = radius * 0.62f)
        }
        Icon(
            Icons.Default.Person,
            contentDescription = null,
            tint = ForestGreen,
            modifier = Modifier.size(42.dp),
        )
    }
}

@Composable
private fun ApplicationStatusCard(
    application: MyApplication,
    isCancelling: Boolean,
    onCancel: () -> Unit,
) {
    val statusStyle =
        when (application.status) {
            "Aceptado" -> StatusStyle(LeafGreen, LightLeaf, "¡Aceptado!")
            "Rechazado" -> StatusStyle(AlertCoral, LightCoral, "No seleccionado")
            else -> StatusStyle(EarthOrange, LightEarth, "Esperando respuesta")
        }

    val formattedDate =
        runCatching {
            OffsetDateTime.parse(application.mission.dateTime).format(
                DateTimeFormatter.ofPattern("d 'de' MMMM 'a las' HH:mm", Locale("es", "CO")),
            )
        }.getOrDefault(application.mission.dateTime)

    val canCancel =
        runCatching {
            val missionHasNotStarted =
                OffsetDateTime.parse(application.mission.dateTime)
                    .isAfter(OffsetDateTime.now())
            val cancellableMission =
                application.mission.status == "Publicada" ||
                    application.mission.status == "Asignada"
            val cancellableApplication =
                application.status == "Postulado" ||
                    application.status == "Aceptado"
            missionHasNotStarted && cancellableMission && cancellableApplication
        }.getOrDefault(false)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = statusStyle.background),
        border = BorderStroke(3.dp, statusStyle.accent),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    text = application.mission.title,
                    color = ForestGreen,
                    fontWeight = FontWeight.ExtraBold,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = statusStyle.label,
                    color = statusStyle.accent,
                    fontWeight = FontWeight.ExtraBold,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = formattedDate,
                color = BarkBrown,
                style = MaterialTheme.typography.bodyMedium,
            )

            if (canCancel) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onCancel,
                    enabled = !isCancelling,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(2.dp, statusStyle.accent),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ForestGreen),
                ) {
                    if (isCancelling) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = statusStyle.accent,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text(
                            text =
                                if (application.status == "Aceptado") {
                                    "Salir de la misión"
                                } else {
                                    "Cancelar postulación"
                                },
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

private data class StatusStyle(
    val accent: Color,
    val background: Color,
    val label: String,
)

private val ForestGreen = Color(0xFF214E34)
private val LeafGreen = Color(0xFF64A95C)
private val LightLeaf = Color(0xFFDCEFD3)
private val RiverTeal = Color(0xFF38A89D)
private val EarthOrange = Color(0xFFE58A3A)
private val LightEarth = Color(0xFFFFE2C2)
private val SunYellow = Color(0xFFF2C94C)
private val SandBackground = Color(0xFFF7F3E8)
private val CreamSurface = Color(0xFFFFFCF3)
private val BarkBrown = Color(0xFF594A3C)
private val AlertCoral = Color(0xFFE85D3F)
private val LightCoral = Color(0xFFFFDDD4)
