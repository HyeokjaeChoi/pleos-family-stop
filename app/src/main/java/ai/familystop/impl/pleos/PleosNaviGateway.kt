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
 * Main thread only: SDK callbacks are re-posted to main, so [onResult] callbacks run there too.
 * App-scoped; call [close] to release the service binding.
 */
class PleosNaviGateway(context: Context) : NaviGateway {
    private class Pending(
        val index: WaypointIndex,
        val cmd: WaypointCmdType,
        val onResult: (Result<Unit>) -> Unit
    ) {
        var timeout: Runnable? = null
    }

    private val main = Handler(Looper.getMainLooper())
    private val helper = NaviHelper(context.applicationContext)
    private val slots = WaypointSlots()
    private val pending = mutableListOf<Pending>()
    private var connected = false

    private val listener = object : NaviHelperEventListener {
        override fun onWaypointChanged(info: WaypointChangedInfo) {
            main.post { onChanged(info) }
        }

        // The SDK error does not say which request failed: fail all in flight and roll back.
        override fun onError(code: NaviErrorCode) {
            main.post { failPending(IllegalStateException("navi error: $code")) }
        }
    }

    init {
        connect()
    }

    // ponytail: no sync with waypoints set in the Navi UI (getWaypointInfo); reconcile if the demo needs it.
    private fun connect() {
        helper.initialize(
            {
                main.post {
                    connected = true
                    helper.addListener(listener)
                }
            },
            { main.post { connected = false } }
        )
    }

    fun close() {
        helper.removeListener(listener)
        helper.release()
        failPending(IllegalStateException("navi closed"))
    }

    override fun addWaypoint(place: StopPlace, onResult: (Result<Unit>) -> Unit) {
        if (!ready()) return onResult(Result.failure(IllegalStateException("navi not connected")))
        val i = slots.reserve(place)
            ?: return onResult(Result.failure(IllegalStateException("waypoints full or duplicate")))
        val index = WaypointIndex.entries[i]
        track(Pending(index, WaypointCmdType.ADD, onResult))
        helper.addWaypoint(
            RequestWaypointInfo(
                latitude = place.lat,
                longitude = place.lon,
                poiName = place.name,
                waypointIndex = index,
                poiId = place.poiId.orEmpty(),
                poiSubId = "",
                address = ""
            )
        )
    }

    override fun removeWaypoint(place: StopPlace, onResult: (Result<Unit>) -> Unit) {
        if (!ready()) return onResult(Result.failure(IllegalStateException("navi not connected")))
        val i = slots.indexOf(place) ?: return onResult(Result.success(Unit)) // nothing to undo
        val index = WaypointIndex.entries[i]
        if (pending.any { it.index == index && it.cmd == WaypointCmdType.REMOVE }) {
            return onResult(Result.success(Unit)) // double tap while the first remove is in flight
        }
        track(Pending(index, WaypointCmdType.REMOVE, onResult))
        helper.removeWaypoint(index)
    }

    // After a failed init the next call retries; initialize() is a no-op while connecting/connected.
    private fun ready(): Boolean {
        if (!connected) connect()
        return connected
    }

    private fun track(p: Pending) {
        p.timeout = Runnable { fail(p, IllegalStateException("navi timeout")) }
        main.postDelayed(p.timeout!!, REQUEST_TIMEOUT_MS)
        pending += p
    }

    private fun onChanged(info: WaypointChangedInfo) {
        val p = pending.firstOrNull { it.index == info.index && it.cmd == info.cmdType }
        if (info.cmdType == WaypointCmdType.REMOVE) {
            // Also covers removals made in the Navi UI.
            val all = info.index == WaypointIndex.ALL
            (if (all) 0..1 else listOf(info.index.ordinal)).forEach(slots::release)
        }
        p ?: return
        done(p)
        p.onResult(Result.success(Unit))
    }

    private fun done(p: Pending) {
        pending.remove(p)
        p.timeout?.let(main::removeCallbacks)
    }

    private fun fail(p: Pending, error: Throwable) {
        done(p)
        if (p.cmd == WaypointCmdType.ADD) slots.release(p.index.ordinal)
        p.onResult(Result.failure(error))
    }

    private fun failPending(error: Throwable) = pending.toList().forEach { fail(it, error) }

    private companion object {
        const val REQUEST_TIMEOUT_MS = 5_000L
    }
}
