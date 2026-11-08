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
    fun waypointsAddedElsewhereAreKnown() { // #42: 주행 카드가 이미 2개를 채웠다
        val navi = Manual()
        val shared = mutableListOf(a, b)
        val scope = CoroutineScope(Job() + Dispatchers.Unconfined)
        val h = HandoffController(scope, navi, held = shared)
        h.pick(c)
        assertEquals(HandoffState.NeedReplace(c, listOf(a, b)), h.state.value)
        h.backToCompare()
        h.pick(a) // 이미 들어간 후보: 게이트웨이 호출 없이 완료
        assertEquals(HandoffState.Done(a, undoable = false), h.state.value)
        assertTrue(navi.adds.isEmpty())
    }

    @Test
    fun failureCanRetryOrReturnAndLateResultIsIgnored() {
        val navi = Manual()
        val h = ctl(navi)
        h.pick(a)
        navi.adds[0].second(Result.failure(IllegalStateException("x")))
        navi.removes[0].second(Result.success(Unit)) // 실패한 add의 슬롯 정리
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
        assertEquals(HandoffState.Done(a, undoable = false), expired.state.value)
        expired.undo()
        assertEquals(listOf(a), navi.waypoints)
    }

    @Test
    fun cancelDuringReplaceDoesNotAddAndFailedSwapSaysSo() {
        val navi = Manual()
        val h = ctl(navi)
        h.pick(a)
        navi.adds[0].second(Result.success(Unit))
        h.backToCompare()
        h.pick(b)
        navi.adds[1].second(Result.success(Unit))
        h.backToCompare()
        h.pick(c)
        h.replace(a)
        h.backToCompare()
        navi.removes[0].second(Result.success(Unit))
        assertEquals(2, navi.adds.size) // C는 추가 요청되지 않음
        h.pick(a)
        navi.adds[2].second(Result.success(Unit))
        h.backToCompare()
        h.pick(c)
        h.replace(b)
        navi.removes[1].second(Result.success(Unit))
        navi.adds[3].second(Result.failure(IllegalStateException("x")))
        navi.removes[2].second(Result.success(Unit))
        assertTrue((h.state.value as HandoffState.Failed).reason.contains("지워졌"))
    }

    @Test
    fun undoFailureKeepsButtonAndFlagsIt() {
        val navi = Manual()
        val h = ctl(navi)
        h.pick(a)
        navi.adds[0].second(Result.success(Unit))
        h.undo()
        navi.removes[0].second(Result.failure(IllegalStateException("x")))
        assertEquals(HandoffState.Done(a, undoable = true, undoFailed = true), h.state.value)
    }

    @Test
    fun failedAddCleansUpSlotBeforeReportingSoRetryWorks() {
        val navi = Manual()
        val h = ctl(navi)
        h.pick(a)
        navi.adds[0].second(Result.failure(IllegalStateException("x")))
        assertEquals(HandoffState.Sending(a), h.state.value) // 정리 remove 응답 전
        assertEquals(listOf(a), navi.removes.map { it.first })
        navi.removes[0].second(Result.success(Unit))
        assertTrue(h.state.value is HandoffState.Failed)
    }

    @Test
    fun picksAreIgnoredWhileARequestIsStillPending() {
        val navi = Manual()
        val h = ctl(navi)
        h.pick(a)
        h.backToCompare()
        h.pick(a) // 첫 add 응답 전: 중복 add를 보내지 않는다
        assertEquals(1, navi.adds.size)
        navi.adds[0].second(Result.success(Unit))
        h.pick(a) // 이미 들어감: 새 요청·새 취소 유예 없음
        assertEquals(1, navi.adds.size)
        assertEquals(HandoffState.Done(a, undoable = false), h.state.value)
    }
}
