package ai.familystop.feature.input

import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TripInputTest {
    private val now = LocalDateTime.of(2026, 11, 8, 10, 0)
    private val ok = TripDraft(destination = "강릉", timeText = "13:30", needs = setOf(StopNeed.MEAL))

    private fun errors(draft: TripDraft, auto: Int? = null) =
        (validate(draft, auto, now) as? ValidationResult.Invalid)?.errors

    @Test
    fun validDraftPasses() {
        val r = validate(ok, null, now) as ValidationResult.Ok
        assertEquals(LocalDateTime.of(2026, 11, 8, 13, 30), r.input.arrival)
        assertEquals(Soc.Unknown, r.input.soc)
    }

    @Test
    fun missingDestinationAndPastTimeAreRejected() {
        assertEquals(listOf(InputError.DESTINATION_MISSING), errors(ok.copy(destination = " ")))
        assertEquals(listOf(InputError.TIME_PAST), errors(ok.copy(timeText = "09:59")))
        assertEquals(listOf(InputError.TIME_INVALID), errors(ok.copy(timeText = "25:00")))
    }

    @Test
    fun autoSocWinsOverManualAndManualIsLabelled() {
        assertEquals(Soc.Auto(40), resolveSoc(40, 80))
        assertEquals(Soc.Manual(80), resolveSoc(null, 80))
        assertEquals(Soc.Unknown, resolveSoc(null, null))
        assertTrue(socMismatch(40, 80))
        assertFalse(socMismatch(40, 45))
        assertFalse(socMismatch(null, 80))
    }

    @Test
    fun manualSocOutOfRangeRejectedOnlyWhenUsed() {
        assertEquals(listOf(InputError.SOC_RANGE), errors(ok.copy(manualSocText = "120")))
        assertEquals(null, errors(ok.copy(manualSocText = "120"), auto = 50))
    }
}
