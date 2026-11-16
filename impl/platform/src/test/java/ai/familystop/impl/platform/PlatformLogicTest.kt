package ai.familystop.impl.platform

import android.location.LocationManager
import android.speech.tts.TextToSpeech
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class PlatformLogicTest {
    private val all: (String) -> Boolean = { true }

    @Test
    fun providerFusedOnApi31ElseGps() {
        assertEquals(LocationManager.FUSED_PROVIDER, pickProvider(31, all))
        assertEquals(LocationManager.GPS_PROVIDER, pickProvider(30, all))
        assertEquals(
            LocationManager.GPS_PROVIDER,
            pickProvider(34) {
                it ==
                    LocationManager.GPS_PROVIDER
            }
        )
        assertNull(pickProvider(34) { false })
    }

    @Test
    fun ttsInitErrors() {
        assertNull(ttsInitError(TextToSpeech.SUCCESS, TextToSpeech.LANG_AVAILABLE))
        assertNotNull(ttsInitError(TextToSpeech.ERROR, TextToSpeech.LANG_AVAILABLE))
        assertNotNull(ttsInitError(TextToSpeech.SUCCESS, TextToSpeech.LANG_MISSING_DATA))
        assertNotNull(ttsInitError(TextToSpeech.SUCCESS, TextToSpeech.ERROR))
    }

    @Test
    fun staleUtteranceDoesNotOwnFocus() {
        assertEquals(true, isCurrentUtterance("p-3", 3, "p-"))
        assertEquals(false, isCurrentUtterance("p-2", 3, "p-"))
        assertEquals(false, isCurrentUtterance(null, 3, "p-"))
    }
}
