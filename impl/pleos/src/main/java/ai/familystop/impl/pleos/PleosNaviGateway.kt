package ai.familystop.impl.pleos

import ai.familystop.core.NaviGateway
import ai.familystop.core.StopPlace
import ai.pleos.playground.navi.constants.NaviErrorCode
import ai.pleos.playground.navi.constants.WaypointCmdType
import ai.pleos.playground.navi.constants.WaypointIndex
import ai.pleos.playground.navi.data.RequestWaypointInfo
import ai.pleos.playground.navi.data.WaypointChangedInfo
import ai.pleos.playground.navi.data.WaypointInfo
import ai.pleos.playground.navi.helper.listener.NaviHelperEventListener
import android.os.Handler
import android.os.Looper

/**
 * [NaviGateway] on Pleos NaviHelper (ADR-0001, ADR-0003).
 * Main thread only: SDK callbacks are re-posted to main, so `onResult` callbacks run there too.
 * Requests run one at a time (queued until connected), so slot state never races.
 * App-scoped; the Navi connection belongs to the shared [NaviSession].
 */
class PleosNaviGateway(private val session: NaviSession) : NaviGateway {
    private class Op(
        val place: StopPlace,
        val cmd: WaypointCmdType,
        val onResult: (Result<Unit>) -> Unit
    ) {
        var index: WaypointIndex? = null
        var timeout: Runnable? = null
    }

    private val main = Handler(Looper.getMainLooper())
    private val helper get() = session.helper
    private val slots = WaypointSlots()
    private val queue = ArrayDeque<Op>()
    private var inFlight: Op? = null
    private var ready = false
    private var syncing = false
    private var connecting = false

    private val listener = object : NaviHelperEventListener {
        override fun onWaypointChanged(info: WaypointChangedInfo) {
            main.post { onChanged(info) }
        }

        override fun onWayPointInfo(list: List<WaypointInfo>) {
            val places = list.map {
                StopPlace(it.name, it.latitude, it.longitude, it.poiId?.takeIf(String::isNotEmpty))
            }
            main.post {
                if (!syncing) return@post
                slots.sync(places)
                endSync()
            }
        }

        // The SDK error does not say which request failed: fail the one in flight.
        override fun onError(code: NaviErrorCode) {
            main.post {
                inFlight?.let {
                    finish(it, Result.failure(IllegalStateException("navi error: $code")))
                }
            }
        }
    }

    // ponytail: no sync with waypoints set in the Navi UI (getWaypointInfo); reconcile if the demo needs it.
    private fun ensureReady() {
        if (ready || connecting) return
        connecting = true
        session.whenReady(
            {
                connecting = false
                ready = true
                helper.addListener(listener)
                startSync()
            },
            {
                connecting = false
                failQueued(it)
            }
        )
    }

    override fun addWaypoint(place: StopPlace, onResult: (Result<Unit>) -> Unit) =
        enqueue(Op(place, WaypointCmdType.ADD, onResult))

    override fun removeWaypoint(place: StopPlace, onResult: (Result<Unit>) -> Unit) =
        enqueue(Op(place, WaypointCmdType.REMOVE, onResult))

    private fun enqueue(op: Op) {
        queue += op
        // A failed init retries on the next request.
        ensureReady()
        drain()
    }

    // Navi keeps waypoints across app restarts, and adding into an occupied slot is ignored silently (no event,
    // no error: #69). So learn the real slot state once before the first request.
    private val syncTimeout = Runnable { endSync() }

    private fun startSync() {
        syncing = true
        main.postDelayed(syncTimeout, SYNC_TIMEOUT_MS)
        helper.getWaypointInfo()
    }

    private fun endSync() {
        syncing = false
        main.removeCallbacks(syncTimeout)
        drain()
    }

    private fun drain() {
        while (ready && !syncing && inFlight == null) {
            val op = queue.removeFirstOrNull() ?: return
            if (op.cmd == WaypointCmdType.ADD) send(op) else sendRemove(op)
        }
    }

    private fun send(op: Op) {
        // Already a waypoint (left from an earlier run): nothing to send.
        if (slots.indexOf(op.place) != null) return op.onResult(Result.success(Unit))
        val i = slots.reserve(op.place)
            ?: return op.onResult(Result.failure(IllegalStateException("waypoints full")))
        start(op, WaypointIndex.entries[i])
        helper.addWaypoint(
            RequestWaypointInfo(
                latitude = op.place.lat,
                longitude = op.place.lon,
                poiName = op.place.name,
                waypointIndex = op.index!!,
                poiId = op.place.poiId.orEmpty(),
                poiSubId = "",
                address = ""
            )
        )
    }

    private fun sendRemove(op: Op) {
        // Not in a slot: nothing to undo (also a repeated undo after the first one finished).
        val i = slots.indexOf(op.place) ?: return op.onResult(Result.success(Unit))
        start(op, WaypointIndex.entries[i])
        helper.removeWaypoint(op.index!!)
    }

    private fun start(op: Op, index: WaypointIndex) {
        op.index = index
        op.timeout = Runnable { finish(op, Result.failure(IllegalStateException("navi timeout"))) }
        main.postDelayed(op.timeout!!, REQUEST_TIMEOUT_MS)
        inFlight = op
    }

    private fun onChanged(info: WaypointChangedInfo) {
        val op = inFlight
        if (info.cmdType == WaypointCmdType.REMOVE) {
            // Also covers removals made in the Navi UI.
            if (info.index ==
                WaypointIndex.ALL
            ) {
                (0..1).forEach(slots::release)
            } else {
                slots.release(info.index.ordinal)
            }
        }
        // Navi may report an ADD under another index than requested (E2E: 2nd add arrived as FIRST), and only one
        // request is in flight, so any ADD completes it. REMOVE keeps the index match (ALL/other removals differ).
        if (op != null &&
            op.cmd == info.cmdType &&
            (info.cmdType == WaypointCmdType.ADD || op.index == info.index)
        ) {
            finish(op, Result.success(Unit))
        }
    }

    // A failed or timed-out add keeps its slot: Navi may still apply it late, so the undo must send a real removal.
    // A failed removal keeps its slot too; only a confirmed removal event (onChanged) frees it.
    private fun finish(op: Op, result: Result<Unit>) {
        if (inFlight !== op) return
        inFlight = null
        op.timeout?.let(main::removeCallbacks)
        op.onResult(result)
        drain()
    }

    private fun failQueued(error: Throwable) {
        // Snapshot first: callbacks may enqueue retries, which must wait for the next init.
        val batch = queue.toList()
        queue.clear()
        batch.forEach { it.onResult(Result.failure(error)) }
    }

    private companion object {
        // Measured emulator re-route after addWaypoint: 5~12 s before onWaypointChanged (#14 E2E; 5 s timed out a
        // successful add). Trade-off: a genuinely lost reply holds the picker/handoff up to 15 s. Do not go below 12 s.
        const val REQUEST_TIMEOUT_MS = 15_000L
        const val SYNC_TIMEOUT_MS = 5_000L
    }
}
