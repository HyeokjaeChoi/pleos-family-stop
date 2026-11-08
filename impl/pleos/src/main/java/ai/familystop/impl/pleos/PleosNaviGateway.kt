package ai.familystop.impl.pleos

import ai.familystop.core.NaviGateway
import ai.familystop.core.StopPlace
import ai.pleos.playground.navi.constants.NaviErrorCode
import ai.pleos.playground.navi.constants.WaypointCmdType
import ai.pleos.playground.navi.constants.WaypointIndex
import ai.pleos.playground.navi.data.RequestWaypointInfo
import ai.pleos.playground.navi.data.WaypointChangedInfo
import ai.pleos.playground.navi.helper.NaviHelper
import ai.pleos.playground.navi.helper.listener.NaviHelperEventListener
import android.content.Context
import android.os.Handler
import android.os.Looper

/**
 * [NaviGateway] on Pleos NaviHelper (ADR-0001, ADR-0003).
 * Main thread only: SDK callbacks are re-posted to main, so `onResult` callbacks run there too.
 * Requests run one at a time (queued until connected), so slot state never races.
 * App-scoped; call [close] to release the service binding.
 */
class PleosNaviGateway(context: Context) : NaviGateway {
    private class Op(
        val place: StopPlace,
        val cmd: WaypointCmdType,
        val onResult: (Result<Unit>) -> Unit
    ) {
        var index: WaypointIndex? = null
        var timeout: Runnable? = null
    }

    private val main = Handler(Looper.getMainLooper())
    private val helper = NaviHelper(context.applicationContext)
    private val slots = WaypointSlots()
    private val queue = ArrayDeque<Op>()
    private var inFlight: Op? = null
    private var connected = false
    private var connecting = false
    private var closed = false

    private val listener = object : NaviHelperEventListener {
        override fun onWaypointChanged(info: WaypointChangedInfo) {
            main.post { onChanged(info) }
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

    init {
        connect()
    }

    // ponytail: no sync with waypoints set in the Navi UI (getWaypointInfo); reconcile if the demo needs it.
    private fun connect() {
        connecting = true
        helper.initialize(
            {
                main.post {
                    if (closed) return@post
                    connecting = false
                    connected = true
                    helper.addListener(listener)
                    drain()
                }
            },
            {
                main.post {
                    if (closed) return@post
                    connecting = false
                    connected = false
                    failQueued(IllegalStateException("navi init failed: ${it.message}"))
                }
            }
        )
    }

    fun close() {
        if (closed) return
        closed = true
        connected = false
        helper.removeListener(listener)
        helper.release()
        val error = IllegalStateException("navi closed")
        inFlight?.let { finish(it, Result.failure(error)) }
        failQueued(error)
    }

    override fun addWaypoint(place: StopPlace, onResult: (Result<Unit>) -> Unit) =
        enqueue(Op(place, WaypointCmdType.ADD, onResult))

    override fun removeWaypoint(place: StopPlace, onResult: (Result<Unit>) -> Unit) =
        enqueue(Op(place, WaypointCmdType.REMOVE, onResult))

    private fun enqueue(op: Op) {
        if (closed) return op.onResult(Result.failure(IllegalStateException("navi closed")))
        queue += op
        // A failed init retries on the next request.
        if (!connected && !connecting) connect()
        drain()
    }

    private fun drain() {
        while (connected && inFlight == null) {
            val op = queue.removeFirstOrNull() ?: return
            if (op.cmd == WaypointCmdType.ADD) send(op) else sendRemove(op)
        }
    }

    private fun send(op: Op) {
        val i = slots.reserve(op.place)
            ?: return op.onResult(
                Result.failure(IllegalStateException("waypoints full or duplicate"))
            )
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
        if (op != null &&
            op.index == info.index &&
            op.cmd == info.cmdType
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
        // Emulator re-route after addWaypoint takes 5~9 s before onWaypointChanged (docs/agents/emulator.md §6).
        const val REQUEST_TIMEOUT_MS = 15_000L
    }
}
