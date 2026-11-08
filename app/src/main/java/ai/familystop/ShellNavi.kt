package ai.familystop

import ai.familystop.core.NaviGateway
import ai.familystop.core.StopCandidate
import ai.familystop.core.StopPlace
import ai.familystop.feature.voice.VoiceGuide

/**
 * Announces every waypoint add by voice; reports adds and removals to the [Session].
 * [held] is the waypoint ledger shared by the driving cards and the handoff flow (#42, main thread only).
 */
class ShellNavi(
    private val inner: NaviGateway,
    private val guide: VoiceGuide,
    private val session: Session,
    private val candidateOf: (StopPlace) -> StopCandidate?
) : NaviGateway {
    val held = mutableListOf<StopPlace>()

    override fun addWaypoint(place: StopPlace, onResult: (Result<Unit>) -> Unit) =
        inner.addWaypoint(place) { r ->
            guide.announceResult(place, r.isSuccess)
            if (r.isSuccess) {
                if (place !in held) held += place
                candidateOf(place)?.let(session::onWaypointAdded)
            }
            onResult(r)
        }

    override fun removeWaypoint(place: StopPlace, onResult: (Result<Unit>) -> Unit) =
        inner.removeWaypoint(place) { r ->
            if (r.isSuccess) {
                held.remove(place)
                session.onWaypointRemoved(place)
            }
            onResult(r)
        }
}
