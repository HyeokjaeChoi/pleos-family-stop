package ai.familystop.feature.handoff

import ai.familystop.core.NaviGateway
import ai.familystop.core.StopPlace
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** Pleos 경유지 최대치(ADR-0003). */
const val MAX_WAYPOINTS = 2

/** 주행 중 한 번 탭 뒤 취소를 허용하는 시간(ADR-0003). ponytail: 시연하며 조정. */
const val UNDO_GRACE_MS = 10_000L

sealed interface HandoffState {
    data object Idle : HandoffState

    /** 응답 대기. 성공으로 보지 않는다(F-JTGAFQ). */
    data class Sending(val place: StopPlace) : HandoffState

    /** 경유지가 가득 차 어느 것을 바꿀지 묻는다. */
    data class NeedReplace(val place: StopPlace, val options: List<StopPlace>) : HandoffState

    /** 게이트웨이가 성공 응답을 준 뒤에만. 전달 성공 ≠ 방문. */
    data class Done(val place: StopPlace, val undoable: Boolean, val undoFailed: Boolean = false) :
        HandoffState

    /** 실패 → 재시도([HandoffController.retry]) 또는 비교 복귀([HandoffController.backToCompare]). */
    data class Failed(val place: StopPlace, val reason: String) : HandoffState
}

/**
 * 선택한 정차 지점을 경유지로 넘기는 흐름 (F-JTGAFQ, ADR-0003). 메인 스레드 전용.
 * 이 컨트롤러가 넣은 경유지만 안다 — 내비 UI에서 사용자가 직접 넣은 경유지는 모른다(포트에 조회가 없음).
 */
class HandoffController(
    private val scope: CoroutineScope,
    private val navi: NaviGateway,
    private val undoGraceMs: Long = UNDO_GRACE_MS
) {
    private val _state = MutableStateFlow<HandoffState>(HandoffState.Idle)
    val state: StateFlow<HandoffState> = _state

    private val held = mutableListOf<StopPlace>()
    private var seq = 0

    /** 같은 장소를 이미 보내는 중이거나 넣은 뒤면 병합(무시), 다른 장소를 보내는 중이면 무시한다. */
    fun pick(place: StopPlace) {
        when (val s = _state.value) {
            is HandoffState.Sending, is HandoffState.NeedReplace -> return
            is HandoffState.Done -> if (s.place == place) return
            else -> Unit
        }
        if (place in held) return done(place)
        if (held.size >= MAX_WAYPOINTS) {
            _state.value = HandoffState.NeedReplace(place, held.toList())
        } else {
            add(place)
        }
    }

    /** [NeedReplace]에서 [target]을 빼고 대기 중이던 장소를 넣는다. */
    fun replace(target: StopPlace) {
        val s = _state.value as? HandoffState.NeedReplace ?: return
        if (target !in s.options) return
        val mine = ++seq
        _state.value = HandoffState.Sending(s.place)
        navi.removeWaypoint(target) { r ->
            r.onSuccess { held.remove(target) }
            if (mine != seq) return@removeWaypoint // 그사이 취소됨: 새 경유지는 넣지 않는다
            if (r.isSuccess) {
                addAs(mine, s.place, "기존 경유지는 이미 지워졌어요. 다시 시도해 주세요.")
            } else {
                fail(mine, s.place, "기존 경유지를 바꾸지 못했어요.")
            }
        }
    }

    /** [Failed]에서 같은 장소를 다시 보낸다. */
    fun retry() {
        val s = _state.value as? HandoffState.Failed ?: return
        _state.value = HandoffState.Idle
        pick(s.place)
    }

    /** 비교 화면으로 돌아간다. 늦게 오는 응답은 상태를 바꾸지 않는다. */
    fun backToCompare() {
        seq++
        _state.value = HandoffState.Idle
    }

    /** 유예 안의 [Done]을 되돌린다. */
    fun undo() {
        val s = _state.value as? HandoffState.Done ?: return
        if (!s.undoable) return
        navi.removeWaypoint(s.place) { r ->
            r.onSuccess { held.remove(s.place) }
            val cur = _state.value
            if (cur !is HandoffState.Done || cur.place != s.place) return@removeWaypoint
            _state.value = if (r.isSuccess) HandoffState.Idle else cur.copy(undoFailed = true)
        }
    }

    private fun add(place: StopPlace) {
        val mine = ++seq
        _state.value = HandoffState.Sending(place)
        addAs(mine, place)
    }

    private fun addAs(mine: Int, place: StopPlace, failReason: String = "길 안내에 전달하지 못했어요.") {
        navi.addWaypoint(place) { r ->
            // 늦은 성공이어도 경유지는 이미 들어갔으니 기록은 남긴다.
            r.onSuccess { if (place !in held) held += place }
            if (mine != seq) return@addWaypoint
            if (r.isSuccess) {
                done(place)
            } else {
                fail(mine, place, failReason)
            }
        }
    }

    private fun done(place: StopPlace) {
        val mine = ++seq
        _state.value = HandoffState.Done(place, undoable = true)
        scope.launch {
            delay(undoGraceMs)
            val s = _state.value
            if (mine == seq && s is HandoffState.Done) _state.value = s.copy(undoable = false)
        }
    }

    private fun fail(mine: Int, place: StopPlace, reason: String) {
        if (mine == seq) _state.value = HandoffState.Failed(place, reason)
    }
}
