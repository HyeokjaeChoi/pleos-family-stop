package ai.familystop.feature.voice

import ai.familystop.core.StopCandidate
import ai.familystop.core.StopPlace
import ai.familystop.core.VoiceOutput
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** 주행 중 첫 안내에서 번호로 읽는 후보 수 (ADR-0002·0003). 3번째는 "다음" 요청 시에만. */
const val SPOKEN_FIRST = 2

/** 첫 안내: 1~2문장, 후보 최대 2개를 번호로. 없으면 null(말하지 않음). */
fun recommendationText(candidates: List<StopCandidate>): String? {
    val spoken = candidates.take(SPOKEN_FIRST)
    if (spoken.isEmpty()) return null
    val list = spoken.mapIndexed { i, c -> "${i + 1}번 ${c.place.name}" }.joinToString(", ")
    return "정차 후보 ${spoken.size}개예요. $list."
}

/** "다음" 안내: 3번째 후보 한 개. */
fun nextText(candidate: StopCandidate): String =
    "${SPOKEN_FIRST + 1}번 ${candidate.place.name}, ${candidate.reason}."

/** 경유지 추가 결과 안내. */
fun resultText(place: StopPlace, ok: Boolean): String =
    if (ok) "경유지에 추가했어요. ${place.name}." else "경유지를 추가하지 못했어요. 정차 후 다시 확인해 주세요."

/**
 * 후보·결과를 [VoiceOutput]으로 안내한다. 음성 실패 시 같은 문장을 다시 읽지 않고
 * 한 줄 [status]만 남긴다(ADR-0002). 호출은 메인 스레드에서.
 */
class VoiceGuide(private val voice: VoiceOutput) {
    private var candidates: List<StopCandidate> = emptyList()
    private var nextSpoken = false

    /** 음성 실패 시 화면에 띄울 한 줄. null = 표시 없음. */
    var status: String? by mutableStateOf(null)
        private set

    /** 3번째 후보가 있고 아직 읽지 않았을 때만 "다음" 버튼을 보인다. */
    val hasNext: Boolean
        get() = candidates.size > SPOKEN_FIRST && !nextSpoken

    fun announce(candidates: List<StopCandidate>) {
        this.candidates = candidates
        nextSpoken = false
        status = null
        val text = recommendationText(candidates)
        if (text ==
            null
        ) {
            voice.stop()
        } else {
            say(text, "후보 ${candidates.take(SPOKEN_FIRST).size}개 · 정차 후 확인")
        }
    }

    fun next() {
        if (!hasNext) return
        nextSpoken = true
        say(nextText(candidates[SPOKEN_FIRST]), "${SPOKEN_FIRST + 1}번 후보 · 정차 후 확인")
    }

    fun announceResult(place: StopPlace, ok: Boolean) {
        status = null
        say(resultText(place, ok), if (ok) "경유지 추가됨" else "경유지 추가 실패 · 정차 후 확인")
    }

    fun stop() = voice.stop()

    private fun say(text: String, onFail: String) = voice.speak(text) { status = onFail }
}
