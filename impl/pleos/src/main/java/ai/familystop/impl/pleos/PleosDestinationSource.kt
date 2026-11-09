package ai.familystop.impl.pleos

import ai.familystop.core.Destination
import ai.familystop.core.DestinationKind
import ai.familystop.core.DestinationSource
import ai.familystop.core.RouteEta
import ai.pleos.playground.navi.constants.BookmarkType
import ai.pleos.playground.navi.constants.NaviErrorCode
import ai.pleos.playground.navi.data.BookmarkInfo
import ai.pleos.playground.navi.data.DestinationInfo
import ai.pleos.playground.navi.data.RecentDestinationInfo
import ai.pleos.playground.navi.helper.listener.NaviHelperEventListener
import android.os.Handler
import android.os.Looper

/**
 * Drops unset entries (blank name or 0,0: measured on the emulator, HOME/OFFICE come back empty),
 * removes duplicates and puts saved HOME/OFFICE before recents. Order of recents is kept.
 */
internal fun cleanDestinations(all: List<Destination>): List<Destination> =
    all.filter { it.name.isNotBlank() && !(it.lat == 0.0 && it.lon == 0.0) }
        .distinctBy { Triple(it.name, it.lat, it.lon) }
        // Stable sort: bookmarks (HOME, OFFICE) before recents, recents keep their order.
        .sortedBy { it.kind.ordinal }

/** [DestinationSource] on Pleos NaviHelper recent/bookmark/destination info (#65). Main thread only. */
class PleosDestinationSource(private val session: NaviSession) : DestinationSource {
    private val main = Handler(Looper.getMainLooper())
    private val helper get() = session.helper
    private var listening = false
    private var recent: List<Destination>? = null
    private var bookmarks: List<Destination>? = null
    private var loadCb: ((Result<List<Destination>>) -> Unit)? = null
    private var etaCb: ((Result<RouteEta?>) -> Unit)? = null
    private val loadTimeout = Runnable { deliverLoad(force = true) }
    private val etaTimeout =
        Runnable { finishEta(Result.failure(IllegalStateException("eta timeout"))) }

    private val listener = object : NaviHelperEventListener {
        override fun onRecentDestinationInfo(list: List<RecentDestinationInfo>) {
            val mapped = list.map {
                Destination(it.name, it.latitude, it.longitude, DestinationKind.RECENT, it.address)
            }
            main.post {
                recent = mapped
                deliverLoad(force = false)
            }
        }

        override fun onBookmarkInfo(list: List<BookmarkInfo>) {
            val mapped = list.mapNotNull {
                val kind = when (it.category) {
                    BookmarkType.HOME -> DestinationKind.HOME
                    BookmarkType.OFFICE -> DestinationKind.OFFICE
                    else -> null
                }
                kind?.let { k -> Destination(it.name, it.latitude, it.longitude, k, it.address) }
            }
            main.post {
                bookmarks = mapped
                deliverLoad(force = false)
            }
        }

        override fun onDestinationInfo(info: DestinationInfo) {
            // remainTime is seconds (emulator: 6180 for a ~1h43m route).
            main.post {
                finishEta(
                    Result.success(
                        RouteEta(info.name, info.latitude, info.longitude, info.remainTime)
                    )
                )
            }
        }

        override fun onError(code: NaviErrorCode) {
            main.post {
                loadCb?.let { cb ->
                    loadCb = null
                    main.removeCallbacks(loadTimeout)
                    cb(Result.failure(IllegalStateException("navi error: $code")))
                }
                finishEta(Result.failure(IllegalStateException("navi error: $code")))
            }
        }
    }

    override fun load(onResult: (Result<List<Destination>>) -> Unit) {
        loadCb = onResult
        recent = null
        bookmarks = null
        main.removeCallbacks(loadTimeout)
        main.postDelayed(loadTimeout, TIMEOUT_MS)
        whenConnected {
            helper.getRecentDestinationInfo()
            helper.getBookmarkInfo()
        }
    }

    override fun currentEta(onResult: (Result<RouteEta?>) -> Unit) {
        etaCb = onResult
        main.removeCallbacks(etaTimeout)
        main.postDelayed(etaTimeout, TIMEOUT_MS)
        whenConnected { helper.getDestinationInfo() }
    }

    private fun deliverLoad(force: Boolean) {
        val cb = loadCb ?: return
        val r = recent
        val b = bookmarks
        if (!force && (r == null || b == null)) return
        // On timeout a lone list is still useful; with nothing at all it is a failure.
        loadCb = null
        main.removeCallbacks(loadTimeout)
        if (r == null && b == null) {
            cb(Result.failure(IllegalStateException("navi destinations timeout")))
        } else {
            cb(Result.success(cleanDestinations(b.orEmpty() + r.orEmpty())))
        }
    }

    private fun finishEta(result: Result<RouteEta?>) {
        val cb = etaCb ?: return
        etaCb = null
        main.removeCallbacks(etaTimeout)
        cb(result)
    }

    private fun whenConnected(request: () -> Unit) {
        session.whenReady(
            {
                if (!listening) {
                    listening = true
                    helper.addListener(listener)
                }
                request()
            },
            {
                val e = it
                loadCb?.let { cb ->
                    loadCb = null
                    main.removeCallbacks(loadTimeout)
                    cb(Result.failure(e))
                }
                finishEta(Result.failure(e))
            }
        )
    }

    private companion object {
        const val TIMEOUT_MS = 8_000L
    }
}
