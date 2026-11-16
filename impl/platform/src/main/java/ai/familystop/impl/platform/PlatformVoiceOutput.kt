package ai.familystop.impl.platform

import ai.familystop.core.VoiceOutput
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
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

/** True if the utterance belongs to the current request (a flushed older one must not drop focus). */
internal fun isCurrentUtterance(utteranceId: String?, gen: Int, prefix: String): Boolean =
    utteranceId?.removePrefix(prefix)?.toIntOrNull() == gen

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
    private val audio = appContext.getSystemService(AudioManager::class.java)
    private val attrs = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
        .build()
    private var focus: AudioFocusRequest? = null
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
        abandonFocus()
    }

    /** Releases the engine; the next [speak] starts a fresh one. */
    fun close() {
        gen++
        pending = null
        ready = false
        abandonFocus()
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
            override fun onDone(utteranceId: String?) = release(utteranceId)

            override fun onStop(utteranceId: String?, interrupted: Boolean) = release(utteranceId)

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) = onError(utteranceId, -1)

            override fun onError(utteranceId: String?, errorCode: Int) {
                if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "utterance error code=$errorCode")
                release(utteranceId)
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
        if (Log.isLoggable(
                TAG,
                Log.DEBUG
            )
        ) {
            Log.d(
                TAG,
                "init status=$status lang=$lang engine=${engine.defaultEngine} error=$error"
            )
        }
        if (error != null) {
            val g = gen
            val wanted = pending != null
            close() // so a later speak() retries (e.g. voice data installed meanwhile)
            gen = g // close() bumped it; this failure still belongs to the pending request
            if (wanted) fail(g, error)
            return
        }
        engine.setAudioAttributes(attrs)
        ready = true
        pending?.let { say(it) }
        pending = null
    }

    private fun say(text: String) {
        // Speak even if focus is denied: guidance is safety-relevant.
        if (focus == null) {
            val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(attrs)
                .build()
            focus = req
            audio.requestAudioFocus(req)
        }
        if (tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "$ID_PREFIX$gen") ==
            TextToSpeech.ERROR
        ) {
            fail(gen, "음성 안내를 재생하지 못했습니다")
        }
    }

    // Callbacks arrive on a binder thread; hop to main before touching state.
    private fun release(utteranceId: String?) {
        main.post { if (isCurrentUtterance(utteranceId, gen, ID_PREFIX)) abandonFocus() }
    }

    private fun abandonFocus() {
        focus?.let { audio.abandonAudioFocusRequest(it) }
        focus = null
    }

    private fun fail(requestGen: Int?, message: String) {
        val cb = errorCb
        main.post { if (requestGen == gen) cb(message) }
    }

    private companion object {
        // Off by default: adb shell setprop log.tag.FamilyStopTts DEBUG
        const val TAG = "FamilyStopTts"
        const val ID_PREFIX = "stop-notice-"
    }
}
