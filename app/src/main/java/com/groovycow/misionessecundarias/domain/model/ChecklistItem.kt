package com.groovycow.misionessecundarias.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Tarea del checklist de ejecución que el publicador crea y marca como cumplida (HU-053). */
@Serializable
data class ChecklistItem(
    val id: String,
    @SerialName("mission_id") val missionId: String,
    val title: String,
    @SerialName("is_done") val isDone: Boolean = false,
)

/**
 * Porcentaje de progreso de la misión: tareas cumplidas sobre tareas totales, redondeado.
 *
 * Sin tareas el progreso es 0.
 */
fun checklistProgressPercent(items: List<ChecklistItem>): Int {
    if (items.isEmpty()) return 0
    val done = items.count { it.isDone }
    return Math.round(done * 100f / items.size)
}
