package ai.familystop.impl.platform

import ai.familystop.core.VoiceOutput
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale

/** Error text for a failed TTS init/language setup, null = ready. */
internal fun ttsInitError(status: Int, langResult: Int): String? = when {
    status != TextToSpeech.SUCCESS -> "음성 엔진을 시작하지 못했습니다"
    langResult < TextToSpeech.LANG_AVAILABLE -> "한국어 음성을 사용할 수 없습니다"
    else -> null
}

/**
 * [VoiceOutput] on Android TextToSpeech; swapped for Pleos TTS ON_DEVICE when it opens (ADR-0002).
 * Call from the main thread; every `onError` is delivered on the main thread.
 */
class PlatformVoiceOutput(context: Context) : VoiceOutput {
    private val appContext = context.applicationContext
    private val main = Handler(Looper.getMainLooper())
    private var tts: TextToSpeech? = null
    private var ready = false
    private var pending: String? = null
    private var errorCb: (String) -> Unit = {}

    // Bumped by every speak/stop/close; errors of an older request are dropped.
    private var gen = 0

    override fun speak(text: String, onError: (String) -> Unit) {
        gen++
        errorCb = onError
        if (ready) {
            say(text)
        } else {
            pending = text
            if (tts == null) init()
        }
    }

    override fun stop() {
        gen++
        pending = null
        tts?.stop()
    }

    /** Releases the engine; the next [speak] starts a fresh one. */
    fun close() {
        gen++
        pending = null
        ready = false
        tts?.shutdown()
        tts = null
    }

    private fun init() {
        lateinit var engine: TextToSpeech
        engine = TextToSpeech(appContext) { status ->
            // Posted so `engine` is assigned even if init fails synchronously.
            main.post { onInit(engine, status) }
        }
        tts = engine
        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = Unit
            override fun onDone(utteranceId: String?) = Unit

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) = onError(utteranceId, -1)

            override fun onError(utteranceId: String?, errorCode: Int) {
                Log.w(TAG, "utterance error code=$errorCode (TextToSpeech.ERROR_*)")
                fail(utteranceId?.removePrefix(ID_PREFIX)?.toIntOrNull(), "음성 안내를 재생하지 못했습니다")
            }
        })
    }

    private fun onInit(engine: TextToSpeech, status: Int) {
        if (engine !== tts) return
        val lang = if (status ==
            TextToSpeech.SUCCESS
        ) {
            engine.setLanguage(Locale.KOREAN)
        } else {
            TextToSpeech.ERROR
        }
        val error = ttsInitError(status, lang)
        Log.i(TAG, "init status=$status lang=$lang engine=${engine.defaultEngine} error=$error")
        if (error != null) {
            val g = gen
            val wanted = pending != null
            close() // so a later speak() retries (e.g. voice data installed meanwhile)
            gen = g // close() bumped it; this failure still belongs to the pending request
            if (wanted) fail(g, error)
            return
        }
        ready = true
        pending?.let { say(it) }
        pending = null
    }

    private fun say(text: String) {
        if (tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "$ID_PREFIX$gen") ==
            TextToSpeech.ERROR
        ) {
            fail(gen, "음성 안내를 재생하지 못했습니다")
        }
    }

    private fun fail(requestGen: Int?, message: String) {
        val cb = errorCb
        main.post { if (requestGen == gen) cb(message) }
    }

    private companion object {
        const val TAG = "FamilyStopTts"
        const val ID_PREFIX = "stop-notice-"
    }
}
