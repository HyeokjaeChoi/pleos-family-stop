package ai.familystop.feature.drive

import ai.familystop.core.NaviGateway
import ai.familystop.core.StopPlace
import ai.familystop.core.designsystem.App
import ai.familystop.core.designsystem.AppButton
import ai.familystop.core.designsystem.AppCard
import ai.familystop.core.designsystem.AppIcons
import ai.familystop.core.designsystem.AppMotion
import ai.familystop.core.designsystem.EmptyState
import ai.familystop.core.designsystem.R as DsR
import ai.familystop.core.designsystem.Status
import ai.familystop.core.designsystem.StatusChip
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * [chip]은 상태를 항상 아이콘+문구 한 쌍으로만 받는다. A stop candidate shown as one large card:
 * [metrics] are (value, label) pairs shown big, [summary] is 1~2 short lines (DESIGN.md 8절).
 */
data class DriveCandidate(
    val place: StopPlace,
    val summary: String,
    val chip: Pair<Status, String>? = null,
    val metrics: List<Pair<String, String>> = emptyList()
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
    sourceLabel: String? = null,
    footer: @Composable () -> Unit = {},
    parkedContent: @Composable (InputDraft) -> Unit = {}
) {
    val draft = rememberSaveable(saver = DraftSaver) { InputDraft() }
    val tracker = remember { MovementTracker() }
    var reading by remember { mutableStateOf(tracker.reading) }
    val pickerSaver = remember(navi) { pickControllerSaver(navi, ::nowMs) }
    val picker = rememberSaveable(navi, saver = pickerSaver) {
        PickController(navi, ::nowMs)
    }
    var now by remember { mutableLongStateOf(nowMs()) }

    DisposableEffect(movement) {
        movement.start { state, reason -> reading = tracker.onSignal(state, reason, nowMs()) }
        onDispose { movement.stop() }
    }
    // A fresh Undoable must not read the 250 ms-old clock (it showed 11 s for the first frame).
    LaunchedEffect(picker.state) { now = nowMs() }
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
        Column(Modifier.fillMaxSize()) {
            if (candidates.isEmpty()) {
                if (reading.state == MovementState.UNKNOWN) UnknownMovementChip()
                EmptyState(
                    stringResource(R.string.drive_empty_title),
                    stringResource(R.string.drive_empty_body),
                    Modifier.weight(1f).fillMaxWidth()
                )
            } else {
                // Content is centred in the free height instead of hugging the top-left (#50 D1/D2).
                Spacer(Modifier.weight(1f))
                Column(verticalArrangement = Arrangement.spacedBy(App.space.xl)) {
                    DriveHeader(sourceLabel)
                    if (reading.state == MovementState.UNKNOWN) UnknownMovementChip()
                    val locked =
                        picker.state !is PickState.Idle && picker.state !is PickState.Failed
                    val adding = (picker.state as? PickState.Adding)?.place
                    Row(
                        Modifier.height(IntrinsicSize.Max),
                        horizontalArrangement = Arrangement.spacedBy(App.space.lg)
                    ) {
                        candidates.take(2).forEach { c ->
                            CandidateCard(
                                c,
                                enabled = !locked,
                                busy = adding == c.place,
                                modifier = Modifier.weight(1f).fillMaxHeight()
                            ) { picker.pick(c.place) }
                        }
                    }
                    StatusSlot(picker.state, { now }) { picker.undo() }
                }
                Spacer(Modifier.weight(1f))
            }
            footer()
        }
    }
}

@Composable
private fun UnknownMovementChip() =
    StatusChip(Status.Unknown, stringResource(R.string.drive_unknown_movement))

@Composable
private fun DriveHeader(sourceLabel: String?) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(App.space.md)
    ) {
        Icon(
            AppIcons.EvStation,
            contentDescription = null,
            tint = App.colors.textPrimary,
            modifier = Modifier.size(App.icon.primary)
        )
        Text(
            stringResource(R.string.drive_header),
            style = App.type.driveTitle,
            color = App.colors.textPrimary
        )
        Spacer(Modifier.weight(1f))
        // ADR-0001: demo data must be labelled on screen.
        if (sourceLabel != null) {
            Text(sourceLabel, style = App.type.stopBody, color = App.colors.textSecondary)
        }
    }
}

@Composable
private fun CandidateCard(
    c: DriveCandidate,
    enabled: Boolean,
    busy: Boolean,
    modifier: Modifier,
    onPick: () -> Unit
) {
    AppCard(modifier, onClick = onPick, enabled = enabled, spacing = App.space.lg) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(App.space.lg)
        ) {
            Text(
                c.place.name,
                style = App.type.driveTitle,
                color = App.colors.textPrimary,
                maxLines = 2,
                modifier = Modifier.weight(1f)
            )
            c.chip?.let { (status, text) -> StatusChip(status, text) }
        }
        if (c.metrics.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(App.space.xl)) {
                c.metrics.forEach { (value, label) ->
                    Column {
                        Text(value, style = App.type.driveTitle, color = App.colors.textPrimary)
                        Text(label, style = App.type.stopBody, color = App.colors.textSecondary)
                    }
                }
            }
        }
        Text(c.summary, style = App.type.driveBody, color = App.colors.textSecondary, maxLines = 2)
        Spacer(Modifier.weight(1f))
        AppButton(
            stringResource(if (busy) R.string.drive_adding else R.string.drive_add_waypoint),
            null,
            Modifier.fillMaxWidth(),
            enabled = enabled || busy,
            large = true,
            icon = AppIcons.AddLocationAlt,
            background = if (enabled || busy) null else App.colors.bgRaised
        )
    }
}

/** Fixed-height slot so showing/hiding a status never moves the cards (#50 D6). */
@Composable
private fun StatusSlot(state: PickState, now: () -> Long, onUndo: () -> Unit) {
    val reduce = AppMotion.reduce
    val risePx = with(LocalDensity.current) { AppMotion.RISE_DP.dp.roundToPx() }
    Box(
        Modifier
            .fillMaxWidth()
            .height(App.statusSlot)
            .semantics { liveRegion = LiveRegionMode.Polite },
        contentAlignment = Alignment.CenterStart
    ) {
        // 입장 200ms ease-out(+12dp 상승), 퇴장 160ms. 카운트다운은 같은 클래스라 재전환 없음.
        AnimatedContent(
            state,
            contentKey = { it::class },
            transitionSpec = {
                (
                    (
                        fadeIn(AppMotion.spec(reduce, AppMotion.ENTER_MS)) +
                            slideInVertically(AppMotion.spec(reduce, AppMotion.ENTER_MS)) { risePx }
                        ) togetherWith
                        fadeOut(AppMotion.spec(reduce, AppMotion.EXIT_MS, AppMotion.EaseExit))
                    ).using(
                    SizeTransform(clip = false) { _, _ ->
                        AppMotion.spec(reduce, AppMotion.ENTER_MS)
                    }
                )
            },
            label = "statusSlot"
        ) { st ->
            when (st) {
                is PickState.Undoable -> UndoBar(st, now, onUndo)
                is PickState.Failed -> StatusBar(
                    Status.Danger,
                    stringResource(R.string.drive_failed_chip),
                    stringResource(R.string.drive_failed_message)
                )
                is PickState.Done ->
                    if (st.note != null) {
                        StatusBar(
                            Status.Danger,
                            stringResource(R.string.drive_check_chip),
                            stringResource(st.note.message())
                        )
                    } else {
                        StatusChip(
                            Status.Ok,
                            stringResource(R.string.drive_added_chip, st.place.name)
                        )
                    }
                else -> Unit
            }
        }
    }
}

/** Shared raised container of the fixed status slot (UndoBar / failure). */
@Composable
private fun SlotBar(content: @Composable RowScope.() -> Unit) {
    Row(
        Modifier.fillMaxSize()
            .clip(RoundedCornerShape(App.radius.md))
            .background(App.colors.bgRaised)
            .padding(horizontal = App.space.xl),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(App.space.lg),
        content = content
    )
}

@Composable
private fun StatusBar(status: Status, chip: String, message: String) = SlotBar {
    StatusChip(status, chip)
    Text(
        message,
        style = App.type.driveBody,
        color = App.colors.textPrimary,
        modifier = Modifier.weight(1f)
    )
}

@Composable
private fun UndoBar(s: PickState.Undoable, now: () -> Long, onUndo: () -> Unit) {
    val sec = ((s.expiresAtMs - now() + 999) / 1000).coerceAtLeast(0)
    SlotBar {
        Text(
            stringResource(
                R.string.drive_undo_bar,
                s.place.name,
                pluralStringResource(DsR.plurals.ds_unit_seconds, sec.toInt(), sec.toInt())
            ),
            style = App.type.driveBody,
            color = App.colors.textPrimary,
            modifier = Modifier.weight(1f)
        )
        AppButton(
            stringResource(R.string.drive_undo),
            onUndo,
            primary = false,
            large = true,
            icon = AppIcons.Undo,
            background = App.colors.bgSurface
        )
    }
}

private fun PickNote.message() = when (this) {
    PickNote.UndoFailed -> R.string.drive_note_undo_failed
    PickNote.ResultLost -> R.string.drive_note_result_lost
}
