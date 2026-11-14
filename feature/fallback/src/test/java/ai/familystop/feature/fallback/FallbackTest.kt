package ai.familystop.feature.fallback

import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FallbackTest {
    @Test
    fun neverReceivedIsUnknown() {
        assertEquals("미확인", describeValue(null, 0))
    }

    @Test
    fun lastKnownShowsSourceAndTimeAndIsNotPresentedAsCurrent() {
        val at = 14 * 3_600_000L + 5 * 60_000L
        val k = LastKnown("72%", "차량", at)
        assertEquals("72% · 마지막 확인값 · 차량 오후 2:05", describeValue(k, at + 60_000L, ZoneOffset.UTC))
        assertEquals(
            "72% · 마지막 확인값 · 차량 1월 1일 오후 2:05",
            describeValue(k, at + 86_400_000L, ZoneOffset.UTC)
        )
    }

    @Test
    fun everyCapabilityExplainsPurposeLimitAndAlternative() {
        Capability.entries.forEach {
            val n = PermissionNotice(it)
            assertEquals(3, n.lines.size)
            assertTrue(n.lines.all(String::isNotBlank))
        }
        assertTrue(PermissionNotice(Capability.MICROPHONE).title.startsWith("마이크"))
        assertTrue(PermissionNotice(Capability.LOCATION).title.startsWith("위치"))
    }
}
