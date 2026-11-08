package ai.familystop.feature.metrics

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MetricsTest {
    private val dir = java.nio.file.Files.createTempDirectory("metrics").toFile()
    private val store = FileMetricsStore(dir)
    private val collector = MetricsCollector(store)

    private fun metrics(id: String, origin: DataOrigin = DataOrigin.SDK) =
        TripMetrics(id, origin, "지인", 30, 10, listOf("가까워서", "아이 화장실"), listOf("수유실"), 25000)

    @Test
    fun noCollectionWithoutDriverConsent() {
        val id = collector.startTrip()
        assertFalse(collector.record(metrics(id)))
        assertFalse(collector.grantConsent(id, isDriver = false))
        assertFalse(collector.record(metrics(id)))
        assertNull(store.load(id))
    }

    @Test
    fun recordRoundTripKeepsOriginAndWithdrawDeletes() {
        val id = collector.startTrip()
        assertTrue(collector.grantConsent(id, isDriver = true))
        assertTrue(collector.record(metrics(id, DataOrigin.VIRTUAL)))
        assertEquals(metrics(id, DataOrigin.VIRTUAL), store.load(id))
        assertTrue(collector.withdraw(id))
        assertNull(store.load(id))
        assertFalse(collector.record(metrics(id)))
        assertEquals(0, dir.listFiles()!!.size)
    }

    @Test
    fun storeFailureNeverThrows() {
        val broken = MetricsCollector(
            object : MetricsStore {
                override fun save(metrics: TripMetrics) = error("disk full")

                override fun deleteTrip(tripId: String) = error("io")

                override fun load(tripId: String): TripMetrics? = null

                override fun listTrips() = emptyList<String>()
            }
        )
        val id = broken.startTrip()
        broken.grantConsent(id, true)
        assertFalse(broken.record(metrics(id)))
        assertFalse(broken.withdraw(id))
    }

    @Test
    fun tripIdIsRandomAndPathSafe() {
        assertTrue(collector.startTrip() != collector.startTrip())
        val bad = runCatching { store.load("../x") }
        assertTrue(bad.isFailure)
    }

    @Test
    fun listsRoundTripNewlinesAndEmptyItemsAndWithdrawRemovesTmp() {
        val id = collector.startTrip()
        collector.grantConsent(id, true)
        val m = metrics(
            id
        ).copy(choiceReasons = listOf("a\nb", "", "c"), unresolvedNeeds = emptyList())
        assertTrue(collector.record(m))
        assertEquals(m, store.load(id))
        File(dir, "$id.tmp").writeText("partial")
        assertTrue(collector.withdraw(id))
        assertTrue(collector.storedTrips().isEmpty())
    }

    @Test
    fun corruptFileLoadsAsNullAndOrphansAreListed() {
        File(dir, "abc.properties").writeText("origin=NOPE")
        assertNull(store.load("abc"))
        assertEquals(listOf("abc"), collector.storedTrips())
        assertTrue(collector.withdraw("abc"))
        assertTrue(collector.storedTrips().isEmpty())
    }
}
