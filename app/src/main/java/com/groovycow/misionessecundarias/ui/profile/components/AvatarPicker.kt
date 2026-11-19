package com.groovycow.misionessecundarias.ui.profile.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.groovycow.misionessecundarias.domain.model.AvatarId
import com.groovycow.misionessecundarias.ui.theme.NaturePalette

/**
 * Selector de avatar: cuadrícula con los avatares del catálogo; el elegido se resalta con un borde.
 *
 * @param selected Avatar elegido, o null.
 * @param onSelect Se invoca al tocar un avatar.
 * @param modifier Modificador del contenedor.
 * @param errorText Mensaje de error a mostrar bajo la cuadrícula, o null.
 */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun AvatarPicker(
    selected: AvatarId?,
    onSelect: (AvatarId) -> Unit,
    modifier: Modifier = Modifier,
    errorText: String? = null,
) {
    Column(modifier = modifier) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            AvatarId.entries.forEach { avatar ->
                val isSelected = avatar == selected
                ProfileAvatar(
                    avatar = avatar,
                    size = 64.dp,
                    modifier =
                        Modifier
                            .clip(CircleShape)
                            .border(
                                border = BorderStroke(if (isSelected) 3.dp else 0.dp, NaturePalette.ForestGreen),
                                shape = CircleShape,
                            )
                            .clickable { onSelect(avatar) },
                )
            }
        }
        if (errorText != null) {
            Text(
                text = errorText,
                color = NaturePalette.AlertCoral,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}
