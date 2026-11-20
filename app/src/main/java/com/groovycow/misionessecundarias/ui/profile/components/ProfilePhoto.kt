package com.groovycow.misionessecundarias.ui.profile.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.groovycow.misionessecundarias.ui.theme.NaturePalette

/**
 * Foto de perfil circular cargada con Coil; mientras carga, si falla o si [photoUrl] es nulo
 * muestra un marcador con el ícono de persona.
 *
 * @param photoUrl URL pública de la foto (puede llevar `?v=` para invalidar la caché), o null.
 * @param modifier Modificador del contenedor.
 * @param size Diámetro de la foto.
 * @param contentDescription Texto accesible de la foto.
 */
@Composable
fun ProfilePhoto(
    photoUrl: String?,
    modifier: Modifier = Modifier,
    size: Dp = 96.dp,
    contentDescription: String? = "Foto de perfil",
) {
    Box(
        modifier = modifier.size(size).clip(CircleShape).background(NaturePalette.LightLeaf),
        contentAlignment = Alignment.Center,
    ) {
        if (photoUrl == null) {
            PhotoPlaceholder(size)
        } else {
            SubcomposeAsyncImage(
                model = photoUrl,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(size),
                loading = { PhotoPlaceholder(size) },
                error = { PhotoPlaceholder(size) },
            )
        }
    }
}

@Composable
private fun PhotoPlaceholder(size: Dp) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(size)) {
        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = null,
            tint = NaturePalette.ForestGreen,
            modifier = Modifier.size(size * 0.55f),
        )
    }
}
