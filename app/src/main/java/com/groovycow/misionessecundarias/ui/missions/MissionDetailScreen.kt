package com.groovycow.misionessecundarias.ui.missions

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.groovycow.misionessecundarias.domain.util.parseWktPoint
import com.groovycow.misionessecundarias.ui.applications.ApplicationUiState
import com.groovycow.misionessecundarias.ui.applications.ApplicationViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun MissionDetailRoute(
    missionId: String,
    publisherId: String,
    missionLocationWkt: String?,
    missionTitle: String,
    missionDescription: String,
    missionDateTime: String,
    maxParticipants: Int,
    modality: String,
    category: String?,
    requirements: String?,
    publisherName: String?,
    approximateZone: String,
    onBack: () -> Unit,
    onManageApplicants: () -> Unit,
    applicationViewModel: ApplicationViewModel = koinViewModel(),
) {
    val applicationState by applicationViewModel.uiState.collectAsState()

    LaunchedEffect(missionId) {
        applicationViewModel.loadMission(publisherId, missionId)
    }

    MissionDetailScreen(
        missionId = missionId,
        missionLocationWkt = missionLocationWkt,
        missionTitle = missionTitle,
        missionDescription = missionDescription,
        missionDateTime = missionDateTime,
        maxParticipants = maxParticipants,
        modality = modality,
        category = category,
        requirements = requirements,
        publisherName = publisherName,
        approximateZone = approximateZone,
        onBack = onBack,
        onManageApplicants = onManageApplicants,
        onApply = { applicationViewModel.applyToMission(missionId) },
        onMarkOnWay = { applicationViewModel.markOnWay(missionId) },
        onArrived = { lat, lon -> applicationViewModel.markArrived(missionId, lat, lon) },
        onDismissApplicationMessage = applicationViewModel::dismissMessage,
        applicationState = applicationState,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MissionDetailScreen(
    missionId: String,
    missionLocationWkt: String?,
    missionTitle: String,
    missionDescription: String,
    missionDateTime: String,
    maxParticipants: Int,
    modality: String,
    category: String?,
    requirements: String?,
    publisherName: String?,
    approximateZone: String,
    onBack: () -> Unit,
    onManageApplicants: () -> Unit = {},
    onApply: () -> Unit = {},
    onMarkOnWay: () -> Unit = {},
    onArrived: (Double?, Double?) -> Unit = { _, _ -> },
    onDismissApplicationMessage: () -> Unit = {},
    applicationState: ApplicationUiState = ApplicationUiState(),
) {
    val formattedDate = formatMissionDateTime(missionDateTime)

    val context = LocalContext.current
    val locationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    var distanceError by remember { mutableStateOf<String?>(null) }

    val locationPermissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
        ) { isGranted ->
            if (isGranted) {
                if (ContextCompat.checkSelfPermission
                        (context, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED
                ) {
                    locationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                        .addOnSuccessListener { location ->
                            if (location != null) {
                                validateArrival(location, missionLocationWkt) { success, msg ->
                                    if (success) {
                                        onArrived(location.latitude, location.longitude)
                                    } else {
                                        distanceError = msg
                                    }
                                }
                            }
                        }
                }
            }
        }

    Scaffold(
        containerColor = SandBackground,
        topBar = {
            TopAppBar(
                modifier = Modifier.statusBarsPadding(),
                title = { Text("Misión", color = ForestGreen, fontWeight = FontWeight.ExtraBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = EarthOrange)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SandBackground),
                windowInsets = WindowInsets(0, 0, 0, 0),
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 16.dp).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = missionTitle,
                style = MaterialTheme.typography.headlineSmall,
                color = ForestGreen,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
            )

            category?.takeIf { it.isNotBlank() }?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = it,
                    color = CreamSurface,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.background(RiverTeal, RoundedCornerShape(20.dp)).padding(horizontal = 14.dp, vertical = 6.dp),
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            PublisherCard(publisherName = publisherName)
            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Empieza el " + formattedDate,
                modifier = Modifier.fillMaxWidth(),
                color = BarkBrown,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )

            Spacer(modifier = Modifier.height(12.dp))
            MissionLocationPanel(modality = modality)
            Spacer(modifier = Modifier.height(16.dp))

            InfoLabel(
                title = "Tipo de misión",
                value = if (modality.equals("remota", ignoreCase = true)) "Misión remota" else "Misión de campo",
                accentColor = RiverTeal,
                backgroundColor = LightRiver,
            )

            Spacer(modifier = Modifier.height(14.dp))

            if (modality.equals("campo", ignoreCase = true)) {
                InfoSection(
                    title = "Ubicación aproximada",
                    text = "$approximateZone\n\n(La dirección exacta se revelará al ser aceptado)",
                    accentColor = EarthOrange,
                    backgroundColor = LightEarth,
                )
                Spacer(modifier = Modifier.height(14.dp))
            }

            InfoSection(
                title = "Para esta misión necesitarás",
                text = requirements?.takeIf { it.isNotBlank() } ?: "No hay requerimientos específicos.",
                accentColor = LeafGreen,
                backgroundColor = LightLeaf,
            )

            Spacer(modifier = Modifier.height(14.dp))
            InfoSection(
                title = "Descripción de la misión",
                text = missionDescription,
                accentColor = EarthOrange,
                backgroundColor = LightEarth,
            )

            Spacer(modifier = Modifier.height(14.dp))
            MissionRewardsNotice()

            Spacer(modifier = Modifier.height(14.dp))
            InfoSection(
                title = "Participantes",
                text = "Cupo máximo: " + maxParticipants + " participantes. Los participantes aceptados aparecerán aquí.",
                accentColor = RiverTeal,
                backgroundColor = LightRiver,
            )

            Spacer(modifier = Modifier.height(18.dp))

            if (applicationState.isAccepted) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = LightEarth),
                    border = BorderStroke(2.dp, EarthOrange),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Panel de Asistencia", fontWeight = FontWeight.Bold, color = ForestGreen)
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = applicationState.isOnWay,
                                onCheckedChange = { isChecked ->
                                    if (isChecked && !applicationState.isOnWay) onMarkOnWay()
                                },
                                enabled = !applicationState.isOnWay && !applicationState.isUpdatingStatus,
                            )
                            Text("Voy en camino", fontWeight = FontWeight.SemiBold)
                        }

                        Button(
                            onClick = {
                                if (ContextCompat.checkSelfPermission
                                        (context, Manifest.permission.ACCESS_FINE_LOCATION)
                                    != PackageManager.PERMISSION_GRANTED
                                ) {
                                    locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                                } else {
                                    locationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                                        .addOnSuccessListener { location ->
                                            if (location != null) {
                                                validateArrival(location, missionLocationWkt) { success, msg ->
                                                    if (success) {
                                                        onArrived(location.latitude, location.longitude)
                                                    } else {
                                                        distanceError = msg
                                                    }
                                                }
                                            }
                                        }
                                }
                            },
                            enabled =
                                applicationState.isOnWay && !applicationState.hasArrived &&
                                    !applicationState.isUpdatingStatus,
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            colors =
                                ButtonDefaults.buttonColors(
                                    containerColor =
                                        if (applicationState.hasArrived) {
                                            LeafGreen
                                        } else {
                                            EarthOrange
                                        },
                                ),
                        ) {
                            if (applicationState.isUpdatingStatus) {
                                CircularProgressIndicator(
                                    modifier =
                                        Modifier.size(20.dp),
                                    color = CreamSurface,
                                )
                            } else {
                                Text(
                                    if (applicationState.hasArrived) {
                                        "¡Llegada Registrada!"
                                    } else {
                                        "Marcar Llegué (GPS < 50m)"
                                    },
                                )
                            }
                        }
                    }
                }
            } else {
                Button(
                    onClick = if (applicationState.isPublisher) onManageApplicants else onApply,
                    enabled = applicationState.canPerformPrimaryAction(),
                    modifier = Modifier.fillMaxWidth().height(58.dp),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = LeafGreen,
                            contentColor = CreamSurface,
                            disabledContainerColor = LightLeaf,
                            disabledContentColor = ForestGreen,
                        ),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    if (applicationState.isCheckingOwnership || applicationState.isSubmitting) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = CreamSurface, strokeWidth = 2.dp)
                    } else {
                        Text(
                            text =
                                when {
                                    applicationState.isPublisher -> "Gestionar postulantes"
                                    applicationState.wasSubmitted -> "Postulación enviada"
                                    else -> "Postularse a la misión"
                                },
                            fontWeight = FontWeight.ExtraBold,
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    distanceError?.let { error ->
        AlertDialog(
            onDismissRequest = { distanceError = null },
            containerColor = CreamSurface,
            title = { Text("Muy lejos", color = AlertCoral, fontWeight = FontWeight.ExtraBold) },
            text = { Text(error, color = BarkBrown) },
            confirmButton = {
                TextButton(
                    onClick = { distanceError = null },
                ) { Text("Entendido", color = ForestGreen, fontWeight = FontWeight.Bold) }
            },
        )
    }

    applicationState.message?.let { message ->
        AlertDialog(
            onDismissRequest = onDismissApplicationMessage,
            containerColor = CreamSurface,
            title = {
                Text(
                    text = if (applicationState.isError) "Error" else "Información",
                    color = if (applicationState.isError) AlertCoral else ForestGreen,
                    fontWeight = FontWeight.ExtraBold,
                )
            },
            text = { Text(text = message, color = BarkBrown) },
            confirmButton = {
                TextButton(onClick = onDismissApplicationMessage) { Text("Aceptar", color = ForestGreen, fontWeight = FontWeight.Bold) }
            },
        )
    }
}

private fun ApplicationUiState.canPerformPrimaryAction(): Boolean =
    !isCheckingOwnership && (isPublisher || (!isSubmitting && !wasSubmitted))

private fun validateArrival(
    currentLocation: Location,
    missionWkt: String?,
    onResult: (Boolean, String?) -> Unit,
) {
    if (missionWkt == null) {
        onResult(true, null)
        return
    }
    val missionPoint = parseWktPoint(missionWkt)
    if (missionPoint != null) {
        val results = FloatArray(1)
        Location.distanceBetween(
            currentLocation.latitude,
            currentLocation.longitude,
            missionPoint.latitude,
            missionPoint.longitude,
            results,
        )
        if (results[0] <= 50.0) {
            onResult(true, null)
        } else {
            onResult(false, "Estás a ${results[0].toInt()} metros. Debes estar a menos de 50 metros para marcar llegada.")
        }
    } else {
        onResult(true, null)
    }
}

// ... Aquí van tus composables PublisherCard, MissionLocationPanel, InfoLabel, InfoSection ...
// Si los borraste, te los vuelvo a poner aquí debajo. Asegúrate de tenerlos.

@Composable
private fun PublisherCard(publisherName: String?) {
    Row(
        modifier =
            Modifier.fillMaxWidth().background(
                CreamSurface,
                RoundedCornerShape(14.dp),
            ).border(2.dp, EarthOrange, RoundedCornerShape(14.dp)).padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Publicado por", color = BarkBrown, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = publisherName ?: "Usuario desconocido",
            color = EarthOrange,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun MissionLocationPanel(modality: String) {
    val isRemote = modality.equals("remota", ignoreCase = true)
    Card(
        modifier = Modifier.fillMaxWidth().height(190.dp),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(3.dp, RiverTeal),
        colors = CardDefaults.cardColors(containerColor = LightRiver),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (isRemote) {
                Text(
                    "Esta misión es remota y no requiere una ubicación física.",
                    color = ForestGreen,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(24.dp),
                )
            } else {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        color = LightLeaf,
                        radius = size.minDimension * 0.34f,
                        center = Offset(size.width * 0.2f, size.height * 0.3f),
                    )
                    drawCircle(
                        color = CreamSurface,
                        radius = size.minDimension * 0.28f,
                        center = Offset(size.width * 0.78f, size.height * 0.73f),
                    )
                    for (step in 0..7) {
                        val x = size.width * step / 7f
                        drawLine(
                            color = MapLine,
                            start = Offset(x, 0f),
                            end = Offset((x + size.width * 0.25f).coerceAtMost(size.width), size.height),
                            strokeWidth = 4f,
                        )
                    }
                    for (step in 0..5) {
                        val y = size.height * step / 5f
                        drawLine(
                            color = MapLine,
                            start = Offset(0f, y),
                            end = Offset(size.width, (y + size.height * 0.2f).coerceAtMost(size.height)),
                            strokeWidth = 4f,
                        )
                    }
                }
                Text(
                    "MAPA DE UBICACIÓN",
                    color = ForestGreen,
                    fontWeight = FontWeight.ExtraBold,
                    modifier =
                        Modifier.align(
                            Alignment.TopStart,
                        ).padding(
                            12.dp,
                        ).background(
                            CreamSurface.copy(alpha = 0.9f),
                            RoundedCornerShape(8.dp),
                        ).padding(horizontal = 10.dp, vertical = 5.dp),
                )
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = "Ubicación aproximada",
                    tint = AlertCoral,
                    modifier = Modifier.size(54.dp),
                )
            }
        }
    }
}

@Composable
private fun InfoLabel(
    title: String,
    value: String,
    accentColor: Color,
    backgroundColor: Color,
) {
    Row(
        modifier =
            Modifier.fillMaxWidth().background(
                backgroundColor,
                RoundedCornerShape(14.dp),
            ).border(2.dp, accentColor, RoundedCornerShape(14.dp)).padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, color = BarkBrown, fontWeight = FontWeight.Bold)
        Text(value, color = accentColor, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
private fun InfoSection(
    title: String,
    text: String,
    accentColor: Color,
    backgroundColor: Color,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(title, color = ForestGreen, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(7.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(2.dp, accentColor),
            colors = CardDefaults.cardColors(containerColor = backgroundColor),
        ) {
            Text(text, modifier = Modifier.padding(16.dp), color = BarkBrown, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

private val ForestGreen = Color(0xFF214E34)
private val LeafGreen = Color(0xFF64A95C)
private val LightLeaf = Color(0xFFDCEFD3)
private val RiverTeal = Color(0xFF38A89D)
private val LightRiver = Color(0xFFCDEDEA)
private val EarthOrange = Color(0xFFE58A3A)
private val LightEarth = Color(0xFFFFE2C2)
private val SandBackground = Color(0xFFF7F3E8)
private val CreamSurface = Color(0xFFFFFCF3)
private val BarkBrown = Color(0xFF594A3C)
private val AlertCoral = Color(0xFFE85D3F)
private val MapLine = Color(0xFF7ECAC2)
