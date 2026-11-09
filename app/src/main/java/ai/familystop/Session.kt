package ai.familystop

import ai.familystop.core.DataSource
import ai.familystop.core.RouteEta
import ai.familystop.core.StopCandidate
import ai.familystop.core.StopPlace
import ai.familystop.feature.fallback.FallbackCause
import ai.familystop.feature.input.ChargeNeed
import ai.familystop.feature.input.DestinationsState
import ai.familystop.feature.input.TripDraft
import ai.familystop.feature.input.TripInput
import ai.familystop.feature.metrics.DataOrigin
import ai.familystop.feature.metrics.MetricsCollector
import ai.familystop.feature.metrics.TripMetrics
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.time.LocalDateTime
import kotlinx.coroutines.Job

/** 정차 화면 단계: 입력·추천 한 화면(PLAN) → 경유지 추가 후 충전 중 화면(ACTIVITY). 탭 없음(#65). */
enum class Phase { PLAN, ACTIVITY }

/** One trip's screen state. Held by [AppGraph] so it survives Activity recreation. */
class Session(private val metrics: MetricsCollector) {
    var tripId by mutableStateOf(metrics.startTrip())
        private set
    var phase by mutableStateOf(Phase.PLAN)
    var trip by mutableStateOf<TripInput?>(null)
    var draft by mutableStateOf(TripDraft())
    var destinations by mutableStateOf<DestinationsState>(DestinationsState.Loading)
    var eta by mutableStateOf<RouteEta?>(null)
    var etaFailed by mutableStateOf(false)
    var etaLoadedAt by mutableStateOf<LocalDateTime?>(null)
    var locating by mutableStateOf(false)
    var fallback by mutableStateOf<FallbackCause?>(null)
    var stop by mutableStateOf<StopCandidate?>(null)
        private set
    var locateJob: Job? = null
    var announced: Any? = null // last recommendation read aloud

    /** New trip = new random id (metrics are only recorded with consent, which has no screen now). */
    fun newTrip() {
        tripId = metrics.startTrip()
        stop = null
    }

    /** Waypoint accepted by the navigation app (from the handoff flow or the driving cards). */
    fun onWaypointAdded(c: StopCandidate) {
        stop = c
        record(c)
        if (trip?.chargeNeed == ChargeNeed.YES) phase = Phase.ACTIVITY
    }

    /** A cancelled stop leaves the charging screen. */
    fun onWaypointRemoved(place: StopPlace) {
        if (stop?.place != place) return
        stop = null
        phase = Phase.PLAN
    }

    private fun record(c: StopCandidate) {
        metrics.record(
            TripMetrics(
                tripId = tripId,
                origin = if (c.source == DataSource.VIRTUAL) DataOrigin.VIRTUAL else DataOrigin.SDK,
                recruitChannel = "demo",
                baselinePlanMinutes = null,
                prototypePlanMinutes = null,
                choiceReasons = listOf(c.reason),
                unresolvedNeeds = c.missed.map { it.name },
                spendWon = null
            )
        )
    }
}
