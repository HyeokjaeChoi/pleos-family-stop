package ai.familystop

import ai.familystop.core.DataSource
import ai.familystop.core.Destination
import ai.familystop.core.RouteEta
import ai.familystop.core.StopCandidate
import ai.familystop.core.StopPlace
import ai.familystop.feature.fallback.FallbackCause
import ai.familystop.feature.input.ChargeNeed
import ai.familystop.feature.input.DestinationsState
import ai.familystop.feature.input.PlanStep
import ai.familystop.feature.input.TripDraft
import ai.familystop.feature.input.TripInput
import ai.familystop.feature.input.mergeCurrent
import ai.familystop.feature.metrics.DataOrigin
import ai.familystop.feature.metrics.MetricsCollector
import ai.familystop.feature.metrics.TripMetrics
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.time.LocalDateTime
import kotlinx.coroutines.Job

/** 정차 화면 단계: 입력·추천 3단계(PLAN, [PlanStep]) → 경유지 추가 후 충전 중 화면(ACTIVITY). 탭 없음(#65). */
enum class Phase { PLAN, ACTIVITY }

/** One trip's screen state. Held by [AppGraph] so it survives Activity recreation. */
class Session(private val metrics: MetricsCollector) {
    var tripId by mutableStateOf(metrics.startTrip())
        private set
    var phase by mutableStateOf(Phase.PLAN)
    var step by mutableStateOf(PlanStep.DESTINATION) // PLAN 안의 3단계(#72), 화면을 벗어나도 유지
    var trip by mutableStateOf<TripInput?>(null)
    var draft by mutableStateOf(TripDraft())
    var rawList by mutableStateOf<List<Destination>?>(null) // null = not loaded yet
    var listFailed by mutableStateOf(false)
    var eta by mutableStateOf<RouteEta?>(null)
    var etaFailed by mutableStateOf(false)
    var etaLoadedAt by mutableStateOf<LocalDateTime?>(null)

    /** Navi's list plus the route it is guiding now; derived so the two async answers cannot overwrite each other. */
    val destinations: DestinationsState by derivedStateOf {
        val merged = mergeCurrent(rawList.orEmpty(), eta)
        when {
            merged.isNotEmpty() -> DestinationsState.Ready(merged)
            rawList != null || listFailed -> DestinationsState.Unavailable
            else -> DestinationsState.Loading
        }
    }
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
                choiceReasons = listOf(c.reason.needs.joinToString("+") { it.name }),
                unresolvedNeeds = c.missed.map { it.name },
                spendWon = null
            )
        )
    }
}
