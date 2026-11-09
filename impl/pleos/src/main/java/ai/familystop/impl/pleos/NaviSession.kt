package ai.familystop.impl.pleos

import ai.pleos.playground.navi.helper.NaviHelper
import android.content.Context
import android.os.Handler
import android.os.Looper

/**
 * One [NaviHelper] for the whole app. The Navi service keeps a single callback per appId, so a second helper
 * silently replaces the first one's callback (#69: waypoint replies never reached the gateway).
 * Main thread only; app-scoped.
 */
class NaviSession(context: Context) {
    val helper = NaviHelper(context.applicationContext)
    private val main = Handler(Looper.getMainLooper())
    private var connected = false
    private var connecting = false
    private val waiting = mutableListOf<Pair<() -> Unit, (Throwable) -> Unit>>()

    /** Runs [onReady] once connected (immediately if already). A failed init fails all waiters; the next call retries. */
    fun whenReady(onReady: () -> Unit, onFail: (Throwable) -> Unit) {
        if (connected) return onReady()
        waiting += onReady to onFail
        if (connecting) return
        connecting = true
        helper.initialize(
            {
                main.post {
                    connecting = false
                    connected = true
                    flush { it.first() }
                }
            },
            {
                main.post {
                    connecting = false
                    val e = IllegalStateException("navi init failed: ${it.message}")
                    flush { w -> w.second(e) }
                }
            }
        )
    }

    private fun flush(call: (Pair<() -> Unit, (Throwable) -> Unit>) -> Unit) =
        waiting.toList().also { waiting.clear() }.forEach(call)
}
