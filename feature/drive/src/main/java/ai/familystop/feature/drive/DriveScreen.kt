package ai.familystop.feature.drive

import ai.familystop.core.NaviGateway
import ai.familystop.core.StopPlace
import ai.familystop.core.designsystem.App
import ai.familystop.core.designsystem.AppButton
import ai.familystop.core.designsystem.AppCard
import ai.familystop.core.designsystem.Status
import ai.familystop.core.designsystem.StatusChip
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import kotlinx.coroutines.delay

/** A stop candidate shown as one large card; [summary] is 1~3 short lines (DESIGN.md 8절). */
data class DriveCandidate(
    val place: StopPlace,
    val summary: String,
    val status: Status? = null,
    val statusText: String = ""
)

/** Input draft that survives driving <-> parked switches and Activity recreation (F-OAXFEH). */
class InputDraft(initial: String = "") {
    var text by mutableStateOf(initial)
}

private val DraftSaver = Saver<InputDraft, String>(save = { it.text }, restore = { InputDraft(it) })

private fun nowMs() = System.nanoTime() / 1_000_000

/**
 * Driving/unknown: at most 2 big cards, tap = `addWaypoint` + 10 s undo. Parked: [parkedContent]
 * (detail screens belong to other issues). Scrolling, expanding and comparing are absent while
 * restricted (ADR-0003, ADR-0004).
 */
@Composable
fun DriveScreen(
    candidates: List<DriveCandidate>,
    navi: NaviGateway,
    movement: MovementSignalSource,
    modifier: Modifier = Modifier,
    parkedContent: @Composable (InputDraft) -> Unit = {}
) {
    val draft = rememberSaveable(saver = DraftSaver) { InputDraft() }
    val tracker = remember { MovementTracker() }
    var reading by remember { mutableStateOf(tracker.reading) }
    val picker = rememberSaveable(navi, saver = pickControllerSaver(navi, ::nowMs)) {
        PickController(navi, ::nowMs)
    }
    var now by remember { mutableLongStateOf(nowMs()) }

    DisposableEffect(movement) {
        movement.start { state, reason -> reading = tracker.onSignal(state, reason, nowMs()) }
        onDispose { movement.stop() }
    }
    LaunchedEffect(picker) {
        while (true) {
            delay(250)
            now = nowMs()
            reading = tracker.tick(now)
            picker.tick(now)
        }
    }

    Box(modifier.fillMaxSize().background(App.colors.bgBase).padding(App.space.lg)) {
        if (!reading.restricted) {
            parkedContent(draft)
            return@Box
        }
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(App.space.lg)) {
            if (reading.state == MovementState.UNKNOWN) {
                Text(
                    "이동 상태 미확인 · 주행 중 제한 적용",
                    style = App.type.driveBody,
                    color = App.colors.textSecondary
                )
            }
            if (candidates.isEmpty()) {
                Text(
                    "정차 후 입력하면 추천 후보가 여기에 나타나요",
                    style = App.type.driveBody,
                    color = App.colors.textSecondary
                )
            }
            val locked = picker.state !is PickState.Idle && picker.state !is PickState.Failed
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(App.space.lg)) {
                candidates.take(2).forEach { c ->
                    CandidateCard(c, !locked, Modifier.weight(1f).fillMaxSize()) {
                        picker.pick(c.place)
                    }
                }
            }
            when (val s = picker.state) {
                is PickState.Undoable -> UndoBar(s, now) { picker.undo() }
                is PickState.Failed -> Text(
                    s.message,
                    style = App.type.driveBody,
                    color = App.colors.statusDanger
                )
                is PickState.Done -> StatusChip(
                    if (s.note != null) Status.Danger else Status.Ok,
                    s.note ?: "${s.place.name} 경유지 추가됨"
                )
                else -> Unit
            }
        }
    }
}

@Composable
private fun CandidateCard(
    c: DriveCandidate,
    enabled: Boolean,
    modifier: Modifier,
    onPick: () -> Unit
) {
    AppCard(modifier, onClick = onPick, enabled = enabled) {
        Text(
            c.place.name,
            style = App.type.driveTitle,
            color = App.colors.textPrimary,
            maxLines = 2
        )
        Text(
            c.summary,
            style = App.type.driveBody,
            color = App.colors.textSecondary,
            maxLines = 3
        )
        c.status?.let { StatusChip(it, c.statusText) }
        Spacer(Modifier.weight(1f))
        AppButton("경유지로 추가", null, Modifier.fillMaxWidth(), enabled = enabled, large = true)
    }
}

@Composable
private fun UndoBar(s: PickState.Undoable, now: Long, onUndo: () -> Unit) {
    val sec = ((s.expiresAtMs - now + 999) / 1000).coerceAtLeast(0)
    Row(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(App.radius.md))
            .background(App.colors.bgSurface)
            .padding(horizontal = App.space.lg),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(App.space.lg)
    ) {
        Text(
            "${s.place.name} 경유지로 추가했습니다",
            style = App.type.driveBody,
            color = App.colors.textPrimary,
            modifier = Modifier.weight(1f)
        )
        AppButton("취소 (${sec}초)", onUndo, large = true)
    }
}
