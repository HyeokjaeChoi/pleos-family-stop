package ai.familystop

import ai.familystop.core.DataSource
import ai.familystop.core.NaviGateway
import ai.familystop.core.StopCandidate
import ai.familystop.core.StopNeed
import ai.familystop.core.StopRequest
import ai.familystop.core.designsystem.App
import ai.familystop.core.designsystem.AppButton
import ai.familystop.core.designsystem.Status
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
import ai.familystop.feature.handoff.HandoffScreen
import ai.familystop.feature.input.TripInput
import ai.familystop.feature.input.TripInputScreen
import ai.familystop.feature.metrics.ConsentScreen
import ai.familystop.feature.recommend.FETCH_TIMEOUT_MS
import ai.familystop.feature.recommend.RecommendScreen
import ai.familystop.feature.recommend.RecommendState
import ai.familystop.feature.recommend.driveCards
import ai.familystop.feature.voice.VoiceBar
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import java.time.LocalDateTime
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private fun StopCandidate.driveCard() = DriveCandidate(
    place,
    "$reason\n+${extraMinutes}분 · ${"%.1f".format(detourKm)}km · ${
        if (source == DataSource.VIRTUAL) "가상 데이터" else "실제 데이터"
    }",
    if (missed.isEmpty()) Status.Ok to "필요 조건 충족" else Status.Danger to "일부 미충족"
)

@Composable
fun AppRoot(graph: AppGraph) {
    val context = LocalContext.current
    val s = graph.session
    val holder = rememberSaveableStateHolder()
    val rec by graph.recommend.state.collectAsState()
    var soc by remember { mutableStateOf<Int?>(null) }
    val movement = remember { CarUxMovementSource(context) }
    val navi = graph.shellNavi // stable: DriveScreen keys its PickController (10 s undo) on it
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
        // Old cards must not stay selectable while the new location is pending or failed.
        graph.recommend.clear()
        graph.guide.announce(emptyList())
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

    // FINE only: PlatformLocationSource uses GPS below API 31, which rejects coarse-only grants.
    fun locationGranted() = context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED

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
        s.newTrip()
        s.trip = trip
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
            Parked(graph, rec, soc, navi, ::submit, ::requestLocation, holder)
        }
        VoiceBar(graph.guide, Modifier.padding(App.space.lg))
    }
}

@Composable
private fun Parked(
    graph: AppGraph,
    rec: RecommendState,
    soc: Int?,
    navi: NaviGateway,
    onSubmit: (TripInput) -> Unit,
    onRetry: () -> Unit,
    holder: SaveableStateHolder
) {
    val s = graph.session
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            now = LocalDateTime.now()
        }
    }
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(App.space.lg)) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(App.space.lg),
            verticalArrangement = Arrangement.spacedBy(App.space.lg)
        ) {
            Step.entries.filter { it != Step.ACTIVITY || s.charging }.forEach { step ->
                AppButton(step.label, { s.step = step }, primary = s.step == step)
            }
        }
        // Keeps each step's typed-in state while driving removes the parked UI from composition.
        holder.SaveableStateProvider(s.step.name) {
            when (s.step) {
                Step.INPUT -> TripInputScreen(
                    parked = true,
                    autoSocPercent = soc,
                    onSubmit = onSubmit
                )
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
                        if (yes) s.grantConsent()
                        s.step = Step.INPUT
                    },
                    onWithdraw = s::withdraw
                )
            }
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
    Column(verticalArrangement = Arrangement.spacedBy(App.space.lg)) {
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
                onPick = graph.handoff::pick,
                onRetry = onRetry,
                modifier = Modifier.weight(1f, fill = false)
            )
        }
        val handoff by graph.handoff.state.collectAsState()
        HandoffScreen(
            handoff,
            onReplace = graph.handoff::replace,
            onRetry = graph.handoff::retry,
            onBack = graph.handoff::backToCompare,
            onUndo = graph.handoff::undo
        )
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
