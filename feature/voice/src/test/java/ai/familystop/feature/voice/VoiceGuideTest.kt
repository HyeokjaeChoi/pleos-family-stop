package ai.familystop.feature.voice

import ai.familystop.core.DataSource
import ai.familystop.core.StopCandidate
import ai.familystop.core.StopNeed
import ai.familystop.core.StopPlace
import ai.familystop.core.StopReason
import ai.familystop.core.VoiceOutput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private class RecordingVoice(var fail: Boolean = false) : VoiceOutput {
    val spoken = mutableListOf<String>()
    var stops = 0

    override fun speak(text: String, onError: (String) -> Unit) {
        spoken += text
        if (fail) onError("x")
    }

    override fun stop() {
        stops++
    }
}

private fun cand(name: String) = StopCandidate(
    StopPlace(name, 0.0, 0.0),
    emptySet(),
    emptySet(),
    StopReason(listOf(StopNeed.MEAL), coversAll = false),
    5,
    1.0,
    DataSource.VIRTUAL
)

class VoiceGuideTest {
    private val three = listOf(cand("가"), cand("나"), cand("다"))

    @Test
    fun drivingReadsOnlyTwoNumberedCandidatesInOneOrTwoSentences() {
        val v = RecordingVoice()
        VoiceGuide(v).announce(three)
        assertEquals(listOf("정차 후보 2개예요. 1번 가, 2번 나."), v.spoken)
    }

    @Test
    fun nextReadsThirdOnceThenDisappears() {
        val v = RecordingVoice()
        val g = VoiceGuide(v)
        g.announce(three)
        assertTrue(g.hasNext)
        g.next()
        g.next()
        assertEquals("3번 다, 식사 해결.", v.spoken[1])
        assertEquals(2, v.spoken.size)
        assertFalse(g.hasNext)
    }

    @Test
    fun twoCandidatesHaveNoNext() {
        val g = VoiceGuide(RecordingVoice())
        g.announce(three.take(2))
        assertFalse(g.hasNext)
    }

    @Test
    fun failureShowsOneLineAndNeverRetries() {
        val v = RecordingVoice(fail = true)
        val g = VoiceGuide(v)
        g.announce(three)
        assertEquals("후보 2개 · 정차 후 확인", g.status)
        assertEquals(1, v.spoken.size)
    }

    @Test
    fun emptyIsSilentAndNewAnnounceClearsStatus() {
        val v = RecordingVoice(fail = true)
        val g = VoiceGuide(v)
        g.announce(three)
        v.fail = false
        g.announce(emptyList())
        assertNull(g.status)
        assertEquals(1, v.spoken.size)
        assertEquals(1, v.stops)
    }

    @Test
    fun resultAnnouncedAndFailureFallsBackToLine() {
        val v = RecordingVoice(fail = true)
        val g = VoiceGuide(v)
        g.announceResult(StopPlace("가", 0.0, 0.0), ok = true)
        assertEquals("경유지에 추가했어요. 가.", v.spoken.single())
        assertEquals("경유지 추가됨", g.status)
    }

    @Test
    fun failedNextKeepsButtonForManualRetry() {
        val v = RecordingVoice()
        val g = VoiceGuide(v)
        g.announce(three)
        v.fail = true
        g.next()
        assertEquals("3번 후보 · 정차 후 확인", g.status)
        assertTrue(g.hasNext)
    }

    @Test
    fun lateErrorOfOlderRequestIsIgnored() {
        val cbs = mutableListOf<(String) -> Unit>()
        val v = object : VoiceOutput {
            override fun speak(text: String, onError: (String) -> Unit) {
                cbs += onError
            }

            override fun stop() = Unit
        }
        val g = VoiceGuide(v)
        g.announce(three)
        g.announceResult(StopPlace("가", 0.0, 0.0), ok = true)
        cbs[0]("late")
        assertNull(g.status)
    }

    @Test
    fun interruptedNextReadCanBeRequestedAgain() {
        val g = VoiceGuide(RecordingVoice())
        g.announce(three)
        g.next()
        assertFalse(g.hasNext)
        g.announceResult(StopPlace("가", 0.0, 0.0), ok = true)
        assertTrue(g.hasNext)
        g.next()
        g.stop()
        assertTrue(g.hasNext)
    }
}
