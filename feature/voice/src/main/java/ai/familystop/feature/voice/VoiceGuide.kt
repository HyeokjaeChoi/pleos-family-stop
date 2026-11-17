package ai.familystop.feature.voice

import ai.familystop.core.StopCandidate
import ai.familystop.core.StopPlace
import ai.familystop.core.StopReason
import ai.familystop.core.VoiceOutput
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** 주행 중 첫 안내에서 번호로 읽는 후보 수 (ADR-0002·0003). 3번째는 "다음" 요청 시에만. */
const val SPOKEN_FIRST = 2

/** 음성으로 읽을 내용 (ADR-0007: 의미 값. 문장은 [VoiceGuide]의 render가 리소스로 조립한다). */
sealed interface Utterance {
    /** 첫 안내: 후보 이름을 번호 순서대로. */
    data class Recommend(val names: List<String>) : Utterance

    /** "다음" 안내: [ordinal]번 후보 한 개와 추천 이유. */
    data class Next(val ordinal: Int, val name: String, val reason: StopReason) : Utterance

    /** 경유지 추가 결과. */
    data class Result(val name: String, val ok: Boolean) : Utterance
}

/** 음성 실패 시 화면 한 줄 상태 (의미 값). */
sealed interface VoiceStatus {
    data class Candidates(val count: Int) : VoiceStatus
    data class NextCandidate(val ordinal: Int) : VoiceStatus
    data object Added : VoiceStatus
    data object AddFailed : VoiceStatus
}

/** 첫 안내: 후보 최대 2개를 번호로. 없으면 null(말하지 않음). */
fun recommendation(candidates: List<StopCandidate>): Utterance.Recommend? =
    candidates.take(SPOKEN_FIRST).map { it.place.name }.takeIf { it.isNotEmpty() }
        ?.let(Utterance::Recommend)

/** "다음" 안내: 3번째 후보 한 개. */
fun nextUtterance(candidate: StopCandidate) =
    Utterance.Next(SPOKEN_FIRST + 1, candidate.place.name, candidate.reason)

/**
 * 후보·결과를 [VoiceOutput]으로 안내한다. 음성 실패 시 같은 문장을 다시 읽지 않고
 * 한 줄 [status]만 남긴다(ADR-0002). 호출은 메인 스레드에서.
 */
class VoiceGuide(private val voice: VoiceOutput, private val render: (Utterance) -> String) {
    private var candidates by mutableStateOf<List<StopCandidate>>(emptyList())
    private var nextSpoken by mutableStateOf(false)
    private var request = 0 // 오래된 요청의 늦은 오류가 최신 상태를 덮지 않게 한다

    /** 음성 실패 시 화면에 띄울 한 줄. null = 표시 없음. */
    var status: VoiceStatus? by mutableStateOf(null)
        private set

    /** 3번째 후보가 있고 아직 읽지 않았을 때만 "다음" 버튼을 보인다. */
    val hasNext: Boolean
        get() = candidates.size > SPOKEN_FIRST && !nextSpoken

    fun announce(candidates: List<StopCandidate>) {
        this.candidates = candidates
        nextSpoken = false
        val spoken = recommendation(candidates)
        if (spoken == null) {
            request++
            status = null
            voice.stop()
        } else {
            say(spoken, VoiceStatus.Candidates(spoken.names.size))
        }
    }

    /** 읽기에 실패하면 버튼을 남겨 사용자가 직접 다시 누를 수 있다(자동 재시도 없음). */
    fun next() {
        if (!hasNext) return
        say(
            nextUtterance(candidates[SPOKEN_FIRST]),
            VoiceStatus.NextCandidate(SPOKEN_FIRST + 1),
            nextRead = true
        )
    }

    fun announceResult(place: StopPlace, ok: Boolean) =
        say(Utterance.Result(place.name, ok), if (ok) VoiceStatus.Added else VoiceStatus.AddFailed)

    fun stop() {
        request++
        nextSpoken = false // 읽다 끊긴 3번째 후보는 다시 누를 수 있다
        voice.stop()
    }

    private fun say(what: Utterance, onFail: VoiceStatus, nextRead: Boolean = false) {
        val id = ++request
        status = null
        nextSpoken = nextRead // 새 요청이 3번째 후보 낭독을 끊으면 버튼을 되살린다
        voice.speak(render(what)) {
            if (id == request) {
                nextSpoken = false
                status = onFail
            }
        }
    }
}
