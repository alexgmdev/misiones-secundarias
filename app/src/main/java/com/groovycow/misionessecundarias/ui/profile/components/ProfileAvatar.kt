package com.groovycow.misionessecundarias.ui.profile.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.groovycow.misionessecundarias.domain.model.AvatarId
import com.groovycow.misionessecundarias.ui.profile.ProfileMessages
import com.groovycow.misionessecundarias.ui.theme.NaturePalette

/** Estilo visual de un avatar: color del aro, color de acento de los adornos y el ícono central. */
private data class AvatarStyle(
    val ring: Color,
    val accent: Color,
    val icon: ImageVector,
)

private fun AvatarId.style(): AvatarStyle =
    when (this) {
        AvatarId.BOSQUE -> AvatarStyle(NaturePalette.LeafGreen, NaturePalette.LightLeaf, Icons.Default.Face)
        AvatarId.RIO -> AvatarStyle(NaturePalette.RiverTeal, NaturePalette.LightRiver, Icons.Default.Person)
        AvatarId.SOL -> AvatarStyle(NaturePalette.SunYellow, NaturePalette.LightEarth, Icons.Default.Home)
        AvatarId.TIERRA -> AvatarStyle(NaturePalette.EarthOrange, NaturePalette.LightEarth, Icons.Default.Place)
        AvatarId.ESTRELLA -> AvatarStyle(NaturePalette.ForestGreen, NaturePalette.SunYellow, Icons.Default.Star)
        AvatarId.FLOR -> AvatarStyle(NaturePalette.AlertCoral, NaturePalette.LightCoral, Icons.Default.Settings)
        AvatarId.CORAZON -> AvatarStyle(NaturePalette.BarkBrown, NaturePalette.LightCoral, Icons.Default.Favorite)
        AvatarId.BRUJULA -> AvatarStyle(NaturePalette.MapLine, NaturePalette.MintPanel, Icons.Default.LocationOn)
    }

/**
 * Avatar predefinido dibujado con Canvas al estilo de `GameAvatar`: aro de color, disco crema,
 * dos adornos y un ícono Material central.
 *
 * @param avatar Avatar del catálogo.
 * @param modifier Modificador del contenedor.
 * @param size Diámetro del avatar.
 */
@Composable
fun ProfileAvatar(
    avatar: AvatarId,
    modifier: Modifier = Modifier,
    size: Dp = 96.dp,
) {
    val style = avatar.style()
    val label = ProfileMessages.avatarLabel(avatar)
    Box(
        modifier = modifier.size(size).semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = this.size.minDimension / 2
            drawCircle(color = style.ring, radius = radius)
            drawCircle(color = style.accent, radius = radius * 0.82f)
            drawCircle(color = NaturePalette.CreamSurface, radius = radius * 0.67f)
            drawCircle(
                color = style.accent,
                radius = radius * 0.16f,
                center = center.copy(x = center.x * 0.35f, y = center.y * 0.43f),
            )
            drawCircle(
                color = style.ring,
                radius = radius * 0.12f,
                center = center.copy(x = center.x * 1.75f, y = center.y * 1.55f),
            )
        }
        Icon(
            imageVector = style.icon,
            contentDescription = null,
            tint = NaturePalette.ForestGreen,
            modifier = Modifier.size(size * 0.5f),
        )
    }
}
