package com.groovycow.misionessecundarias.domain.validation

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Construcción y serialización del inicio de una misión en la zona horaria de Cali. */
object MissionDateTime {
    /**
     * Combina fecha y hora en un timestamp ISO-8601 con el offset de `America/Bogota`.
     *
     * @param date Fecha local de inicio.
     * @param time Hora local de inicio.
     * @return Timestamp con offset, listo para `date_time` (`TIMESTAMPTZ`).
     */
    fun toIsoOffset(
        date: LocalDate,
        time: LocalTime,
    ): String = date.atTime(time).atZone(BOGOTA).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)

    private val BOGOTA: ZoneId = ZoneId.of("America/Bogota")
}
