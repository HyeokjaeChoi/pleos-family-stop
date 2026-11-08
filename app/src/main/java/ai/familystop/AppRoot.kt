package ai.familystop

import ai.familystop.core.DataSource
import ai.familystop.core.NaviGateway
import ai.familystop.core.StopCandidate
import ai.familystop.core.StopNeed
import ai.familystop.core.StopPlace
import ai.familystop.core.StopRequest
import ai.familystop.feature.activity.ActivityCandidate
import ai.familystop.feature.activity.ActivityKind
import ai.familystop.feature.activity.ChargingActivityScreen
import ai.familystop.feature.drive.CarUxMovementSource
import ai.familystop.feature.drive.DriveCandidate
import ai.familystop.feature.drive.DriveScreen
import ai.familystop.feature.fallback.Capability
import ai.familystop.feature.fallback.DataFailure
import ai.familystop.feature.fallback.FallbackCard
import ai.familystop.feature.fallback.FallbackCause
import ai.familystop.feature.input.TripInput
import ai.familystop.feature.input.TripInputScreen
import ai.familystop.feature.metrics.ConsentScreen
import ai.familystop.feature.recommend.FETCH_TIMEOUT_MS
import ai.familystop.feature.recommend.RecommendScreen
import ai.familystop.feature.recommend.RecommendState
import ai.familystop.feature.recommend.driveCards
import ai.familystop.feature.voice.VoiceBar
import ai.familystop.feature.voice.VoiceGuide
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.time.LocalDateTime
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Announces the result of every waypoint change and reports successes to the [Session]. */
private class ShellNavi(
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
            if (r.isSuccess && session.picked?.place == place) session.picked = null
            onResult(r)
        }
}

private fun StopCandidate.driveCard() = DriveCandidate(
    place,
    "$reason\n+${extraMinutes}분 · ${"%.1f".format(detourKm)}km · ${
        if (source == DataSource.VIRTUAL) "가상 데이터" else "실제 데이터"
    }"
)

@Composable
fun AppRoot(graph: AppGraph) {
    val context = LocalContext.current
    val s = graph.session
    val rec by graph.recommend.state.collectAsState()
    var soc by remember { mutableStateOf<Int?>(null) }
    val movement = remember { CarUxMovementSource(context) }
    // Stable instance: DriveScreen keys its PickController (and the 10 s undo) on this object.
    val navi = remember {
        ShellNavi(graph.navi, graph.guide, s) { place ->
            (graph.recommend.state.value as? RecommendState.Success)
                ?.candidates?.firstOrNull { it.place == place }
        }
    }

    DisposableEffect(Unit) {
        graph.energy.start { soc = it }
        onDispose { graph.energy.stop() }
    }
    LaunchedEffect(rec) {
        // Once per result: Activity recreation must not read the same candidates again.
        val success = rec as? RecommendState.Success
        if (success != null && s.announced !== success) {
            s.announced = success
            graph.guide.announce(success.candidates)
        }
    }

    fun locate() {
        val trip = s.trip ?: return
        s.locateJob?.cancel()
        s.fallback = null
        s.locating = true
        var done = false
        fun fail(cause: FallbackCause) {
            if (done) return
            done = true
            graph.location.stop()
            s.locating = false
            s.fallback = cause
        }
        s.locateJob = graph.scope.launch {
            // process scope: survives Activity recreation
            delay(FETCH_TIMEOUT_MS)
            fail(FallbackCause.Data(DataFailure("현재 위치", "지금은 현재 위치 기준 추천을 할 수 없어요.")))
        }
        graph.location.start(
            onUpdate = { p ->
                if (!done) {
                    done = true
                    s.locateJob?.cancel()
                    graph.location.stop()
                    s.locating = false
                    graph.recommend.request(
                        StopRequest(p, trip.needs.map { StopNeed.valueOf(it.name) }.toSet())
                    )
                }
            },
            onUnavailable = { fail(FallbackCause.Data(DataFailure("현재 위치", it))) }
        )
    }

    val permission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) locate() else s.fallback = FallbackCause.Permission(Capability.LOCATION)
    }

    fun locationGranted() = listOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ).any { context.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }

    // Permissions are asked on first use of the feature that needs them (F-JUUGZR).
    fun requestLocation() {
        if (locationGranted()) {
            locate()
        } else {
            permission.launch(
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }
    }

    fun submit(trip: TripInput) {
        s.trip = trip
        s.picked = null
        s.step = Step.RECOMMEND
        requestLocation()
    }

    Column(Modifier.fillMaxSize()) {
        DriveScreen(
            candidates = rec.driveCards().map { it.driveCard() },
            navi = navi,
            movement = movement,
            modifier = Modifier.weight(1f)
        ) {
            Parked(graph, rec, soc, navi, ::submit, ::requestLocation)
        }
        VoiceBar(graph.guide, Modifier.padding(24.dp))
    }
}

@Composable
private fun Parked(
    graph: AppGraph,
    rec: RecommendState,
    soc: Int?,
    navi: NaviGateway,
    onSubmit: (TripInput) -> Unit,
    onRetry: () -> Unit
) {
    val s = graph.session
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            now = LocalDateTime.now()
        }
    }
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Step.entries.filter { it != Step.ACTIVITY || s.charging }.forEach { step ->
                val m = Modifier.heightIn(min = 76.dp)
                if (s.step == step) {
                    Button({ s.step = step }, m) { Text(step.label) }
                } else {
                    OutlinedButton({ s.step = step }, m) { Text(step.label) }
                }
            }
        }
        when (s.step) {
            Step.INPUT -> TripInputScreen(parked = true, autoSocPercent = soc, onSubmit = onSubmit)
            Step.RECOMMEND -> Recommend(graph, rec, navi, onRetry)
            Step.ACTIVITY -> ChargingActivityScreen(
                parked = true,
                candidates = (rec as? RecommendState.Success)?.candidates.orEmpty()
                    .mapNotNull(::activityOf),
                chargeSessionId = s.tripId,
                autoCharging = false, // no Pleos charging-state API; user confirms (PL-UJFQXQ)
                autoChargeEndsAt = null,
                now = now
            )
            Step.CONSENT -> ConsentScreen(
                parked = true,
                tripId = s.tripId,
                consented = s.consented,
                onResult = { yes ->
                    if (yes) s.consented = graph.metrics.grantConsent(s.tripId, isDriver = true)
                    s.step = Step.INPUT
                },
                onWithdraw = { if (graph.metrics.withdraw(s.tripId)) s.consented = false }
            )
        }
    }
}

@Composable
private fun Recommend(
    graph: AppGraph,
    rec: RecommendState,
    navi: NaviGateway,
    onRetry: () -> Unit
) {
    val s = graph.session
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        val cause = s.fallback
        when {
            s.locating -> Text("현재 위치를 확인하는 중…")
            cause != null -> FallbackCard(
                cause,
                nowMs = System.currentTimeMillis(),
                onRetry = onRetry,
                onManual = { s.step = Step.INPUT }
            )
            else -> RecommendScreen(
                state = rec,
                onPick = { navi.addWaypoint(it) {} },
                onRetry = onRetry,
                modifier = Modifier.weight(1f, fill = false)
            )
        }
        s.picked?.let { c ->
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("${c.place.name} 경유지로 추가했습니다")
                OutlinedButton({
                    navi.removeWaypoint(c.place) {}
                }, Modifier.heightIn(min = 76.dp)) {
                    Text("취소")
                }
            }
        }
    }
}

// ponytail: 왕복·활동 시간은 시연 고정값. 경로 기반 계산은 외부 제공처 연동 시.
private fun activityOf(c: StopCandidate): ActivityCandidate? {
    val kind = when {
        StopNeed.MEAL in c.met -> ActivityKind.MEAL
        StopNeed.KIDS_REST in c.met -> ActivityKind.REST
        else -> return null
    }
    return ActivityCandidate(
        c.place.name,
        kind,
        2 * c.extraMinutes,
        if (kind ==
            ActivityKind.MEAL
        ) {
            30
        } else {
            15
        }
    )
}
