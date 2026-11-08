package ai.familystop.feature.drive

import ai.familystop.core.NaviGateway
import ai.familystop.core.StopPlace
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.setValue

sealed interface PickState {
    data object Idle : PickState
    data class Adding(val place: StopPlace) : PickState
    data class Undoable(val place: StopPlace, val expiresAtMs: Long) : PickState
    data class Removing(val place: StopPlace) : PickState
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

    internal fun restore(s: PickState) {
        state = s
    }

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
        state = PickState.Removing(s.place) // keeps cards locked until the removal answers
        navi.removeWaypoint(s.place) { r ->
            state = if (r.isSuccess) {
                PickState.Idle
            } else {
                PickState.Done(s.place, "취소하지 못했습니다. 정차 후 내비에서 지우세요.")
            }
        }
    }

    fun tick(nowMs: Long) {
        val s = state as? PickState.Undoable ?: return
        if (nowMs >= s.expiresAtMs) state = PickState.Done(s.place)
    }
}

private const val LOST_NOTE = "진행 중이던 요청 결과를 확인하지 못했습니다. 정차 후 내비의 경유지를 확인하세요."

/**
 * Survives Activity recreation (e.g. day/night switch). An in-flight add/remove loses its callback,
 * so it restores as a locked [PickState.Done] with a note instead of accepting another pick.
 */
fun pickControllerSaver(navi: NaviGateway, clock: () -> Long) = Saver<PickController, List<Any?>>(
    save = { c ->
        when (val s = c.state) {
            is PickState.Undoable -> listOf(s.expiresAtMs) + s.place.toList()
            is PickState.Done -> listOf(s.note) + s.place.toList()
            is PickState.Adding -> listOf(LOST_NOTE) + s.place.toList()
            is PickState.Removing -> listOf(LOST_NOTE) + s.place.toList()
            else -> emptyList()
        }
    },
    restore = { l ->
        PickController(navi, clock).also { c ->
            if (l.isNotEmpty()) {
                val place =
                    StopPlace(l[1] as String, l[2] as Double, l[3] as Double, l[4] as String?)
                c.restore(
                    when (val head = l[0]) {
                        is Long -> PickState.Undoable(place, head)
                        else -> PickState.Done(place, head as String?)
                    }
                )
            }
        }
    }
)

private fun StopPlace.toList() = listOf(name, lat, lon, poiId)
