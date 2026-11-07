package ai.familystop.feature.drive

import ai.familystop.core.NaviGateway
import ai.familystop.core.StopPlace
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

sealed interface PickState {
    data object Idle : PickState
    data class Adding(val place: StopPlace) : PickState
    data class Undoable(val place: StopPlace, val expiresAtMs: Long) : PickState
    data class Done(val place: StopPlace, val note: String? = null) : PickState
    data class Failed(val place: StopPlace, val message: String) : PickState
}

/** One tap adds the waypoint at once; a 10 s undo window follows (ADR-0003). One pick per screen. */
class PickController(
    private val navi: NaviGateway,
    private val clock: () -> Long,
    private val undoMs: Long = 10_000
) {
    var state: PickState by mutableStateOf(PickState.Idle)
        private set

    fun pick(place: StopPlace) {
        if (state !is PickState.Idle && state !is PickState.Failed) return
        state = PickState.Adding(place)
        navi.addWaypoint(place) { r ->
            state = r.fold(
                { PickState.Undoable(place, clock() + undoMs) },
                { PickState.Failed(place, "경유지를 추가하지 못했습니다. 다시 누르거나 정차 후 선택하세요.") }
            )
        }
    }

    fun undo() {
        val s = state as? PickState.Undoable ?: return
        state = PickState.Idle
        navi.removeWaypoint(s.place) { r ->
            if (r.isFailure &&
                state == PickState.Idle
            ) {
                state =
                    PickState.Done(s.place, "취소하지 못했습니다. 정차 후 내비에서 지우세요.")
            }
        }
    }

    fun tick(nowMs: Long) {
        val s = state as? PickState.Undoable ?: return
        if (nowMs >= s.expiresAtMs) state = PickState.Done(s.place)
    }
}
