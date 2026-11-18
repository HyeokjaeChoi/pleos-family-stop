package ai.familystop.impl.platform

import android.location.LocationManager
import android.speech.tts.TextToSpeech
import java.util.Locale
import org.junit.Assert.assertEquals
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
    fun languageFallsBackToKoreanThenFails() {
        val ok = TextToSpeech.LANG_AVAILABLE
        val none = TextToSpeech.LANG_MISSING_DATA
        assertEquals(Locale.ENGLISH, pickLanguage(Locale.ENGLISH) { ok })
        assertEquals(
            Locale.KOREAN,
            pickLanguage(Locale.ENGLISH) {
                if (it ==
                    Locale.KOREAN
                ) {
                    ok
                } else {
                    none
                }
            }
        )
        assertNull(pickLanguage(Locale.ENGLISH) { TextToSpeech.LANG_NOT_SUPPORTED })
        val tried = mutableListOf<Locale>()
        assertNull(
            pickLanguage(Locale.KOREA) {
                tried += it
                none
            }
        )
        assertEquals(listOf(Locale.KOREA), tried)
    }

    @Test
    fun staleUtteranceDoesNotOwnFocus() {
        assertEquals(true, isCurrentUtterance("p-3", 3, "p-"))
        assertEquals(false, isCurrentUtterance("p-2", 3, "p-"))
        assertEquals(false, isCurrentUtterance(null, 3, "p-"))
    }

    @Test
    fun gpsNeedsFineButFusedAcceptsCoarse() {
        assertEquals(true, permitted(LocationManager.GPS_PROVIDER, fine = true, coarse = false))
        assertEquals(false, permitted(LocationManager.GPS_PROVIDER, fine = false, coarse = true))
        assertEquals(true, permitted(LocationManager.FUSED_PROVIDER, fine = false, coarse = true))
        assertEquals(false, permitted(LocationManager.FUSED_PROVIDER, fine = false, coarse = false))
    }
}
