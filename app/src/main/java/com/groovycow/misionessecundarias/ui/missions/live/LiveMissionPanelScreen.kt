package com.groovycow.misionessecundarias.ui.missions.live

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.groovycow.misionessecundarias.domain.model.Mission
import com.groovycow.misionessecundarias.domain.model.MissionApplicant
import kotlinx.coroutines.delay
import org.koin.androidx.compose.koinViewModel

/** Intervalo con el que el panel consulta incorporaciones o retiros de participantes. */
private const val REFRESH_INTERVAL_MILLIS = 10_000L

/** Panel de ejecución en vivo con los avatares animados de los solucionadores (HU-051). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveMissionPanelScreen(
    missionId: String,
    missionTitle: String,
    onBack: () -> Unit,
    onMissionCompleted: (Mission) -> Unit = {},
    viewModel: LivePanelViewModel = koinViewModel(),
    checklistViewModel: ChecklistViewModel = koinViewModel(),
    pausesViewModel: PausesViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val checklistState by checklistViewModel.uiState.collectAsState()
    val pausesState by pausesViewModel.uiState.collectAsState()

    LaunchedEffect(missionId) {
        viewModel.load(missionId)
        checklistViewModel.load(missionId)
        pausesViewModel.load(missionId)
        while (true) {
            delay(REFRESH_INTERVAL_MILLIS)
            viewModel.refresh(missionId)
            checklistViewModel.refresh(missionId)
            pausesViewModel.load(missionId)
        }
    }

    Scaffold(
        containerColor = LiveSand,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Panel en vivo",
                        color = LiveForest,
                        fontWeight = FontWeight.ExtraBold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = LiveOrange,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = LiveSand),
            )
        },
    ) { paddingValues ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
        ) {
            Text(
                text = missionTitle,
                modifier = Modifier.fillMaxWidth(),
                color = LiveForest,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(12.dp))

            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        color = LiveTeal,
                    )
                }

                uiState.error != null -> LiveNotice(uiState.error!!, LiveCoral)

                uiState.participants.isEmpty() ->
                    LiveNotice(
                        "Todavía no hay solucionadores aceptados en esta misión.",
                        LiveOrange,
                    )

                else -> {
                    val count = uiState.participants.size
                    LiveNotice(
                        if (count == 1) "1 solucionador en la misión" else "$count solucionadores en la misión",
                        LiveLeaf,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    AvatarArena(
                        participants = uiState.participants,
                        modifier = Modifier.fillMaxWidth().height(ARENA_HEIGHT),
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            PausesSection(
                state = pausesState,
                onToggle = { kind -> pausesViewModel.toggle(missionId, kind) },
            )
            Spacer(modifier = Modifier.height(16.dp))
            ChecklistSection(
                state = checklistState,
                onAdd = { title, onAdded -> checklistViewModel.add(missionId, title, onAdded) },
                onToggle = checklistViewModel::toggle,
                onDelete = checklistViewModel::delete,
            )
            Spacer(modifier = Modifier.height(16.dp))
            CompleteMissionSection(
                missionId = missionId,
                progressPercent = checklistState.progressPercent,
                participants = uiState.participants,
                onMissionCompleted = onMissionCompleted,
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun LiveNotice(
    message: String,
    accentColor: Color,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(3.dp, accentColor),
        colors = CardDefaults.cardColors(containerColor = LiveCream),
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(16.dp),
            color = LiveBrown,
            fontWeight = FontWeight.Bold,
        )
    }
}

/** Escenario donde cada solucionador se desplaza de forma continua. */
@Composable
private fun AvatarArena(
    participants: List<MissionApplicant>,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier =
            modifier
                .background(LiveLightLeaf, RoundedCornerShape(20.dp))
                .border(3.dp, LiveForest, RoundedCornerShape(20.dp)),
    ) {
        val maxX = (maxWidth - AVATAR_SIZE).coerceAtLeast(0.dp)
        val maxY = (maxHeight - AVATAR_SIZE - LABEL_HEIGHT).coerceAtLeast(0.dp)
        participants.forEachIndexed { index, participant ->
            key(participant.id) {
                MovingAvatar(
                    name = participant.profile.fullName,
                    index = index,
                    maxX = maxX,
                    maxY = maxY,
                )
            }
        }
    }
}

@Composable
private fun MovingAvatar(
    name: String,
    index: Int,
    maxX: Dp,
    maxY: Dp,
) {
    val transition = rememberInfiniteTransition(label = "avatar-$index")
    val horizontal by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 3500 + (index % 5) * 700, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
                initialStartOffset = StartOffset((index * 450) % 3000),
            ),
        label = "x",
    )
    val vertical by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 2600 + (index % 4) * 600, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
                initialStartOffset = StartOffset((index * 700) % 2500),
            ),
        label = "y",
    )
    val color = AVATAR_COLORS[index % AVATAR_COLORS.size]

    Column(
        modifier = Modifier.offset(x = maxX * horizontal, y = maxY * vertical),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier =
                Modifier
                    .size(AVATAR_SIZE)
                    .background(color, CircleShape)
                    .border(3.dp, LiveForest, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = participantInitials(name),
                color = LiveCream,
                fontWeight = FontWeight.ExtraBold,
            )
        }
        Text(
            text = name.substringBefore(' '),
            color = LiveForest,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.size(width = AVATAR_SIZE + 16.dp, height = LABEL_HEIGHT),
            textAlign = TextAlign.Center,
        )
    }
}

private val ARENA_HEIGHT = 260.dp
private val AVATAR_SIZE = 52.dp
private val LABEL_HEIGHT = 20.dp
private val LiveForest = Color(0xFF214E34)
private val LiveLeaf = Color(0xFF64A95C)
private val LiveLightLeaf = Color(0xFFDCEFD3)
private val LiveTeal = Color(0xFF38A89D)
private val LiveOrange = Color(0xFFE58A3A)
private val LiveSand = Color(0xFFF7F3E8)
private val LiveCream = Color(0xFFFFFCF3)
private val LiveBrown = Color(0xFF594A3C)
private val LiveCoral = Color(0xFFE85D3F)
private val AVATAR_COLORS = listOf(LiveTeal, LiveOrange, LiveLeaf, LiveCoral, LiveBrown)
