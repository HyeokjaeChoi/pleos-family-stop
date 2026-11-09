package ai.familystop.feature.activity

import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChargeSessionTest {
    private val t0 = LocalDateTime.of(2026, 11, 9, 17, 0)
    private val clock = ChargeClock(t0, t0.plusMinutes(25))

    @Test
    fun stepperClampsToRange() {
        assertEquals(30, stepMinutes(25, 1))
        assertEquals(20, stepMinutes(25, -1))
        assertEquals(MIN_MINUTES, stepMinutes(MIN_MINUTES, -1))
        assertEquals(MAX_MINUTES, stepMinutes(MAX_MINUTES, 1))
    }

    @Test
    fun remainingAndProgressFollowTheClock() {
        assertEquals(25 * 60L, clock.remainingSeconds(t0))
        assertEquals(0f, clock.progress(t0), 0f)
        assertEquals(0.5f, clock.progress(t0.plusSeconds(750)), 0.001f)
        assertEquals(0L, clock.remainingSeconds(t0.plusHours(1)))
        assertEquals(1f, clock.progress(t0.plusHours(1)), 0f)
        assertTrue(clock.finished(t0.plusMinutes(25)))
        assertFalse(clock.finished(t0.plusMinutes(24)))
    }

    @Test
    fun extendMovesOnlyTheEnd() {
        val e = clock.extend(5)
        assertEquals(t0, e.startedAt)
        assertEquals(30 * 60L, e.remainingSeconds(t0))
    }

    @Test
    fun formatsMinutesAndHours() {
        assertEquals("24:12", formatRemaining(24 * 60 + 12))
        assertEquals("00:00", formatRemaining(-3))
        assertEquals("1:05:09", formatRemaining(3600 + 5 * 60 + 9))
    }

    @Test
    fun lateOnlyWhenArrivalIsAfterWish() {
        val wish = t0
        assertEquals(27L, lateMinutes(wish.plusMinutes(27), wish))
        assertNull(lateMinutes(wish, wish))
        assertNull(lateMinutes(wish.minusMinutes(3), wish))
    }
}
