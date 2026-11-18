package ai.familystop.feature.voice

import ai.familystop.core.DataSource
import ai.familystop.core.StopCandidate
import ai.familystop.core.StopNeed
import ai.familystop.core.StopPlace
import ai.familystop.core.StopReason
import ai.familystop.core.VoiceError
import ai.familystop.core.VoiceOutput
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private class RecordingVoice(var fail: Boolean = false) : VoiceOutput {
    val spoken = mutableListOf<String>()
    var stops = 0

    override fun speak(locale: Locale, text: (Locale) -> String, onError: (VoiceError) -> Unit) {
        spoken += text(locale)
        if (fail) onError(VoiceError.PLAYBACK)
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
    private val said = mutableListOf<Utterance>()
    private fun guide(v: VoiceOutput) = VoiceGuide(v, { Locale.KOREAN }) { u, _ ->
        said += u
        u.toString()
    }
    private val three = listOf(cand("가"), cand("나"), cand("다"))

    @Test
    fun drivingReadsOnlyTwoNumberedCandidatesInOneOrTwoSentences() {
        val v = RecordingVoice()
        guide(v).announce(three)
        assertEquals(listOf(Utterance.Recommend(listOf("가", "나"))), said)
        assertEquals(1, v.spoken.size)
    }

    @Test
    fun nextReadsThirdOnceThenDisappears() {
        val v = RecordingVoice()
        val g = guide(v)
        g.announce(three)
        assertTrue(g.hasNext)
        g.next()
        g.next()
        assertEquals(
            Utterance.Next(3, "다", StopReason(listOf(StopNeed.MEAL), coversAll = false)),
            said[1]
        )
        assertEquals(2, v.spoken.size)
        assertFalse(g.hasNext)
    }

    @Test
    fun twoCandidatesHaveNoNext() {
        val g = guide(RecordingVoice())
        g.announce(three.take(2))
        assertFalse(g.hasNext)
    }

    @Test
    fun failureShowsOneLineAndNeverRetries() {
        val v = RecordingVoice(fail = true)
        val g = guide(v)
        g.announce(three)
        assertEquals(VoiceStatus.Candidates(2), g.status)
        assertEquals(1, v.spoken.size)
    }

    @Test
    fun emptyIsSilentAndNewAnnounceClearsStatus() {
        val v = RecordingVoice(fail = true)
        val g = guide(v)
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
        val g = guide(v)
        g.announceResult(StopPlace("가", 0.0, 0.0), ok = true)
        assertEquals(Utterance.Result("가", true), said.single())
        assertEquals(VoiceStatus.Added, g.status)
    }

    @Test
    fun failedNextKeepsButtonForManualRetry() {
        val v = RecordingVoice()
        val g = guide(v)
        g.announce(three)
        v.fail = true
        g.next()
        assertEquals(VoiceStatus.NextCandidate(3), g.status)
        assertTrue(g.hasNext)
    }

    @Test
    fun lateErrorOfOlderRequestIsIgnored() {
        val cbs = mutableListOf<(VoiceError) -> Unit>()
        val v = object : VoiceOutput {
            override fun speak(
                locale: Locale,
                text: (Locale) -> String,
                onError: (VoiceError) -> Unit
            ) {
                cbs += onError
            }

            override fun stop() = Unit
        }
        val g = guide(v)
        g.announce(three)
        g.announceResult(StopPlace("가", 0.0, 0.0), ok = true)
        cbs[0](VoiceError.PLAYBACK)
        assertNull(g.status)
    }

    @Test
    fun interruptedNextReadCanBeRequestedAgain() {
        val g = guide(RecordingVoice())
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
