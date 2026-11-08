package ai.familystop.feature.drive

import ai.familystop.core.NaviGateway
import ai.familystop.core.StopPlace
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/** A stop candidate shown as one large card; [summary] is 1~3 short lines (DESIGN.md 8절). */
data class DriveCandidate(val place: StopPlace, val summary: String)

/** Input draft that survives driving <-> parked switches and Activity recreation (F-OAXFEH). */
class InputDraft(initial: String = "") {
    var text by mutableStateOf(initial)
}

private val DraftSaver = Saver<InputDraft, String>(save = { it.text }, restore = { InputDraft(it) })

private class Palette(night: Boolean) { // DESIGN.md 2절
    val bgBase = if (night) Color(0xFF080A0C) else Color(0xFFDDE3E8)
    val surface = if (night) Color(0xFF1B2126) else Color(0xFFFFFFFF)
    val raised = if (night) Color(0xFF272E34) else Color(0xFFEEF1F4)
    val text = if (night) Color(0xFFF2F5F7) else Color(0xFF11181D)
    val textSub = if (night) Color(0xFFB4BEC6) else Color(0xFF46535C)
    val accent = if (night) Color(0xFF5CB8FF) else Color(0xFF0A62BA)
    val onAccent = if (night) Color(0xFF06121C) else Color(0xFFFFFFFF)
    val danger = if (night) Color(0xFFFF6B6B) else Color(0xFFB71C1C)
}

private val DriveTitle =
    TextStyle(fontSize = 40.sp, lineHeight = 52.sp, fontWeight = FontWeight.Bold)
private val DriveBody =
    TextStyle(fontSize = 32.sp, lineHeight = 44.sp, fontWeight = FontWeight.Medium)

private fun nowMs() = System.nanoTime() / 1_000_000

/**
 * Driving/unknown: at most 2 big cards, tap = `addWaypoint` + 10 s undo. Parked: [parkedContent]
 * (detail screens belong to other issues). Scrolling, expanding and comparing are absent while
 * restricted (ADR-0003, ADR-0004).
 */
private const val TAG = "FamilyStopMove"

@Composable
fun DriveScreen(
    candidates: List<DriveCandidate>,
    navi: NaviGateway,
    movement: MovementSignalSource,
    modifier: Modifier = Modifier,
    parkedContent: @Composable (InputDraft) -> Unit = {}
) {
    val night = isSystemInDarkTheme()
    val p = remember(night) { Palette(night) }
    val draft = rememberSaveable(saver = DraftSaver) { InputDraft() }
    val tracker = remember { MovementTracker() }
    var reading by remember { mutableStateOf(tracker.reading) }
    val picker = rememberSaveable(navi, saver = pickControllerSaver(navi, ::nowMs)) {
        PickController(navi, ::nowMs)
    }
    var now by remember { mutableLongStateOf(nowMs()) }

    DisposableEffect(movement) {
        movement.start { state, reason ->
            Log.i(TAG, "signal $state ($reason)")
            reading = tracker.onSignal(state, reason, nowMs())
        }
        onDispose { movement.stop() }
    }
    // Logged for E2E timing of the 2 s hold (docs/e2e).
    LaunchedEffect(reading.state) { Log.i(TAG, "confirmed ${reading.state}") }
    LaunchedEffect(picker) {
        while (true) {
            delay(250)
            now = nowMs()
            reading = tracker.tick(now)
            picker.tick(now)
        }
    }

    Box(modifier.fillMaxSize().background(p.bgBase).padding(24.dp)) {
        if (!reading.restricted) {
            parkedContent(draft)
            return@Box
        }
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            if (reading.state == MovementState.UNKNOWN) {
                Text("이동 상태 미확인 · 주행 중 제한 적용", style = DriveBody, color = p.textSub)
            }
            if (candidates.isEmpty()) {
                Text("정차 후 입력하면 추천 후보가 여기에 나타나요", style = DriveBody, color = p.textSub)
            }
            val locked = picker.state !is PickState.Idle && picker.state !is PickState.Failed
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                candidates.take(2).forEach { c ->
                    CandidateCard(c, p, !locked, Modifier.weight(1f).fillMaxSize()) {
                        picker.pick(c.place)
                    }
                }
            }
            when (val s = picker.state) {
                is PickState.Undoable -> UndoBar(s, now, p) { picker.undo() }
                is PickState.Failed -> Text(s.message, style = DriveBody, color = p.danger)
                is PickState.Done -> Text(
                    s.note ?: "${s.place.name} 경유지 추가됨",
                    style = DriveBody,
                    color = if (s.note != null) p.danger else p.textSub
                )
                else -> Unit
            }
        }
    }
}

@Composable
private fun CandidateCard(
    c: DriveCandidate,
    p: Palette,
    enabled: Boolean,
    modifier: Modifier,
    onPick: () -> Unit
) {
    Column(
        modifier
            .clip(RoundedCornerShape(24.dp))
            .background(p.surface)
            .clickable(enabled = enabled, role = Role.Button, onClick = onPick)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(c.place.name, style = DriveTitle, color = p.text, maxLines = 2)
        Text(
            c.summary,
            style = DriveBody,
            color = p.textSub,
            maxLines = 3,
            modifier = Modifier.weight(1f)
        )
        Box(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 76.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(if (enabled) p.accent else p.raised),
            contentAlignment = Alignment.Center
        ) {
            Text("경유지로 추가", style = DriveBody, color = if (enabled) p.onAccent else p.textSub)
        }
    }
}

@Composable
private fun UndoBar(s: PickState.Undoable, now: Long, p: Palette, onUndo: () -> Unit) {
    val sec = ((s.expiresAtMs - now + 999) / 1000).coerceAtLeast(0)
    Row(
        Modifier.fillMaxWidth().clip(
            RoundedCornerShape(16.dp)
        ).background(p.surface).padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(
            "${s.place.name} 경유지로 추가했습니다",
            style = DriveBody,
            color = p.text,
            modifier = Modifier.weight(1f)
        )
        Box(
            Modifier
                .heightIn(min = 76.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(p.accent)
                .clickable(role = Role.Button, onClick = onUndo)
                .padding(horizontal = 32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("취소 (${sec}초)", style = DriveBody, color = p.onAccent)
        }
    }
}
