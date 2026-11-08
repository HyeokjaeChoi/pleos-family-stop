package ai.familystop

import ai.familystop.core.DataSource
import ai.familystop.core.StopCandidate
import ai.familystop.feature.fallback.FallbackCause
import ai.familystop.feature.input.ChargeNeed
import ai.familystop.feature.input.TripInput
import ai.familystop.feature.metrics.DataOrigin
import ai.familystop.feature.metrics.MetricsCollector
import ai.familystop.feature.metrics.TripMetrics
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Job

enum class Step(val label: String) {
    INPUT("입력"),
    RECOMMEND("추천"),
    ACTIVITY("충전 중 활동"),
    CONSENT("측정 동의")
}

/** One trip's screen state. Held by [AppGraph] so it survives Activity recreation. */
class Session(private val metrics: MetricsCollector) {
    val tripId = metrics.startTrip()
    var step by mutableStateOf(Step.INPUT)
    var trip by mutableStateOf<TripInput?>(null)
    var locating by mutableStateOf(false)
    var fallback by mutableStateOf<FallbackCause?>(null)
    var picked by mutableStateOf<StopCandidate?>(null)
    var consented by mutableStateOf(false)
    var locateJob: Job? = null

    val charging get() = trip?.chargeNeed == ChargeNeed.YES

    /** Waypoint accepted by the navigation app (from the parked list or the driving cards). */
    fun onWaypointAdded(c: StopCandidate) {
        picked = c
        if (charging) step = Step.ACTIVITY
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
