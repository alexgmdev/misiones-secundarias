package com.groovycow.misionessecundarias.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Prioridad / Severidad de una denuncia de moderación. */
enum class ReportPriority(val dbValue: String, val label: String) {
    ALTA("Alta", "Prioridad Alta"),
    MEDIA("Media", "Prioridad Media"),
    BAJA("Baja", "Prioridad Baja"),
    ;

    companion object {
        fun fromDbValue(value: String?): ReportPriority = entries.firstOrNull { it.dbValue.equals(value, ignoreCase = true) } ?: MEDIA
    }
}

/** Estado de gestión de un reporte. */
enum class ReportStatus(val dbValue: String, val label: String) {
    PENDIENTE("Pendiente", "Pendiente"),
    EN_REVISION("En revisión", "En revisión"),
    RESUELTO("Resuelto", "Resuelto"),
    DESESTIMADO("Desestimado", "Desestimado"),
    ;

    companion object {
        fun fromDbValue(value: String?): ReportStatus = entries.firstOrNull { it.dbValue.equals(value, ignoreCase = true) } ?: PENDIENTE
    }
}

/** Criterio de ordenamiento para la cola de moderación. */
enum class ReportSortOrder(val label: String) {
    PRIORITY_HIGH_FIRST("Prioridad Alta"),
    DATE_NEWEST_FIRST("Más recientes"),
    DATE_OLDEST_FIRST("Más antiguos"),
}

/**
 * Reporte de moderación / denuncia enviada por un usuario (HU - Moderación y Seguridad).
 */
@Serializable
data class ModerationReport(
    val id: String,
    @SerialName("reporter_id") val reporterId: String,
    @SerialName("target_type") val targetType: String,
    @SerialName("target_id") val targetId: String,
    val reason: String,
    val details: String? = null,
    val priority: String = "Media",
    val status: String = "Pendiente",
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("reporter_profile") val reporterProfile: ReporterProfile? = null,
) {
    val reportPriority: ReportPriority
        get() = ReportPriority.fromDbValue(priority)

    val reportStatus: ReportStatus
        get() = ReportStatus.fromDbValue(status)
}

@Serializable
data class ReporterProfile(
    @SerialName("full_name") val fullName: String,
)
