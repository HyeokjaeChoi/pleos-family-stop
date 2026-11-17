package ai.familystop.feature.drive

import ai.familystop.core.StopPlace
import ai.familystop.fake.FakeNaviGateway
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DriveLogicTest {
    @Test
    fun parkedNeedsTwoSecondsAndAnyOtherSignalResets() {
        val t = MovementTracker()
        assertEquals(MovementState.UNKNOWN, t.reading.state)
        t.onSignal(MovementState.PARKED, "p", 0)
        assertTrue(t.tick(1_999).restricted)
        t.onSignal(MovementState.DRIVING, "d", 1_500) // resets the hold
        t.onSignal(MovementState.PARKED, "p", 1_600)
        assertTrue(t.tick(3_500).restricted)
        assertEquals(MovementState.PARKED, t.tick(3_600).state)
        assertEquals(MovementState.DRIVING, t.onSignal(MovementState.DRIVING, "d", 4_000).state)
    }

    @Test
    fun tapAddsAtOnceUndoRemovesAndWindowExpires() {
        val navi = FakeNaviGateway()
        var now = 0L
        val c = PickController(navi, { now })
        val a = StopPlace("a", 0.0, 0.0)
        c.pick(a)
        assertEquals(listOf(a), navi.waypoints)
        assertEquals(PickState.Undoable(a, 10_000), c.state)
        c.pick(StopPlace("b", 0.0, 0.0)) // ignored while locked
        assertEquals(1, navi.waypoints.size)
        c.undo()
        assertTrue(navi.waypoints.isEmpty() && c.state == PickState.Idle)
        c.pick(a)
        now = 10_000
        c.tick(now)
        assertEquals(PickState.Done(a), c.state)
        c.undo() // too late
        assertEquals(1, navi.waypoints.size)
    }

    @Test
    fun cardsStayLockedWhileUndoPendingAndFailedUndoIsReported() {
        var cb: ((Result<Unit>) -> Unit)? = null
        val navi = object : ai.familystop.core.NaviGateway {
            override fun addWaypoint(place: StopPlace, onResult: (Result<Unit>) -> Unit) =
                onResult(Result.success(Unit))
            override fun removeWaypoint(place: StopPlace, onResult: (Result<Unit>) -> Unit) {
                cb = onResult
            }
        }
        val c = PickController(navi, { 0 })
        val a = StopPlace("a", 0.0, 0.0)
        c.pick(a)
        c.undo()
        c.pick(StopPlace("b", 0.0, 0.0)) // ignored: removal still pending
        assertEquals(PickState.Removing(a), c.state)
        cb!!(Result.failure(IllegalStateException()))
        assertEquals(PickState.Done(a, PickNote.UndoFailed), c.state)
    }
}
