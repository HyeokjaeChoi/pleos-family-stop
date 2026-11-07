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

/** [NaviGateway] on Pleos NaviHelper (ADR-0001, ADR-0003). Call from the main thread. */
class PleosNaviGateway(context: Context) : NaviGateway {
    private class Pending(
        val index: Int,
        val cmd: WaypointCmdType,
        val onResult: (Result<Unit>) -> Unit,
        val place: StopPlace
    )

    private val helper = NaviHelper(context)
    private val slots = WaypointSlots()
    private val pending = mutableListOf<Pending>()
    private var connected = false
    private var failure: Throwable? = null

    private val listener = object : NaviHelperEventListener {
        override fun onWaypointChanged(info: WaypointChangedInfo) {
            val i = info.index.ordinal
            val p = pending.firstOrNull { it.index == i && it.cmd == info.cmdType } ?: return
            pending.remove(p)
            if (p.cmd == WaypointCmdType.REMOVE) slots.release(p.index)
            p.onResult(Result.success(Unit))
        }

        // The SDK error does not say which request failed: fail all in flight and roll back.
        override fun onError(code: NaviErrorCode) =
            failPending(IllegalStateException("navi error: $code"))
    }

    init {
        helper.initialize(
            {
                connected = true
                helper.addListener(listener)
            },
            { failure = it }
        )
    }

    override fun addWaypoint(place: StopPlace, onResult: (Result<Unit>) -> Unit) {
        notReady()?.let { return onResult(Result.failure(it)) }
        val i = slots.reserve(place)
            ?: return onResult(Result.failure(IllegalStateException("waypoints full or duplicate")))
        pending += Pending(i, WaypointCmdType.ADD, onResult, place)
        helper.addWaypoint(
            RequestWaypointInfo(
                latitude = place.lat,
                longitude = place.lon,
                poiName = place.name,
                waypointIndex = index(i),
                poiId = place.poiId.orEmpty(),
                poiSubId = "",
                address = ""
            )
        )
    }

    override fun removeWaypoint(place: StopPlace, onResult: (Result<Unit>) -> Unit) {
        notReady()?.let { return onResult(Result.failure(it)) }
        val i = slots.indexOf(place) ?: return onResult(Result.success(Unit)) // nothing to undo
        pending += Pending(i, WaypointCmdType.REMOVE, onResult, place)
        helper.removeWaypoint(index(i))
    }

    private fun notReady(): Throwable? =
        failure ?: IllegalStateException("navi not connected").takeIf { !connected }

    private fun failPending(error: Throwable) {
        val failed = pending.toList()
        pending.clear()
        failed.forEach {
            if (it.cmd == WaypointCmdType.ADD) slots.release(it.index)
            it.onResult(Result.failure(error))
        }
    }

    private fun index(i: Int) = if (i == 0) WaypointIndex.FIRST else WaypointIndex.SECOND
}
