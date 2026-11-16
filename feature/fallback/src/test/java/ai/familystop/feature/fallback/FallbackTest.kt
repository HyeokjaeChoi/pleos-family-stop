package ai.familystop.feature.fallback

import java.time.LocalDateTime
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Test

class FallbackTest {
    @Test
    fun neverReceivedIsUnknown() {
        assertEquals(ValueDescription.Unknown, describeValue(null, 0))
    }

    @Test
    fun lastKnownCarriesSourceAndTimeAndIsNotPresentedAsCurrent() {
        val at = 14 * 3_600_000L + 5 * 60_000L
        val k = LastKnown("72%", "차량", at)
        val time = LocalDateTime.of(1970, 1, 1, 14, 5)
        assertEquals(
            ValueDescription.Known("72%", "차량", time, otherDay = false),
            describeValue(k, at + 60_000L, ZoneOffset.UTC)
        )
        assertEquals(
            ValueDescription.Known("72%", "차량", time, otherDay = true),
            describeValue(k, at + 86_400_000L, ZoneOffset.UTC)
        )
    }
}
