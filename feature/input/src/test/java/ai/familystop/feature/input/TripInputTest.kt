package ai.familystop.feature.input

import ai.familystop.core.Destination
import ai.familystop.core.DestinationKind
import ai.familystop.core.RouteEta
import ai.familystop.core.formatClock
import java.time.LocalDateTime
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TripInputTest {
    private val now = LocalDateTime.of(2026, 11, 9, 10, 0)
    private val dest = Destination("더현대서울", 37.52, 126.93, DestinationKind.RECENT)
    private val ok = TripDraft(destinationKey = destinationKey(dest), hour24 = 13, minute = 30)

    private fun result(d: TripDraft, auto: Int? = null) = validate(d, listOf(dest), auto, now)

    @Test
    fun validDraftPasses() {
        val r = result(ok) as ValidationResult.Ok
        assertEquals(LocalDateTime.of(2026, 11, 9, 13, 30), r.input.arrival)
        assertEquals(dest, r.input.destination)
        assertEquals(Soc.Unknown, r.input.soc)
    }

    @Test
    fun missingDestinationOrTimeIsReportedSeparately() {
        assertEquals(
            listOf(InputError.DESTINATION_MISSING),
            (result(ok.copy(destinationKey = null)) as ValidationResult.Invalid).errors
        )
        assertEquals(
            listOf(InputError.TIME_MISSING),
            (result(ok.copy(hour24 = null)) as ValidationResult.Invalid).errors
        )
        // A key the list no longer holds counts as unset.
        assertEquals(
            listOf(InputError.DESTINATION_MISSING, InputError.TIME_MISSING),
            (result(TripDraft(destinationKey = "gone")) as ValidationResult.Invalid).errors
        )
    }

    @Test
    fun pastTimeMeansTomorrow() {
        assertEquals(LocalDateTime.of(2026, 11, 10, 9, 59), arrivalFrom(9, 59, now))
        // "now" itself counts as past
        assertEquals(LocalDateTime.of(2026, 11, 10, 10, 0), arrivalFrom(10, 0, now))
        assertEquals(LocalDateTime.of(2026, 11, 9, 10, 1), arrivalFrom(10, 1, now))
    }

    @Test
    fun etaDefaultIsNowPlusRemainTimeAndNullWithoutRoute() {
        assertEquals(LocalTime.of(11, 43), etaTime(RouteEta("x", 0.0, 0.0, 6180), now))
        assertNull(etaTime(null, now))
        assertNull(etaTime(RouteEta("x", 0.0, 0.0, 0), now))
    }

    @Test
    fun autoSocWinsOverManualAndManualIsLabelled() {
        assertEquals(Soc.Auto(40), resolveSoc(40, 80))
        assertEquals(Soc.Manual(80), resolveSoc(null, 80))
        assertEquals(Soc.Unknown, resolveSoc(null, null))
        assertTrue(socMismatch(40, 80))
        assertFalse(socMismatch(40, 45))
        assertFalse(socMismatch(null, 80))
        assertEquals(
            Soc.Manual(0),
            (result(ok.copy(manualSoc = 0)) as ValidationResult.Ok).input.soc
        )
    }

    @Test
    fun socStepperStartsAtFiftyAndClamps() {
        assertEquals(55, stepSoc(null, 1))
        assertEquals(0, stepSoc(2, -1))
        assertEquals(100, stepSoc(98, 1))
    }

    @Test
    fun currentDestinationIsAddedOrPromoted() {
        val other = Destination("다른곳", 1.0, 1.0, DestinationKind.RECENT)
        val eta = RouteEta("더현대서울", 37.52, 126.93, 100)
        // Already in the list: promoted to the front as CURRENT, no duplicate.
        assertEquals(
            listOf(dest.copy(kind = DestinationKind.CURRENT), other),
            mergeCurrent(listOf(other, dest), eta)
        )
        // Missing: added with the route's coordinates.
        val added = mergeCurrent(listOf(other), eta)
        assertEquals(DestinationKind.CURRENT, added[0].kind)
        assertEquals(listOf("더현대서울", "다른곳"), added.map { it.name })
        assertEquals(listOf(other), mergeCurrent(listOf(other), null))
    }

    @Test
    fun clockFormatUsesTwelveHourWithNoonAndMidnight() {
        assertEquals("오전 12:05", formatClock(0, 5))
        assertEquals("오후 12:00", formatClock(12, 0))
        assertEquals("오후 11:35", formatClock(23, 35))
        assertEquals("오전 9:07", formatClock(9, 7))
    }

    @Test
    fun summaryChipsShowUnsetValuesAndReturnToTheOwningStep() {
        val empty = summaryChips(TripDraft(), listOf(dest))
        assertEquals(listOf(false, false, false), empty.map { it.filled })
        assertEquals("목적지 · 선택 전", empty[0].label)

        val full = summaryChips(
            ok.copy(needs = setOf(StopNeed.RESTROOM, StopNeed.MEAL)),
            listOf(dest)
        )
        assertEquals(
            listOf("목적지 · 더현대서울", "도착 · 오후 1:30", "필요 · 식사·화장실"),
            full.map { it.label }
        )
        assertEquals(
            listOf(PlanStep.DESTINATION, PlanStep.CONDITIONS, PlanStep.CONDITIONS),
            full.map { it.step }
        )
    }
}
