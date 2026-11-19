package com.groovycow.misionessecundarias.ui.missions

import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Formatea el timestamp almacenado en la zona horaria local de Cali. */
fun formatMissionDateTime(dateTime: String): String =
    runCatching {
        OffsetDateTime.parse(dateTime)
            .atZoneSameInstant(CALI_ZONE)
            .format(LOCAL_DATE_FORMAT)
    }.getOrDefault(dateTime)

private val CALI_ZONE: ZoneId = ZoneId.of("America/Bogota")
private val LOCAL_DATE_FORMAT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEEE d 'de' MMMM 'a las' HH:mm", Locale("es", "CO"))
