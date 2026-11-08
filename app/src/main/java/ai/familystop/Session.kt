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
    var tripId by mutableStateOf(metrics.startTrip())
        private set
    var step by mutableStateOf(Step.INPUT)
    var trip by mutableStateOf<TripInput?>(null)
    var locating by mutableStateOf(false)
    var fallback by mutableStateOf<FallbackCause?>(null)
    var consented by mutableStateOf(false)
    private var lastPick: StopCandidate? = null
    var locateJob: Job? = null
    var announced: Any? = null // last recommendation read aloud

    val charging get() = trip?.chargeNeed == ChargeNeed.YES

    /** New trip = new random id and a fresh consent question (PL-NGEOJQ: consent is per trip). */
    fun newTrip() {
        tripId = metrics.startTrip()
        consented = false
        lastPick = null
    }

    /** Driver consent; a stop already handed off in this trip is recorded right away. */
    fun grantConsent(): Boolean {
        consented = metrics.grantConsent(tripId, isDriver = true)
        if (consented) lastPick?.let(::record)
        return consented
    }

    fun withdraw() {
        if (metrics.withdraw(tripId)) consented = false
    }

    /** Waypoint accepted by the navigation app (from the handoff flow or the driving cards). */
    fun onWaypointAdded(c: StopCandidate) {
        lastPick = c
        record(c)
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
