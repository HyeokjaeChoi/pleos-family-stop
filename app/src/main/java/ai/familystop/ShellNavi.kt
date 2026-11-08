package ai.familystop

import ai.familystop.core.NaviGateway
import ai.familystop.core.StopCandidate
import ai.familystop.core.StopPlace
import ai.familystop.feature.voice.VoiceGuide

/** Announces every waypoint add by voice; reports adds and removals to the [Session]. */
class ShellNavi(
    private val inner: NaviGateway,
    private val guide: VoiceGuide,
    private val session: Session,
    private val candidateOf: (StopPlace) -> StopCandidate?
) : NaviGateway {
    override fun addWaypoint(place: StopPlace, onResult: (Result<Unit>) -> Unit) =
        inner.addWaypoint(place) { r ->
            guide.announceResult(place, r.isSuccess)
            if (r.isSuccess) candidateOf(place)?.let(session::onWaypointAdded)
            onResult(r)
        }

    override fun removeWaypoint(place: StopPlace, onResult: (Result<Unit>) -> Unit) =
        inner.removeWaypoint(place) { r ->
            if (r.isSuccess) session.onWaypointRemoved(place)
            onResult(r)
        }
}
