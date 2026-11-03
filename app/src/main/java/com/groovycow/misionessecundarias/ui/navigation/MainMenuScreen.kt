package com.groovycow.misionessecundarias.ui.navigation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

enum class MainMenuDestination(
    val title: String,
    val description: String,
) {
    PROFILE("Mi perfil", "Consulta y administra tu información personal"),
    AVATAR("Personalizar avatar", "Configura la apariencia de tu avatar"),
    HISTORY("Historial", "Consulta tus misiones anteriores"),
    COLLECTIONS("Mochila", "Revisa tus elementos y reconocimientos"),
    SETTINGS("Ajustes", "Configura las preferencias de la aplicación"),
    SEARCH_MISSIONS("Buscar misiones", "Encuentra oportunidades de colaboración"),
    PUBLISH_MISSION("Publicar misión", "Solicita colaboración para una actividad"),
    MY_APPLICATIONS("Estado de mis postulaciones", "Consulta tus solicitudes y su estado"),
}

@Composable
fun MainMenuScreen(
    onNavigate: (MainMenuDestination) -> Unit,
    username: String? = null,
) {
    Scaffold(containerColor = SandBackground) { paddingValues ->
        Row(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(12.dp),
        ) {
            NatureSideRail(onNavigate = onNavigate)
            Spacer(modifier = Modifier.width(14.dp))
            MainMenuContent(
                username = username,
                onNavigate = onNavigate,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun NatureSideRail(onNavigate: (MainMenuDestination) -> Unit) {
    Column(
        modifier =
            Modifier
                .width(72.dp)
                .fillMaxHeight()
                .background(MintPanel, RoundedCornerShape(28.dp))
                .border(2.dp, RiverTeal, RoundedCornerShape(28.dp))
                .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        SideRailButton(
            icon = Icons.Default.Person,
            contentDescription = "Mi perfil",
            onClick = { onNavigate(MainMenuDestination.PROFILE) },
        )
        SideRailButton(
            icon = Icons.Default.Search,
            contentDescription = "Historial",
            onClick = { onNavigate(MainMenuDestination.HISTORY) },
        )
        SideRailButton(
            icon = Icons.Default.ShoppingCart,
            contentDescription = "Mochila",
            onClick = { onNavigate(MainMenuDestination.COLLECTIONS) },
        )
        SideRailButton(
            icon = Icons.Default.Settings,
            contentDescription = "Ajustes",
            onClick = { onNavigate(MainMenuDestination.SETTINGS) },
        )
    }
}

@Composable
private fun SideRailButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier =
            Modifier
                .size(52.dp)
                .background(CreamSurface, CircleShape)
                .border(2.dp, LeafGreen, CircleShape),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = ForestGreen,
            modifier = Modifier.size(29.dp),
        )
    }
}

@Composable
private fun MainMenuContent(
    username: String?,
    onNavigate: (MainMenuDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Menú\nprincipal",
                style = MaterialTheme.typography.headlineMedium,
                color = ForestGreen,
                fontWeight = FontWeight.ExtraBold,
            )
            NotificationButton()
        }

        Text(
            text = "Hola, ${username?.takeIf { it.isNotBlank() } ?: "aventurero"}!",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = BarkBrown,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
        )

        Spacer(modifier = Modifier.height(12.dp))
        GameAvatar(
            onClick = { onNavigate(MainMenuDestination.AVATAR) },
        )
        Spacer(modifier = Modifier.height(20.dp))

        GameMenuButton(
            text = "Ver misiones",
            backgroundColor = LightLeaf,
            borderColor = LeafGreen,
            onClick = { onNavigate(MainMenuDestination.SEARCH_MISSIONS) },
        )
        Spacer(modifier = Modifier.height(14.dp))
        GameMenuButton(
            text = "Publicar una misión",
            backgroundColor = LightEarth,
            borderColor = EarthOrange,
            onClick = { onNavigate(MainMenuDestination.PUBLISH_MISSION) },
        )
        Spacer(modifier = Modifier.height(14.dp))
        GameMenuButton(
            text = "Estado de las misiones",
            backgroundColor = LightRiver,
            borderColor = RiverTeal,
            onClick = { onNavigate(MainMenuDestination.MY_APPLICATIONS) },
        )
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun NotificationButton() {
    Box(contentAlignment = Alignment.TopEnd) {
        IconButton(
            onClick = { },
            modifier =
                Modifier
                    .size(52.dp)
                    .background(CreamSurface, CircleShape),
        ) {
            Icon(
                Icons.Default.Notifications,
                contentDescription = "Notificaciones",
                tint = ForestGreen,
                modifier = Modifier.size(31.dp),
            )
        }
        Box(
            modifier =
                Modifier
                    .size(13.dp)
                    .background(AlertCoral, CircleShape)
                    .border(2.dp, CreamSurface, CircleShape),
        )
    }
}

@Composable
private fun GameAvatar(onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier =
                Modifier
                    .size(148.dp)
                    .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val radius = size.minDimension / 2
                drawCircle(color = SunYellow, radius = radius)
                drawCircle(color = RiverTeal, radius = radius * 0.82f)
                drawCircle(color = CreamSurface, radius = radius * 0.67f)
                drawCircle(
                    color = LightLeaf,
                    radius = radius * 0.16f,
                    center = center.copy(x = center.x * 0.35f, y = center.y * 0.43f),
                )
                drawCircle(
                    color = EarthOrange,
                    radius = radius * 0.12f,
                    center = center.copy(x = center.x * 1.75f, y = center.y * 1.55f),
                )
            }
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Personalizar avatar",
                tint = ForestGreen,
                modifier = Modifier.size(82.dp),
            )
            Text(
                text = "1",
                color = CreamSurface,
                fontWeight = FontWeight.ExtraBold,
                modifier =
                    Modifier
                        .align(Alignment.BottomEnd)
                        .background(EarthOrange, CircleShape)
                        .border(3.dp, CreamSurface, CircleShape)
                        .padding(horizontal = 11.dp, vertical = 6.dp),
            )
        }
        Text(
            text = "Aventurero · Nivel 1",
            color = ForestGreen,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "Toca tu avatar para personalizarlo",
            style = MaterialTheme.typography.bodySmall,
            color = BarkBrown,
        )
    }
}

@Composable
private fun GameMenuButton(
    text: String,
    backgroundColor: Color,
    borderColor: Color,
    onClick: () -> Unit,
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(72.dp)
                .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(3.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = ForestGreen,
                textAlign = TextAlign.Center,
            )
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
private val SunYellow = Color(0xFFF2C94C)
private val SandBackground = Color(0xFFF7F3E8)
private val CreamSurface = Color(0xFFFFFCF3)
private val MintPanel = Color(0xFFCBEBDD)
private val BarkBrown = Color(0xFF594A3C)
private val AlertCoral = Color(0xFFE85D3F)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeaturePlaceholderScreen(
    title: String,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
        )
    }
}
