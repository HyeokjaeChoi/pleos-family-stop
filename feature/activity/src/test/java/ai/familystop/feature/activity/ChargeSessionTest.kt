package ai.familystop.feature.activity

import ai.familystop.core.EnergyState
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChargeSessionTest {
    private val t0 = LocalDateTime.of(2026, 11, 9, 17, 0)

    @Test
    fun unknownChargeStateIsUnavailableEvenWithSoc() {
        assertEquals(ChargeView.UNAVAILABLE, chargeView(null, false))
        assertEquals(ChargeView.UNAVAILABLE, chargeView(EnergyState(socPercent = 62), true))
    }

    @Test
    fun chargingThenOffIsDoneButOffFromStartIsBefore() {
        val on = EnergyState(62, true, 310)
        val off = EnergyState(80, false, 400)
        assertEquals(ChargeView.CHARGING, chargeView(on, false))
        assertEquals(ChargeView.BEFORE, chargeView(off, false))
        assertEquals(ChargeView.DONE, chargeView(off, true))
    }

    @Test
    fun lateOnlyWhenArrivalIsAfterWish() {
        assertEquals(27L, lateMinutes(t0.plusMinutes(27), t0))
        assertNull(lateMinutes(t0, t0))
        assertNull(lateMinutes(t0.minusMinutes(3), t0))
    }
}
