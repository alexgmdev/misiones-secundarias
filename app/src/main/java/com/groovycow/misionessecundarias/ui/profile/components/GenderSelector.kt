package com.groovycow.misionessecundarias.ui.profile.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.groovycow.misionessecundarias.domain.model.Gender
import com.groovycow.misionessecundarias.ui.profile.ProfileMessages
import com.groovycow.misionessecundarias.ui.theme.NaturePalette

/**
 * Selector de género con exactamente tres opciones: Femenino, Masculino y Prefiero no decir.
 *
 * @param selected Género elegido, o null.
 * @param onSelect Se invoca al elegir una opción.
 * @param modifier Modificador del contenedor.
 * @param errorText Mensaje de error a mostrar bajo las opciones, o null.
 */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun GenderSelector(
    selected: Gender?,
    onSelect: (Gender) -> Unit,
    modifier: Modifier = Modifier,
    errorText: String? = null,
) {
    Column(modifier = modifier) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Gender.entries.forEach { gender ->
                FilterChip(
                    selected = gender == selected,
                    onClick = { onSelect(gender) },
                    label = { Text(ProfileMessages.genderLabel(gender)) },
                    colors =
                        FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NaturePalette.LightLeaf,
                            selectedLabelColor = NaturePalette.ForestGreen,
                        ),
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
