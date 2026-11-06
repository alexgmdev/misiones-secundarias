package com.groovycow.misionessecundarias.ui.missions

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.groovycow.misionessecundarias.domain.model.Mission
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MissionListScreen(
    onMissionClick: (Mission) -> Unit,
    onMyApplicationsClick: () -> Unit = {},
    onBack: () -> Unit = {},
    viewModel: MissionViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var selectedModality by remember { mutableStateOf<String?>(null) }
    var filterMenuExpanded by remember { mutableStateOf(false) }

    val visibleMissions =
        remember(uiState.missions, searchQuery, selectedModality) {
            uiState.missions.filter { mission ->
                val matchesSearch =
                    searchQuery.isBlank() ||
                        mission.title.contains(searchQuery, ignoreCase = true)
                val matchesModality =
                    selectedModality == null ||
                        mission.modality.equals(selectedModality, ignoreCase = true)
                matchesSearch && matchesModality
            }
        }

    Scaffold(
        containerColor = SandBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Misiones",
                        color = ForestGreen,
                        fontWeight = FontWeight.ExtraBold,
                    )
                },
                actions = {
                    TextButton(onClick = onMyApplicationsClick) {
                        Text("Estado", color = RiverTeal, fontWeight = FontWeight.Bold)
                    }
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Buscar una misión") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null)
                    },
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
                            Modifier
                                .size(56.dp)
                                .background(LightLeaf, RoundedCornerShape(14.dp))
                                .border(2.dp, LeafGreen, RoundedCornerShape(14.dp)),
                    ) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Filtrar misiones",
                            tint = ForestGreen,
                        )
                    }
                    DropdownMenu(
                        expanded = filterMenuExpanded,
                        onDismissRequest = { filterMenuExpanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("Todas") },
                            onClick = {
                                selectedModality = null
                                filterMenuExpanded = false
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Misiones de campo") },
                            onClick = {
                                selectedModality = "campo"
                                filterMenuExpanded = false
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Misiones remotas") },
                            onClick = {
                                selectedModality = "remota"
                                filterMenuExpanded = false
                            },
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            MissionMapPlaceholder()
            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text =
                    if (selectedModality == null) {
                        "Misiones disponibles"
                    } else if (selectedModality == "campo") {
                        "Misiones de campo"
                    } else {
                        "Misiones remotas"
                    },
                style = MaterialTheme.typography.titleLarge,
                color = ForestGreen,
                fontWeight = FontWeight.ExtraBold,
            )
            Spacer(modifier = Modifier.height(10.dp))

            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        color = RiverTeal,
                    )
                }

                uiState.error != null -> {
                    MissionListMessage(
                        message = uiState.error!!,
                        borderColor = AlertCoral,
                    )
                }

                visibleMissions.isEmpty() -> {
                    MissionListMessage(
                        message =
                            if (searchQuery.isBlank() && selectedModality == null) {
                                "No hay misiones disponibles todavía. ¡Vuelve pronto para una nueva aventura!"
                            } else {
                                "No encontramos misiones con esos criterios."
                            },
                        borderColor = EarthOrange,
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(visibleMissions, key = { it.id }) { mission ->
                            MissionCard(
                                mission = mission,
                                onClick = { onMissionClick(mission) },
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
private fun MissionMapPlaceholder() {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(205.dp),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(3.dp, RiverTeal),
        colors = CardDefaults.cardColors(containerColor = LightRiver),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    color = LightLeaf,
                    radius = size.minDimension * 0.32f,
                    center = Offset(size.width * 0.22f, size.height * 0.3f),
                )
                drawCircle(
                    color = CreamSurface,
                    radius = size.minDimension * 0.27f,
                    center = Offset(size.width * 0.78f, size.height * 0.72f),
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
                text = "ESPACIO PARA MAPLIBRE",
                color = ForestGreen,
                fontWeight = FontWeight.ExtraBold,
                style = MaterialTheme.typography.labelLarge,
                modifier =
                    Modifier
                        .align(Alignment.TopStart)
                        .padding(14.dp)
                        .background(CreamSurface.copy(alpha = 0.9f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
            )

            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = "Ubicación aproximada de misión",
                tint = AlertCoral,
                modifier =
                    Modifier
                        .size(54.dp)
                        .align(Alignment.Center),
            )
        }
    }
}

@Composable
fun MissionCard(
    mission: Mission,
    onClick: () -> Unit,
) {
    val formattedDate = formatMissionDateTime(mission.dateTime)

    val isRemote = mission.modality.equals("remota", ignoreCase = true)
    val accentColor = if (isRemote) RiverTeal else LeafGreen
    val backgroundColor = if (isRemote) LightRiver else LightLeaf

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
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
                    modifier =
                        Modifier
                            .background(accentColor, RoundedCornerShape(20.dp))
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = formattedDate,
                color = BarkBrown,
                style = MaterialTheme.typography.bodyMedium,
            )
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
private val MapLine = Color(0xFF7ECAC2)
