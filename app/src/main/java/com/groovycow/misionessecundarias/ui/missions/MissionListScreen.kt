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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
    var selectedDistance by remember { mutableStateOf<Int?>(null) }
    var userLocation by remember { mutableStateOf<Location?>(null) }

    val availableCategories =
        remember(uiState.missions) {
            uiState.missions.mapNotNull { it.category?.trim()?.takeIf { cat -> cat.isNotBlank() } }.distinct()
        }

    val locationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    val locationPermissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
        ) { isGranted ->
            if (isGranted) {
                if (ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    locationClient.lastLocation.addOnSuccessListener { location ->
                        userLocation = location
                    }
                }
            }
        }

    val getDistanceInKm = { lat: Double, lon: Double ->
        val userLat = userLocation?.latitude
        val userLon = userLocation?.longitude
        if (userLat != null && userLon != null) {
            val results = FloatArray(1)
            Location.distanceBetween(userLat, userLon, lat, lon, results)
            results[0] / 1000.0
        } else {
            Double.MAX_VALUE
        }
    }

    val visibleMissions =
        remember(uiState.missions, searchQuery, selectedModality, selectedCategory, selectedDistance, userLocation) {
            filterMissions(
                missions = uiState.missions,
                searchQuery = searchQuery,
                selectedModality = selectedModality,
                selectedCategory = selectedCategory,
                selectedDistance = selectedDistance,
                userLocation = userLocation,
                getDistance = getDistanceInKm,
            )
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
            MissionSearchAndFilterBar(
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                onModalityChange = { selectedModality = it },
                onCategoryChange = { selectedCategory = it },
                onDistanceChange = { distance ->
                    selectedDistance = distance
                    if (distance != null && ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.ACCESS_FINE_LOCATION,
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    } else if (distance != null) {
                        locationClient.lastLocation.addOnSuccessListener { userLocation = it }
                    }
                },
                availableCategories = availableCategories,
            )

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

            MissionListContent(
                uiState = uiState,
                visibleMissions = visibleMissions,
                userLocation = userLocation,
                getDistanceInKm = getDistanceInKm,
                onMissionClick = onMissionClick,
            )
        }
    }
}

private fun filterMissions(
    missions: List<Mission>,
    searchQuery: String,
    selectedModality: String?,
    selectedCategory: String?,
    selectedDistance: Int?,
    userLocation: Location?,
    getDistance: (Double, Double) -> Double,
): List<Mission> {
    return missions.filter { mission ->
        val matchesSearch =
            searchQuery.isBlank() ||
                mission.title.contains(searchQuery, ignoreCase = true) ||
                mission.description.contains(searchQuery, ignoreCase = true)

        val matchesModality =
            selectedModality == null ||
                mission.modality.equals(selectedModality, ignoreCase = true)

        val matchesCategory =
            selectedCategory == null ||
                mission.category?.trim().equals(selectedCategory, ignoreCase = true)

        var matchesDistance = true
        if (selectedDistance != null && userLocation != null) {
            val point = parseWktPoint(mission.location)
            if (point != null) {
                val distance = getDistance(point.latitude, point.longitude)
                matchesDistance = distance <= selectedDistance
            } else {
                matchesDistance = false
            }
        }

        matchesSearch && matchesModality && matchesCategory && matchesDistance
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MissionSearchAndFilterBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onModalityChange: (String?) -> Unit,
    onCategoryChange: (String?) -> Unit,
    onDistanceChange: (Int?) -> Unit,
    availableCategories: List<String>,
) {
    var filterMenuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
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
                Text(
                    "Modalidad",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    fontWeight = FontWeight.Bold,
                    color = ForestGreen,
                )
                DropdownMenuItem(
                    text = { Text("Todas") },
                    onClick = {
                        onModalityChange(null)
                        filterMenuExpanded = false
                    },
                )
                DropdownMenuItem(
                    text = { Text("Campo") },
                    onClick = {
                        onModalityChange("campo")
                        filterMenuExpanded = false
                    },
                )
                DropdownMenuItem(
                    text = { Text("Remota") },
                    onClick = {
                        onModalityChange("remota")
                        filterMenuExpanded = false
                    },
                )

                HorizontalDivider()

                Text(
                    "Distancia máxima",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    fontWeight = FontWeight.Bold,
                    color = ForestGreen,
                )
                DropdownMenuItem(
                    text = { Text("Sin límite") },
                    onClick = {
                        onDistanceChange(null)
                        filterMenuExpanded = false
                    },
                )
                DropdownMenuItem(
                    text = { Text("Hasta 1 km") },
                    onClick = {
                        onDistanceChange(1)
                        filterMenuExpanded = false
                    },
                )
                DropdownMenuItem(
                    text = { Text("Hasta 5 km") },
                    onClick = {
                        onDistanceChange(5)
                        filterMenuExpanded = false
                    },
                )
                DropdownMenuItem(
                    text = { Text("Hasta 10 km") },
                    onClick = {
                        onDistanceChange(10)
                        filterMenuExpanded = false
                    },
                )

                if (availableCategories.isNotEmpty()) {
                    HorizontalDivider()
                    Text(
                        "Categoría",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        fontWeight = FontWeight.Bold,
                        color = ForestGreen,
                    )
                    DropdownMenuItem(
                        text = { Text("Todas") },
                        onClick = {
                            onCategoryChange(null)
                            filterMenuExpanded = false
                        },
                    )
                    availableCategories.forEach { category ->
                        DropdownMenuItem(
                            text = { Text(category) },
                            onClick = {
                                onCategoryChange(category)
                                filterMenuExpanded = false
                            },
                        )
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
    distance: Double? = null,
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
                Text(
                    text = category,
                    color = accentColor,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
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
        Text(
            text = message,
            modifier = Modifier.padding(20.dp),
            color = BarkBrown,
            fontWeight = FontWeight.SemiBold,
        )
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

@Composable
private fun MissionListContent(
    uiState: MissionUiState,
    visibleMissions: List<Mission>,
    userLocation: Location?,
    getDistanceInKm: (Double, Double) -> Double,
    onMissionClick: (Mission) -> Unit,
) {
    when {
        uiState.isLoading -> {
            CircularProgressIndicator(
                modifier = Modifier.fillMaxWidth().padding(32.dp),
                color = RiverTeal,
            )
        }
        uiState.error != null -> {
            MissionListMessage(message = uiState.error, borderColor = AlertCoral)
        }
        visibleMissions.isEmpty() -> {
            MissionListMessage(
                message = "No encontramos misiones con esos criterios.",
                borderColor = EarthOrange,
            )
        }
        else -> {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(visibleMissions, key = { it.id }) { mission ->
                    MissionCard(
                        mission = mission,
                        onClick = { onMissionClick(mission) },
                        distance =
                            if (userLocation != null && mission.location != null) {
                                parseWktPoint(mission.location)?.let { point ->
                                    getDistanceInKm(point.latitude, point.longitude)
                                }
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
