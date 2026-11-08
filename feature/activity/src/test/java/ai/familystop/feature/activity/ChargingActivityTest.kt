package ai.familystop.feature.activity

import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ChargingActivityTest {
    private val now = LocalDateTime.of(2026, 11, 8, 10, 0)
    private val c =
        ActivityCandidate("휴게소 식당", ActivityKind.MEAL, roundTripMin = 10, activityMin = 25)

    // 가용 = 60 - 10 - 25 - 5(여유) = 20
    @Test
    fun slackIsEndMinusTripMinusActivityMinusBuffer() {
        val end = now.plusMinutes(60)
        assertEquals(Fit.Fits(20, true), assess(c, ChargeInfo.Auto(end), now))
        assertEquals(Fit.Fits(0, true), assess(c, ChargeInfo.Auto(now.plusMinutes(40)), now))
        assertEquals(Fit.TooShort(1, true), assess(c, ChargeInfo.Auto(now.plusMinutes(39)), now))
    }

    @Test
    fun manualIsUnconfirmedAndUnknownNeverClaimsFit() {
        assertEquals(Fit.Fits(20, false), assess(c, ChargeInfo.Manual(now.plusMinutes(60)), now))
        assertEquals(Fit.Unknown, assess(c, ChargeInfo.Unknown, now))
        assertFalse(Fit.Unknown.message().contains("여유"))
        assertEquals("여유 20분 · 직접 입력 기준, 보장 안 돼요", Fit.Fits(20, false).message())
    }

    @Test
    fun autoWinsAndRecalculatesWhenTimeAdvances() {
        val auto = now.plusMinutes(60)
        assertEquals(ChargeInfo.Auto(auto), resolveCharge(auto, now.plusMinutes(5)))
        assertEquals(ChargeInfo.Manual(auto), resolveCharge(null, auto))
        assertEquals(ChargeInfo.Unknown, resolveCharge(null, null))
        assertEquals(Fit.Fits(10, true), assess(c, ChargeInfo.Auto(auto), now.plusMinutes(10)))
    }

    @Test
    fun manualEndIsFixedSoSlackShrinksAndPassedIsFlagged() {
        val end = manualEnd("60", now)!!
        assertEquals(Fit.Fits(10, false), assess(c, ChargeInfo.Manual(end), now.plusMinutes(10)))
        assertEquals(Fit.Passed, assess(c, ChargeInfo.Manual(end), end))
        assertEquals(null, manualEnd("", now))
    }
}
