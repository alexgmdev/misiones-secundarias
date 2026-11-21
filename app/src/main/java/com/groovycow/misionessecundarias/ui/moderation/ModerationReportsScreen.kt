package com.groovycow.misionessecundarias.ui.moderation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.groovycow.misionessecundarias.domain.model.ModerationReport
import com.groovycow.misionessecundarias.domain.model.ReportPriority
import com.groovycow.misionessecundarias.domain.model.ReportSortOrder
import com.groovycow.misionessecundarias.domain.model.ReportStatus
import org.koin.androidx.compose.koinViewModel

/**
 * Pantalla de Gestión y Cola de Reportes de Moderación (HU - Moderación y Seguridad).
 *
 * Permite filtrar y ordenar las denuncias activas, desplegando prioritariamente las denuncias urgentes ('Prioridad Alta').
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModerationReportsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ModerationReportsViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = ModerationSand,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Cola de Moderación",
                        color = ModerationForest,
                        fontWeight = FontWeight.ExtraBold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = ModerationForest,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::loadReports) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Recargar reportes",
                            tint = ModerationForest,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ModerationSand),
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
                is ModerationReportsUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = ModerationForest,
                    )
                }

                is ModerationReportsUiState.AccessDenied -> {
                    AccessDeniedModerationView(
                        onBack = onBack,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }

                is ModerationReportsUiState.Error -> {
                    ErrorModerationView(
                        onRetry = viewModel::loadReports,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }

                is ModerationReportsUiState.Success -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        item {
                            QueueSummary(state.reports.size, state.reports.count { it.reportPriority == ReportPriority.ALTA })
                        }
                        item {
                            FilterAndSortBar(
                                sortOrder = state.sortOrder,
                                selectedPriority = state.selectedPriority,
                                selectedStatus = state.selectedStatus,
                                selectedTargetType = state.selectedTargetType,
                                onSortOrderChanged = viewModel::updateSortOrder,
                                onPriorityFilterChanged = viewModel::updatePriorityFilter,
                                onStatusFilterChanged = viewModel::updateStatusFilter,
                                onTargetTypeFilterChanged = viewModel::updateTargetTypeFilter,
                            )
                        }
                        if (state.reports.isEmpty()) {
                            item { EmptyReportsView(modifier = Modifier.fillMaxWidth()) }
                        } else {
                            items(state.reports, key = { it.id }) { report ->
                                ModerationReportCard(report = report)
                            }
                        }
                        item { Spacer(modifier = Modifier.height(8.dp)) }
                    }
                }
            }
        }
    }
}

/** Barra de filtros y criterio de ordenamiento ('Prioridad Alta' primero). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterAndSortBar(
    sortOrder: ReportSortOrder,
    selectedPriority: ReportPriority?,
    selectedStatus: ReportStatus?,
    selectedTargetType: String?,
    onSortOrderChanged: (ReportSortOrder) -> Unit,
    onPriorityFilterChanged: (ReportPriority?) -> Unit,
    onStatusFilterChanged: (ReportStatus?) -> Unit,
    onTargetTypeFilterChanged: (String?) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "ORDENAR",
            color = ModerationForest,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ReportSortOrder.entries.forEach { order ->
                FilterChip(
                    selected = sortOrder == order,
                    onClick = { onSortOrderChanged(order) },
                    label = { Text(order.label, fontWeight = FontWeight.Bold) },
                    colors =
                        FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ModerationForest,
                            selectedLabelColor = ModerationCream,
                            containerColor = ModerationCream,
                            labelColor = ModerationForest,
                        ),
                    border = FilterChipDefaults.filterChipBorder(borderColor = ModerationLeaf, enabled = true, selected = sortOrder == order),
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "PRIORIDAD",
            color = ModerationForest,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = selectedPriority == null,
                onClick = { onPriorityFilterChanged(null) },
                label = { Text("Todas") },
                colors =
                    FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ModerationLeaf,
                        selectedLabelColor = ModerationCream,
                    ),
            )
            ReportPriority.entries.forEach { priority ->
                FilterChip(
                    selected = selectedPriority == priority,
                    onClick = { onPriorityFilterChanged(priority) },
                    label = { Text(priority.label) },
                    colors =
                        FilterChipDefaults.filterChipColors(
                            selectedContainerColor =
                                if (priority == ReportPriority.ALTA) ModerationCoral else ModerationLeaf,
                            selectedLabelColor = ModerationCream,
                        ),
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text("ESTADO", color = ModerationForest, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = selectedStatus == null,
                onClick = { onStatusFilterChanged(null) },
                label = { Text("Todos") },
                colors = moderationChipColors(),
            )
            ReportStatus.entries.forEach { status ->
                FilterChip(
                    selected = selectedStatus == status,
                    onClick = { onStatusFilterChanged(status) },
                    label = { Text(status.label) },
                    colors = moderationChipColors(),
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text("TIPO DE CONTENIDO", color = ModerationForest, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = selectedTargetType == null,
                onClick = { onTargetTypeFilterChanged(null) },
                label = { Text("Todos") },
                colors = moderationChipColors(),
            )
            listOf("Misión", "Usuario", "Comentario").forEach { type ->
                FilterChip(
                    selected = selectedTargetType == type,
                    onClick = { onTargetTypeFilterChanged(type) },
                    label = { Text(type) },
                    colors = moderationChipColors(),
                )
            }
        }
    }
}

@Composable
private fun moderationChipColors() = FilterChipDefaults.filterChipColors(
    selectedContainerColor = ModerationForest,
    selectedLabelColor = ModerationCream,
    containerColor = ModerationCream,
    labelColor = ModerationForest,
)

@Composable
private fun QueueSummary(total: Int, urgent: Int) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = ModerationForest,
        shape = RoundedCornerShape(20.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text("CENTRO DE SEGURIDAD", color = ModerationLightLeaf, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Spacer(Modifier.height(4.dp))
                Text("Cola de revisión", color = ModerationCream, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                Text("Casos priorizados para tu equipo", color = ModerationCream.copy(alpha = 0.8f), style = MaterialTheme.typography.bodySmall)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(total.toString(), color = ModerationCream, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
                Text("casos visibles", color = ModerationCream.copy(alpha = 0.85f), style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.height(4.dp))
                Surface(color = ModerationCoral, shape = RoundedCornerShape(20.dp)) {
                    Text("$urgent urgentes", modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp), color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/** Tarjeta que muestra los detalles de un reporte de moderación. */
@Composable
private fun ModerationReportCard(
    report: ModerationReport,
    modifier: Modifier = Modifier,
) {
    val priorityColor =
        when (report.reportPriority) {
            ReportPriority.ALTA -> ModerationCoral
            ReportPriority.MEDIA -> ModerationOrange
            ReportPriority.BAJA -> ModerationLeaf
        }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ModerationCream),
        border = BorderStroke(2.dp, priorityColor),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    color = ModerationLightLeaf,
                    shape = CircleShape,
                ) {
                    Text(
                        text = report.targetType.uppercase(),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        color = ModerationForest,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }

                Surface(
                    color = priorityColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, priorityColor),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = priorityColor,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = report.priority.uppercase(),
                            color = priorityColor,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 11.sp,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = report.reason,
                color = ModerationForest,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
            )

            report.details?.takeIf { it.isNotBlank() }?.let { detailsText ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = detailsText,
                    color = ModerationBrown,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
            Column {
                Text(
                    text = "Reportado por ${report.reporterProfile?.fullName ?: "Usuario"}",
                    color = ModerationBrown,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                report.createdAt?.let { createdAt ->
                    Text(
                        text = "Recibido · ${createdAt.take(16).replace('T', ' ')}",
                        color = ModerationBrown.copy(alpha = 0.75f),
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }

                Surface(
                    color = ModerationLightLeaf,
                    shape = RoundedCornerShape(6.dp),
                ) {
                    Text(
                        text = report.status,
                        color = ModerationForest,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                }
            }
        }
    }
}

/** Vista cuando no hay reportes que coincidan. */
@Composable
private fun EmptyReportsView(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(top = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Default.Refresh,
            contentDescription = null,
            tint = ModerationLeaf,
            modifier = Modifier.size(48.dp),
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "No hay reportes pendientes en este criterio.",
            color = ModerationForest,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
    }
}

/** Vista de Acceso Denegado. */
@Composable
private fun AccessDeniedModerationView(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth().padding(16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = ModerationCream),
        border = BorderStroke(2.dp, ModerationCoral),
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = ModerationCoral,
                modifier = Modifier.size(56.dp),
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Acceso Restringido",
                color = ModerationCoral,
                fontWeight = FontWeight.ExtraBold,
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Esta pantalla de moderación es exclusiva para personal autorizado con rol de Moderador.",
                color = ModerationBrown,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ModerationForest),
            ) {
                Text("Volver", fontWeight = FontWeight.Bold)
            }
        }
    }
}

/** Vista de Error. */
@Composable
private fun ErrorModerationView(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "No se pudieron cargar los reportes de moderación.",
            color = ModerationCoral,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = ModerationForest),
        ) {
            Text("Reintentar")
        }
    }
}

private val ModerationForest = Color(0xFF214E34)
private val ModerationLeaf = Color(0xFF64A95C)
private val ModerationLightLeaf = Color(0xFFDCEFD3)
private val ModerationSand = Color(0xFFF7F3E8)
private val ModerationCream = Color(0xFFFFFCF3)
private val ModerationBrown = Color(0xFF594A3C)
private val ModerationCoral = Color(0xFFE85D3F)
private val ModerationOrange = Color(0xFFE58A3A)
