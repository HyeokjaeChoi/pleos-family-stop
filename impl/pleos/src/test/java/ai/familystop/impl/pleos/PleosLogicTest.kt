package ai.familystop.impl.pleos

import ai.familystop.core.Destination
import ai.familystop.core.DestinationKind
import ai.familystop.core.StopPlace
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PleosLogicTest {
    private val a = StopPlace("a", 1.0, 1.0)
    private val b = StopPlace("b", 2.0, 2.0)
    private val c = StopPlace("c", 3.0, 3.0)

    @Test
    fun thirdWaypointRejectedAndReleasedSlotReused() {
        val s = WaypointSlots()
        assertEquals(0, s.reserve(a))
        assertEquals(1, s.reserve(b))
        assertNull(s.reserve(c))
        assertNull(s.reserve(a)) // duplicate
        s.release(s.indexOf(a)!!)
        assertEquals(0, s.reserve(c))
    }

    @Test
    fun socOutOfRangeIsUnknown() {
        assertEquals(55, socOrNull(55))
        assertNull(socOrNull(null))
        assertNull(socOrNull(-1))
        assertNull(socOrNull(101))
    }

    @Test
    fun rangeMetresBecomeKmAndBadValuesAreUnknown() {
        assertEquals(310, rangeKmOrNull(310_400f))
        assertEquals(0, rangeKmOrNull(0f))
        assertNull(rangeKmOrNull(null))
        assertNull(rangeKmOrNull(-5f))
        assertNull(rangeKmOrNull(Float.NaN))
    }

    @Test
    fun destinationsDropUnsetDuplicatesAndPutBookmarksFirst() {
        val unset = Destination("", 0.0, 0.0, DestinationKind.HOME)
        val r1 = Destination("A", 1.0, 2.0, DestinationKind.RECENT)
        val r1dup = Destination("A", 1.0, 2.0, DestinationKind.RECENT)
        val home = Destination("집", 3.0, 4.0, DestinationKind.HOME)
        val out = cleanDestinations(listOf(r1, unset, r1dup, home))
        assertEquals(listOf(home, r1), out)
    }

    @Test
    fun syncReplacesSlotsWithNaviState() {
        val s = WaypointSlots()
        s.reserve(a)
        s.sync(listOf(b, c, a))
        assertEquals(0, s.indexOf(b))
        assertEquals(1, s.indexOf(c))
        assertNull(s.indexOf(a))
        assertNull(s.reserve(a))
    }

    @Test
    fun naviEchoWithZeroCoordinatesStillMatchesByPoiId() {
        val s = WaypointSlots()
        s.sync(listOf(StopPlace("휴게소", 0.0, 0.0, "demo-c")))
        assertEquals(0, s.indexOf(StopPlace("휴게소", 37.25, 127.08, "demo-c")))
        assertNull(s.reserve(StopPlace("휴게소", 37.25, 127.08, "demo-c")))
    }
}
