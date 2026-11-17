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
    fun gpsNeedsFineButFusedAcceptsCoarse() {
        assertEquals(true, permitted(LocationManager.GPS_PROVIDER, fine = true, coarse = false))
        assertEquals(false, permitted(LocationManager.GPS_PROVIDER, fine = false, coarse = true))
        assertEquals(true, permitted(LocationManager.FUSED_PROVIDER, fine = false, coarse = true))
        assertEquals(false, permitted(LocationManager.FUSED_PROVIDER, fine = false, coarse = false))
    }
}
