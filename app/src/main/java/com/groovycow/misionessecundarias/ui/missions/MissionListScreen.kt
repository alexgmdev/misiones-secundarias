package com.groovycow.misionessecundarias.ui.missions

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.groovycow.misionessecundarias.domain.model.Mission
import com.groovycow.misionessecundarias.domain.util.parseWktPoint
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MissionListScreen(
    onMissionClick: (Mission) -> Unit,
    onMyApplicationsClick: () -> Unit = {},
    onBack: () -> Unit = {},
    viewModel: MissionViewModel = koinViewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadMissions()
    }

    var searchQuery by remember { mutableStateOf("") }
    var selectedModality by remember { mutableStateOf<String?>(null) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var selectedDistance by remember { mutableStateOf<Int?>(null) } // 1, 5, 10 km
    var userLocation by remember { mutableStateOf<Location?>(null) }
    var filterMenuExpanded by remember { mutableStateOf(false) }

    // 1. Extraer categorías dinámicamente de las misiones cargadas
    val availableCategories =
        remember(uiState.missions) {
            uiState.missions.mapNotNull { it.category?.trim()?.takeIf { cat -> cat.isNotBlank() } }.distinct()
        }

    // 2. Manejo de Permisos y GPS
    val locationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    val locationPermissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
        ) { isGranted ->
            if (isGranted) {
                // Si acepta, obtener ubicación
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    locationClient.lastLocation.addOnSuccessListener { location ->
                        userLocation = location
                    }
                }
            }
        }

    // Función para calcular distancia en km
    fun getDistanceInKm(
        missionLat: Double,
        missionLon: Double,
    ): Double {
        val userLat = userLocation?.latitude ?: return Double.MAX_VALUE
        val userLon = userLocation?.longitude ?: return Double.MAX_VALUE

        val results = FloatArray(1)
        Location.distanceBetween(userLat, userLon, missionLat, missionLon, results)
        return results[0] / 1000.0 // Convertir metros a km
    }

    // 3. Lógica de Filtrado (HU-029 + HU-030)
    val visibleMissions =
        remember(uiState.missions, searchQuery, selectedModality, selectedCategory, selectedDistance, userLocation) {
            uiState.missions.filter { mission ->
                // Búsqueda (Título y Descripción)
                val matchesSearch =
                    searchQuery.isBlank() ||
                        mission.title.contains(searchQuery, ignoreCase = true) ||
                        mission.description.contains(searchQuery, ignoreCase = true)

                // Modalidad
                val matchesModality =
                    selectedModality == null ||
                        mission.modality.equals(selectedModality, ignoreCase = true)

                // Categoría
                val matchesCategory =
                    selectedCategory == null ||
                        mission.category?.trim().equals(selectedCategory, ignoreCase = true)

                // Distancia
                var matchesDistance = true
                if (selectedDistance != null && userLocation != null) {
                    val point = parseWktPoint(mission.location)
                    if (point != null) {
                        val distance = getDistanceInKm(point.latitude, point.longitude)
                        matchesDistance = distance <= selectedDistance!!
                    } else {
                        matchesDistance = false // Misiones remotas sin coordenadas se ocultan si hay filtro de distancia
                    }
                } else if (selectedDistance != null && userLocation == null) {
                    matchesDistance = true // Si no hay GPS pero el filtro está activo, dejamos pasar hasta que cargue
                }

                matchesSearch && matchesModality && matchesCategory && matchesDistance
            }
        }

    Scaffold(
        containerColor = SandBackground,
        topBar = {
            TopAppBar(
                title = { Text("Misiones", color = ForestGreen, fontWeight = FontWeight.ExtraBold) },
                actions = {
                    TextButton(onClick = onMyApplicationsClick) {
                        Text("Estado", color = RiverTeal, fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = EarthOrange)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SandBackground),
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Buscar misión...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors =
                        OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RiverTeal,
                            unfocusedBorderColor = LeafGreen,
                            focusedContainerColor = CreamSurface,
                            unfocusedContainerColor = CreamSurface,
                        ),
                )
                Spacer(modifier = Modifier.width(10.dp))
                Box {
                    IconButton(
                        onClick = { filterMenuExpanded = true },
                        modifier =
                            Modifier.size(56.dp)
                                .background(LightLeaf, RoundedCornerShape(14.dp))
                                .border(2.dp, LeafGreen, RoundedCornerShape(14.dp)),
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = "Filtrar misiones", tint = ForestGreen)
                    }

                    DropdownMenu(
                        expanded = filterMenuExpanded,
                        onDismissRequest = { filterMenuExpanded = false },
                    ) {
                        // 1. Modalidad
                        Text(
                            "Modalidad",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            fontWeight = FontWeight.Bold,
                            color = ForestGreen,
                        )
                        DropdownMenuItem(text = { Text("Todas") }, onClick = { selectedModality = null })
                        DropdownMenuItem(text = { Text("Campo") }, onClick = { selectedModality = "campo" })
                        DropdownMenuItem(text = { Text("Remota") }, onClick = { selectedModality = "remota" })

                        HorizontalDivider()

                        // 2. Distancia (HU-029) - Requiere GPS
                        Text(
                            "Distancia máxima",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            fontWeight = FontWeight.Bold,
                            color = ForestGreen,
                        )
                        DropdownMenuItem(
                            text = { Text("Sin límite") },
                            onClick = {
                                selectedDistance = null
                                filterMenuExpanded = false
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Hasta 1 km") },
                            onClick = {
                                selectedDistance = 1
                                filterMenuExpanded = false
                                // Pedir permiso al elegir distancia
                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                                    locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                                } else {
                                    locationClient.lastLocation.addOnSuccessListener { userLocation = it }
                                }
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Hasta 5 km") },
                            onClick = {
                                selectedDistance = 5
                                filterMenuExpanded = false
                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                                    locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                                } else {
                                    locationClient.lastLocation.addOnSuccessListener { userLocation = it }
                                }
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Hasta 10 km") },
                            onClick = {
                                selectedDistance = 10
                                filterMenuExpanded = false
                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                                    locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                                } else {
                                    locationClient.lastLocation.addOnSuccessListener { userLocation = it }
                                }
                            },
                        )

                        if (availableCategories.isNotEmpty()) {
                            HorizontalDivider()
                            // 3. Categoría (Dinámica)
                            Text(
                                "Categoría",
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                fontWeight = FontWeight.Bold,
                                color = ForestGreen,
                            )
                            DropdownMenuItem(text = { Text("Todas") }, onClick = { selectedCategory = null })
                            availableCategories.forEach { category ->
                                DropdownMenuItem(
                                    text = { Text(category) },
                                    onClick = {
                                        selectedCategory = category
                                        filterMenuExpanded = false
                                    },
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            MissionMapScreen(
                missions = visibleMissions,
                onMissionClick = onMissionClick,
                modifier = Modifier.fillMaxWidth().height(250.dp),
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Misiones disponibles (${visibleMissions.size})",
                style = MaterialTheme.typography.titleLarge,
                color = ForestGreen,
                fontWeight = FontWeight.ExtraBold,
            )
            Spacer(modifier = Modifier.height(10.dp))

            when {
                uiState.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally), color = RiverTeal)
                uiState.error != null -> MissionListMessage(message = uiState.error!!, borderColor = AlertCoral)
                visibleMissions.isEmpty() ->
                    MissionListMessage(
                        message = "No encontramos misiones con esos criterios.",
                        borderColor = EarthOrange,
                    )
                else -> {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(visibleMissions, key = { it.id }) { mission ->
                            MissionCard(
                                mission = mission,
                                onClick = { onMissionClick(mission) },
                                distance =
                                    if (userLocation != null && mission.location != null) {
                                        parseWktPoint(mission.location)?.let { point -> getDistanceInKm(point.latitude, point.longitude) }
                                    } else {
                                        null
                                    },
                            )
                        }
                        item { Spacer(modifier = Modifier.height(12.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
fun MissionCard(
    mission: Mission,
    onClick: () -> Unit,
    distance: Double? = null, // NUEVO
) {
    val formattedDate = formatMissionDateTime(mission.dateTime)
    val isRemote = mission.modality.equals("remota", ignoreCase = true)
    val accentColor = if (isRemote) RiverTeal else LeafGreen
    val backgroundColor = if (isRemote) LightRiver else LightLeaf

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(3.dp, accentColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = mission.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = ForestGreen,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isRemote) "Remota" else "Campo",
                    color = CreamSurface,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.background(accentColor, RoundedCornerShape(20.dp)).padding(horizontal = 10.dp, vertical = 5.dp),
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = formattedDate, color = BarkBrown, style = MaterialTheme.typography.bodyMedium)

            // NUEVO: Mostrar distancia si está disponible
            if (distance != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "A ${String.format("%.1f", distance)} km de ti",
                    color = RiverTeal,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                )
            }

            mission.category?.takeIf { it.isNotBlank() }?.let { category ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = category, color = accentColor, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun MissionListMessage(
    message: String,
    borderColor: Color,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CreamSurface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(3.dp, borderColor),
    ) {
        Text(text = message, modifier = Modifier.padding(20.dp), color = BarkBrown, fontWeight = FontWeight.SemiBold)
    }
}

private val ForestGreen = Color(0xFF214E34)
private val LeafGreen = Color(0xFF64A95C)
private val LightLeaf = Color(0xFFDCEFD3)
private val RiverTeal = Color(0xFF38A89D)
private val LightRiver = Color(0xFFCDEDEA)
private val EarthOrange = Color(0xFFE58A3A)
private val SandBackground = Color(0xFFF7F3E8)
private val CreamSurface = Color(0xFFFFFCF3)
private val BarkBrown = Color(0xFF594A3C)
private val AlertCoral = Color(0xFFE85D3F)
