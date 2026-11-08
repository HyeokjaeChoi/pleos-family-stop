package ai.familystop.feature.handoff

import ai.familystop.core.NaviGateway
import ai.familystop.core.StopPlace
import ai.familystop.fake.FakeNaviGateway
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HandoffTest {
    private val a = StopPlace("A", 1.0, 1.0)
    private val b = StopPlace("B", 2.0, 2.0)
    private val c = StopPlace("C", 3.0, 3.0)

    // 콜백을 직접 풀어 주는 게이트웨이: 응답 전/후, 실패를 제어한다.
    private class Manual : NaviGateway {
        val adds = mutableListOf<Pair<StopPlace, (Result<Unit>) -> Unit>>()
        val removes = mutableListOf<Pair<StopPlace, (Result<Unit>) -> Unit>>()
        override fun addWaypoint(place: StopPlace, onResult: (Result<Unit>) -> Unit) {
            adds += place to onResult
        }
        override fun removeWaypoint(place: StopPlace, onResult: (Result<Unit>) -> Unit) {
            removes += place to onResult
        }
    }

    private fun ctl(navi: NaviGateway, grace: Long = 60_000L) =
        HandoffController(CoroutineScope(Job() + Dispatchers.Unconfined), navi, grace)

    @Test
    fun successOnlyAfterResponseAndDuplicatePickIsMerged() {
        val navi = Manual()
        val h = ctl(navi)
        h.pick(a)
        h.pick(a)
        assertEquals(HandoffState.Sending(a), h.state.value)
        assertEquals(1, navi.adds.size)
        navi.adds[0].second(Result.success(Unit))
        assertEquals(HandoffState.Done(a, undoable = true), h.state.value)
        h.pick(a)
        assertEquals(1, navi.adds.size)
    }

    @Test
    fun failureCanRetryOrReturnAndLateResultIsIgnored() {
        val navi = Manual()
        val h = ctl(navi)
        h.pick(a)
        navi.adds[0].second(Result.failure(IllegalStateException("x")))
        assertTrue(h.state.value is HandoffState.Failed)
        h.retry()
        assertEquals(HandoffState.Sending(a), h.state.value)
        h.backToCompare()
        navi.adds[1].second(Result.success(Unit))
        assertEquals(HandoffState.Idle, h.state.value)
    }

    @Test
    fun fullAsksWhichToReplaceThenSwaps() {
        val navi = FakeNaviGateway()
        val h = ctl(navi)
        h.pick(a)
        h.pick(b)
        h.backToCompare()
        h.pick(c)
        assertEquals(HandoffState.NeedReplace(c, listOf(a, b)), h.state.value)
        h.replace(a)
        assertEquals(listOf(b, c), navi.waypoints)
        assertEquals(HandoffState.Done(c, undoable = true), h.state.value)
    }

    @Test
    fun undoRemovesWaypointWithinGraceAndExpires() {
        val navi = FakeNaviGateway()
        val h = ctl(navi)
        h.pick(a)
        h.undo()
        assertEquals(emptyList<StopPlace>(), navi.waypoints)
        assertEquals(HandoffState.Idle, h.state.value)

        val expired = ctl(navi, grace = 0)
        expired.pick(a)
        Thread.sleep(50)
        assertEquals(HandoffState.Done(a, undoable = false), expired.state.value)
        expired.undo()
        assertEquals(listOf(a), navi.waypoints)
    }
}
