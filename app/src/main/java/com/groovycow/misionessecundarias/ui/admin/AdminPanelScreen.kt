package com.groovycow.misionessecundarias.ui.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.groovycow.misionessecundarias.domain.model.AdminMetrics
import org.koin.androidx.compose.koinViewModel

/**
 * Pantalla principal del Panel Administrativo de Moderador (HU - Monitoreo y Métricas Administrativas).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPanelScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AdminPanelViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = AdminSand,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Panel Administrativo",
                        color = AdminForest,
                        fontWeight = FontWeight.ExtraBold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = AdminForest,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::loadMetrics) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Recargar métricas",
                            tint = AdminForest,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AdminSand),
            )
        },
    ) { paddingValues ->
        Box(
            modifier =
                modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
        ) {
            when (val state = uiState) {
                is AdminPanelUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = AdminForest,
                    )
                }

                is AdminPanelUiState.AccessDenied -> {
                    AccessDeniedView(
                        onBack = onBack,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }

                is AdminPanelUiState.Error -> {
                    ErrorView(
                        onRetry = viewModel::loadMetrics,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }

                is AdminPanelUiState.Success -> {
                    MetricsDashboard(
                        metrics = state.metrics,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}

/** Dashboard ejecutivo con indicadores y gráficos operativos. */
@Composable
private fun MetricsDashboard(
    metrics: AdminMetrics,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .verticalScroll(rememberScrollState())
                .padding(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ExecutiveHeader()

        Text(
            text = "Indicadores clave",
            style = MaterialTheme.typography.titleMedium,
            color = AdminForest,
            fontWeight = FontWeight.Bold,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MetricCard(
                title = "Misiones totales",
                value = metrics.totalMissions,
                icon = Icons.Default.Star,
                accentColor = AdminForest,
                modifier = Modifier.weight(1f),
            )
            MetricCard(
                title = "Misiones activas",
                value = metrics.activeMissions,
                icon = Icons.Default.PlayArrow,
                accentColor = AdminTeal,
                modifier = Modifier.weight(1f),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MetricCard(
                title = "Completadas",
                value = metrics.completedMissions,
                icon = Icons.Default.CheckCircle,
                accentColor = AdminLeaf,
                modifier = Modifier.weight(1f),
            )
            MetricCard(
                title = "Usuarios registrados",
                value = metrics.totalUsers,
                icon = Icons.Default.Person,
                accentColor = AdminOrange,
                modifier = Modifier.weight(1f),
            )
        }

        MissionActivityChart(metrics)
        CommunityActivityChart(metrics)
        Spacer(modifier = Modifier.height(4.dp))
    }
}

@Composable
private fun ExecutiveHeader() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = AdminForest),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "PANEL DE CONTROL",
                    color = AdminLightLeaf,
                    style = MaterialTheme.typography.labelMedium,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Resumen operativo",
                    color = Color.White,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Actividad y participación de la plataforma",
                    color = AdminHeaderText,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Surface(
                color = AdminBadge,
                shape = RoundedCornerShape(50),
            ) {
                Text(
                    text = "ACCESO AUTORIZADO",
                    color = AdminForest,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                )
            }
        }
    }
}

/** Tarjeta compacta para una métrica de resumen. */
@Composable
private fun MetricCard(
    title: String,
    value: Int,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.heightIn(min = 148.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, AdminBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(38.dp)
                        .background(accentColor.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = value.toString(),
                color = AdminForest,
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 30.sp),
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                color = AdminBrown,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp, lineHeight = 15.sp),
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun MissionActivityChart(metrics: AdminMetrics) {
    val remainingMissions = (metrics.totalMissions - metrics.activeMissions - metrics.completedMissions).coerceAtLeast(0)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, AdminBorder),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ChartHeading(
                title = "Actividad de misiones",
                subtitle = "Distribución del portafolio operativo",
            )
            DistributionBar(
                label = "Activas",
                value = metrics.activeMissions,
                total = metrics.totalMissions,
                color = AdminTeal,
            )
            DistributionBar(
                label = "Completadas",
                value = metrics.completedMissions,
                total = metrics.totalMissions,
                color = AdminLeaf,
            )
            DistributionBar(
                label = "Otras",
                value = remainingMissions,
                total = metrics.totalMissions,
                color = AdminOrange,
            )
        }
    }
}

@Composable
private fun CommunityActivityChart(metrics: AdminMetrics) {
    val participationRatio =
        if (metrics.totalUsers > 0) {
            (metrics.activeSolvers.toFloat() / metrics.totalUsers.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, AdminBorder),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ChartHeading(
                title = "Participación de la comunidad",
                subtitle = "Usuarios con postulaciones aceptadas",
            )
            LinearProgressIndicator(
                progress = { participationRatio },
                modifier = Modifier.fillMaxWidth().height(9.dp),
                color = AdminForest,
                trackColor = AdminLightLeaf,
            )
            Text(
                text = "${metrics.activeSolvers} de ${metrics.totalUsers} usuarios",
                color = AdminBrown,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp, lineHeight = 15.sp),
            )
        }
    }
}

@Composable
private fun ChartHeading(
    title: String,
    subtitle: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            color = AdminForest,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = subtitle,
            color = AdminMuted,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp, lineHeight = 15.sp),
        )
    }
}

@Composable
private fun DistributionBar(
    label: String,
    value: Int,
    total: Int,
    color: Color,
) {
    val ratio = if (total > 0) (value.toFloat() / total.toFloat()).coerceIn(0f, 1f) else 0f

    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                color = AdminBrown,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp, lineHeight = 15.sp),
            )
            Text(
                text = value.toString(),
                color = AdminForest,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
        }
        LinearProgressIndicator(
            progress = { ratio },
            modifier = Modifier.fillMaxWidth().height(7.dp),
            color = color,
            trackColor = AdminTrack,
        )
    }
}

/** Vista de Acceso Denegado cuando el rol no es autorizado. */
@Composable
private fun AccessDeniedView(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth().padding(16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = AdminCream),
        border = BorderStroke(2.dp, AdminCoral),
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = AdminCoral,
                modifier = Modifier.size(56.dp),
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Acceso Restringido",
                color = AdminCoral,
                fontWeight = FontWeight.ExtraBold,
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Esta sección está reservada exclusivamente para personal con rol de Moderador o Administrador.",
                color = AdminBrown,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AdminForest),
            ) {
                Text("Volver", fontWeight = FontWeight.Bold)
            }
        }
    }
}

/** Vista de error genérico. */
@Composable
private fun ErrorView(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "No se pudieron cargar las métricas operativas.",
            color = AdminCoral,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = AdminForest),
        ) {
            Text("Reintentar")
        }
    }
}

private val AdminForest = Color(0xFF214E34)
private val AdminLeaf = Color(0xFF64A95C)
private val AdminLightLeaf = Color(0xFFDCEFD3)
private val AdminSand = Color(0xFFF7F3E8)
private val AdminCream = Color(0xFFFFFCF3)
private val AdminBrown = Color(0xFF594A3C)
private val AdminCoral = Color(0xFFE85D3F)
private val AdminTeal = Color(0xFF38A89D)
private val AdminOrange = Color(0xFFE58A3A)
private val AdminHeaderText = Color(0xFFE5EFE8)
private val AdminBadge = Color(0xFFE5EFE8)
private val AdminBorder = Color(0xFFE4EAE5)
private val AdminMuted = Color(0xFF68766D)
private val AdminTrack = Color(0xFFE7ECE8)
