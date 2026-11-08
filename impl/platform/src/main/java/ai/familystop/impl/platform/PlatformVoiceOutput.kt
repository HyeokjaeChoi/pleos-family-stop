package ai.familystop.impl.platform

import ai.familystop.core.VoiceOutput
import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

/** Error text for a failed TTS init/language setup, null = ready. */
internal fun ttsInitError(status: Int, langResult: Int): String? = when {
    status != TextToSpeech.SUCCESS -> "음성 엔진을 시작하지 못했습니다"
    langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED ->
        "한국어 음성을 사용할 수 없습니다"
    else -> null
}

/** [VoiceOutput] on Android TextToSpeech; swapped for Pleos TTS ON_DEVICE when it opens (ADR-0002). */
class PlatformVoiceOutput(context: Context) : VoiceOutput {
    private val appContext = context.applicationContext
    private val lock = Any()
    private var tts: TextToSpeech? = null
    private var ready = false
    private var initError: String? = null
    private var pending: Pair<String, (String) -> Unit>? = null
    private var errorCb: (String) -> Unit = {}

    override fun speak(text: String, onError: (String) -> Unit) {
        val failure: String?
        synchronized(lock) {
            errorCb = onError
            if (tts == null) init()
            failure = initError
            if (failure == null && !ready) {
                pending = text to onError
                return
            }
        }
        if (failure != null) onError(failure) else say(text)
    }

    override fun stop() {
        synchronized(lock) { pending = null }
        tts?.stop()
    }

    private fun init() {
        tts = TextToSpeech(appContext) { status ->
            val t = tts
            val error = ttsInitError(status, t?.setLanguage(Locale.KOREAN) ?: TextToSpeech.ERROR)
            val queued: Pair<String, (String) -> Unit>?
            synchronized(lock) {
                if (error == null) ready = true else initError = error
                queued = pending
                pending = null
            }
            if (queued != null) if (error == null) say(queued.first) else queued.second(error)
        }.also {
            it.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) = Unit
                override fun onDone(utteranceId: String?) = Unit

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) = errorCb("음성 안내를 재생하지 못했습니다")
            })
        }
    }

    private fun say(text: String) {
        if (tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "stop-notice") == TextToSpeech.ERROR) {
            errorCb("음성 안내를 재생하지 못했습니다")
        }
    }
}
