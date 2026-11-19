package com.groovycow.misionessecundarias.domain.validation

import com.groovycow.misionessecundarias.domain.model.CreateMissionDraft
import com.groovycow.misionessecundarias.domain.model.MissionError
import com.groovycow.misionessecundarias.domain.model.MissionField
import com.groovycow.misionessecundarias.domain.model.MissionModality
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class MissionDraftValidatorTest {
    private lateinit var validator: MissionDraftValidator

    @Before
    fun setUp() {
        validator = MissionDraftValidator(Clock.fixed(NOW, BOGOTA))
    }

    @Test
    fun shouldMarkRequiredTextFieldsWhenTheyAreBlank() {
        val errors = validator.validate(validDraft.copy(title = " ", description = "", requirements = "\n")).fieldErrors

        assertEquals(MissionError.Required, errors[MissionField.TITLE])
        assertEquals(MissionError.Required, errors[MissionField.DESCRIPTION])
        assertEquals(MissionError.Required, errors[MissionField.REQUIREMENTS])
    }

    @Test
    fun shouldRequireMeetingPointAndCoordinatesForFieldMission() {
        val errors =
            validator.validate(
                validDraft.copy(meetingPoint = "", latitude = null, longitude = null),
            ).fieldErrors

        assertEquals(MissionError.Required, errors[MissionField.MEETING_POINT])
        assertEquals(MissionError.Required, errors[MissionField.LATITUDE])
        assertEquals(MissionError.Required, errors[MissionField.LONGITUDE])
    }

    @Test
    fun shouldRejectMeetingPointOutsideCali() {
        val errors = validator.validate(validDraft.copy(meetingPoint = "Parque Principal, Bogotá")).fieldErrors

        assertEquals(MissionError.LocationMustBeInCali, errors[MissionField.MEETING_POINT])
    }

    @Test
    fun shouldRejectCoordinatesOutsideCaliBounds() {
        val errors = validator.validate(validDraft.copy(latitude = 4.61, longitude = -74.08)).fieldErrors

        assertEquals(MissionError.LocationMustBeInCali, errors[MissionField.LATITUDE])
        assertEquals(MissionError.LocationMustBeInCali, errors[MissionField.LONGITUDE])
    }

    @Test
    fun shouldRequireAtLeastTwoParticipants() {
        val errors = validator.validate(validDraft.copy(maxParticipants = 1)).fieldErrors

        assertEquals(MissionError.MinimumParticipants, errors[MissionField.MAX_PARTICIPANTS])
    }

    @Test
    fun shouldRequireDateAndTime() {
        val errors = validator.validate(validDraft.copy(date = null, time = null)).fieldErrors

        assertEquals(MissionError.DateRequired, errors[MissionField.DATE])
        assertEquals(MissionError.TimeRequired, errors[MissionField.TIME])
    }

    @Test
    fun shouldRejectDateBeforeToday() {
        val errors = validator.validate(validDraft.copy(date = LocalDate.of(2026, 11, 4))).fieldErrors

        assertEquals(MissionError.DateInPast, errors[MissionField.DATE])
    }

    @Test
    fun shouldAcceptTodayAndFutureDate() {
        val today = validator.validate(validDraft.copy(date = LocalDate.of(2026, 11, 5)))
        val future = validator.validate(validDraft.copy(date = LocalDate.of(2026, 11, 15)))

        assertTrue(today.isValid)
        assertTrue(future.isValid)
    }

    @Test
    fun shouldNotRequirePhysicalLocationForRemoteMission() {
        val result =
            validator.validate(
                validDraft.copy(
                    modality = MissionModality.REMOTA,
                    meetingPoint = "",
                    latitude = null,
                    longitude = null,
                ),
            )

        assertTrue(result.isValid)
    }

    private companion object {
        val BOGOTA: ZoneId = ZoneId.of("America/Bogota")
        val NOW: Instant = Instant.parse("2026-11-05T17:00:00Z")
        val validDraft =
            CreateMissionDraft(
                title = "Siembra en Parque Dog",
                description = "Jornada de siembra comunitaria",
                requirements = "Guantes",
                category = null,
                modality = MissionModality.CAMPO,
                date = LocalDate.of(2026, 11, 15),
                time = LocalTime.of(10, 0),
                meetingPoint = "Parque Dog Cali",
                latitude = 3.45,
                longitude = -76.53,
                maxParticipants = 2,
            )
    }
}
