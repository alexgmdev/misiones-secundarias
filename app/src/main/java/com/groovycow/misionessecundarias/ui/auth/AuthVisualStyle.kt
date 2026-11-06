package com.groovycow.misionessecundarias.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.groovycow.misionessecundarias.R

internal val GameCream = Color(0xFFFFF3D3)
internal val GameGold = Color(0xFFFFC857)
internal val ForestPanel = Color(0xFF173E36)
internal val ForestInk = Color(0xFF17342F)
internal val ErrorCream = Color(0xFFFFD5C8)
internal val StrongTextShadow = Shadow(Color.Black.copy(alpha = 0.92f), Offset(4f, 4f), 5f)
internal val SoftTextShadow = Shadow(Color.Black.copy(alpha = 0.88f), Offset(3f, 3f), 4f)
internal val ButtonTextShadow = Shadow(Color.White.copy(alpha = 0.35f), Offset(1f, 1f), 2f)

/** Fondo común de acceso, con la ilustración de Cali y un degradado de contraste. */
@Composable
internal fun AuthBackground(content: @Composable BoxScope.() -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(R.drawable.login_cali_background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors =
                                listOf(
                                    Color.Black.copy(alpha = 0.18f),
                                    Color.Black.copy(alpha = 0.08f),
                                    Color.Black.copy(alpha = 0.42f),
                                ),
                        ),
                    ),
        )
        content()
    }
}

/** Panel translúcido compartido por los formularios de autenticación. */
@Composable
internal fun AuthPanel(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier =
            modifier
                .fillMaxWidth()
                .border(2.dp, GameGold.copy(alpha = 0.9f), RoundedCornerShape(22.dp)),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = ForestPanel.copy(alpha = 0.88f)),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp),
            content = content,
        )
    }
}

/** Título claro y sombreado que permanece legible sobre la ilustración. */
@Composable
internal fun AuthTitle(text: String) {
    Text(
        text = text,
        color = GameCream,
        style = MaterialTheme.typography.headlineMedium.copy(shadow = StrongTextShadow),
        fontWeight = FontWeight.Bold,
    )
}

/** Colores comunes de los campos claros incluidos en el panel oscuro. */
@Composable
internal fun authTextFieldColors() =
    OutlinedTextFieldDefaults.colors(
        focusedTextColor = ForestInk,
        unfocusedTextColor = ForestInk,
        focusedContainerColor = GameCream.copy(alpha = 0.96f),
        unfocusedContainerColor = GameCream.copy(alpha = 0.92f),
        focusedBorderColor = GameGold,
        unfocusedBorderColor = GameCream,
        focusedLabelColor = ForestInk,
        unfocusedLabelColor = ForestInk,
        cursorColor = ForestInk,
        focusedTrailingIconColor = ForestInk,
        unfocusedTrailingIconColor = ForestInk,
        errorContainerColor = GameCream.copy(alpha = 0.96f),
    )
