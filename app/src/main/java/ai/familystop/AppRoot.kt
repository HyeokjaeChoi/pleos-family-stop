package ai.familystop

import ai.familystop.core.DataSource
import ai.familystop.core.DestinationKind
import ai.familystop.core.NaviGateway
import ai.familystop.core.StopCandidate
import ai.familystop.core.StopNeed
import ai.familystop.core.StopRequest
import ai.familystop.core.designsystem.App
import ai.familystop.core.designsystem.Status
import ai.familystop.core.designsystem.StatusChip
import ai.familystop.feature.activity.ChargingActivityScreen
import ai.familystop.feature.activity.Journey
import ai.familystop.feature.drive.CarUxMovementSource
import ai.familystop.feature.drive.DriveCandidate
import ai.familystop.feature.drive.DriveScreen
import ai.familystop.feature.fallback.Capability
import ai.familystop.feature.fallback.DataFailure
import ai.familystop.feature.fallback.FallbackCard
import ai.familystop.feature.fallback.FallbackCause
import ai.familystop.feature.handoff.HandoffScreen
import ai.familystop.feature.input.ConditionsStep
import ai.familystop.feature.input.DestinationStep
import ai.familystop.feature.input.DestinationsState
import ai.familystop.feature.input.PlanStep
import ai.familystop.feature.input.PlanSummaryBar
import ai.familystop.feature.input.ValidationResult
import ai.familystop.feature.input.destinationKey
import ai.familystop.feature.input.validate
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
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import java.time.LocalDateTime
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private fun StopCandidate.driveCard() = DriveCandidate(
    place,
    reason,
    if (missed.isEmpty()) Status.Ok to "필요 조건 충족" else Status.Danger to "일부 미충족",
    listOf("+${extraMinutes}분" to "도착 지연", "${"%.1f".format(detourKm)}km" to "우회 거리")
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

    // Input changed: new trip + fresh recommendation for the current needs (right pane refreshes at once).
    fun refetch() {
        s.newTrip()
        requestLocation()
    }

    fun reload() {
        s.rawList = null
        s.listFailed = false
        graph.destinations.load { r ->
            s.rawList = r.getOrNull()
            s.listFailed = r.isFailure
        }
        graph.destinations.currentEta { r ->
            s.eta = r.getOrNull()
            s.etaFailed = r.isFailure || r.getOrNull() == null
            s.etaLoadedAt = LocalDateTime.now()
            // 내비에 설정된 목적지는 기본 선택(#65).
            val current = (s.destinations as? DestinationsState.Ready)?.list
                ?.firstOrNull { it.kind == DestinationKind.CURRENT }
            if (current != null && s.draft.destinationKey == null) {
                s.draft = s.draft.copy(destinationKey = destinationKey(current))
            }
        }
    }
    LaunchedEffect(Unit) { if (s.rawList == null && !s.listFailed) reload() }

    val destList = (s.destinations as? DestinationsState.Ready)?.list.orEmpty()
    val valid = (
        validate(
            s.draft,
            destList,
            soc,
            LocalDateTime.now()
        ) as? ValidationResult.Ok
        )?.input
    LaunchedEffect(valid) { s.trip = valid }
    // 후보 단계에 들어서거나(또는 거기서 유효성·필요 사항이 바뀌면) 조회한다. 휠을 돌릴 때마다 조회하지 않는다.
    LaunchedEffect(s.step, valid != null, valid?.needs) {
        // 필요 사항이 없으면 추천할 게 없어 위치 권한도 묻지 않는다.
        if (s.step == PlanStep.CANDIDATES && valid != null && valid.needs.isNotEmpty()) {
            delay(400)
            refetch()
        }
    }

    val cards = rec.driveCards()
    DriveScreen(
        candidates = cards.map { it.driveCard() },
        navi = navi,
        movement = movement,
        sourceLabel = if (cards.any { it.source == DataSource.VIRTUAL }) "가상 데이터(시연)" else null,
        footer = { VoiceBar(graph.guide) }
    ) {
        Parked(graph, rec, soc, navi, ::requestLocation, ::reload)
    }
}

@Composable
private fun Parked(
    graph: AppGraph,
    rec: RecommendState,
    soc: Int?,
    navi: NaviGateway,
    onRetry: () -> Unit,
    onReload: () -> Unit
) {
    val s = graph.session
    val trip = s.trip
    val stop = s.stop
    if (s.phase == Phase.ACTIVITY && trip != null && stop != null) {
        val base = s.etaLoadedAt?.takeIf { s.eta?.destinationName == trip.destination.name }
            ?.let { at -> s.eta?.let { at.plusSeconds(it.remainSeconds.toLong()) } }
            ?.plusMinutes(stop.extraMinutes.toLong())
        ChargingActivityScreen(
            journey = Journey(
                stopName = stop.place.name,
                stopDetail = "${stop.place.name}까지 ${"%.1f".format(
                    stop.detourKm
                )}km · +${stop.extraMinutes}분",
                destinationName = trip.destination.name,
                baseArrival = base,
                wishedArrival = trip.arrival
            ),
            tasks = trip.needs.map { it.label },
            sessionId = s.tripId
        )
        return
    }
    Column(
        Modifier.fillMaxSize().padding(App.space.lg),
        verticalArrangement = Arrangement.spacedBy(App.space.lg)
    ) {
        PlanSummaryBar(s.draft, s.destinations, s.step, onStep = { s.step = it })
        val body = Modifier.weight(1f)
        when (s.step) {
            PlanStep.DESTINATION -> DestinationStep(
                s.destinations,
                s.draft,
                onDraft = { s.draft = it },
                onRetry = onReload,
                onNext = { s.step = PlanStep.CONDITIONS },
                modifier = body
            )
            PlanStep.CONDITIONS -> ConditionsStep(
                destinations = s.destinations,
                eta = s.eta,
                etaFailed = s.etaFailed,
                autoSocPercent = soc,
                draft = s.draft,
                onDraft = { s.draft = it },
                onNext = { s.step = PlanStep.CANDIDATES },
                modifier = body
            )
            PlanStep.CANDIDATES ->
                if (s.trip != null) {
                    Recommend(graph, rec, navi, onRetry, body)
                } else {
                    StatusChip(Status.Unknown, "도착 시각을 정해 주세요")
                }
        }
    }
}

@Composable
private fun Recommend(
    graph: AppGraph,
    rec: RecommendState,
    navi: NaviGateway,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val s = graph.session
    Column(modifier, verticalArrangement = Arrangement.spacedBy(App.space.lg)) {
        val cause = s.fallback
        when {
            s.locating -> Text("현재 위치를 확인하는 중…")
            cause != null -> FallbackCard(
                cause,
                nowMs = System.currentTimeMillis(),
                onRetry = onRetry,
                onManual = {
                    s.draft = s.draft.copy(destinationKey = null)
                    s.step = PlanStep.DESTINATION
                }
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
