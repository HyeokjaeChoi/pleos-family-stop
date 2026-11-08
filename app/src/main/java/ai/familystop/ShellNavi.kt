package ai.familystop

import ai.familystop.core.NaviGateway
import ai.familystop.core.StopCandidate
import ai.familystop.core.StopPlace
import ai.familystop.feature.voice.VoiceGuide

/** Announces every waypoint add by voice and reports successes to the [Session]; removal passes through. */
class ShellNavi(
    private val inner: NaviGateway,
    private val guide: VoiceGuide,
    private val session: Session,
    private val candidateOf: (StopPlace) -> StopCandidate?
) : NaviGateway by inner {
    override fun addWaypoint(place: StopPlace, onResult: (Result<Unit>) -> Unit) =
        inner.addWaypoint(place) { r ->
            guide.announceResult(place, r.isSuccess)
            if (r.isSuccess) candidateOf(place)?.let(session::onWaypointAdded)
            onResult(r)
        }
}
