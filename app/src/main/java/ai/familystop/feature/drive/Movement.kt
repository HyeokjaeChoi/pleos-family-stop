package ai.familystop.feature.drive

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import java.lang.reflect.Proxy

enum class MovementState { DRIVING, PARKED, UNKNOWN }

/** Confirmed state + evidence + confirmation time (F-GNGJMP). UNKNOWN is restricted like DRIVING (ADR-0004). */
data class MovementReading(val state: MovementState, val reason: String, val sinceMs: Long) {
    val restricted get() = state != MovementState.PARKED
}

/** Raw movement signal from the platform. Impl: [CarUxMovementSource] now, Pleos driving mode after #1. */
interface MovementSignalSource {
    fun start(onSignal: (MovementState, String) -> Unit)
    fun stop()
}

/**
 * Debounce: becoming PARKED needs [holdMs] of uninterrupted PARKED signals; until then the
 * restrictive state stays. Any non-PARKED signal applies at once.
 */
class MovementTracker(private val holdMs: Long = 2_000) {
    var reading = MovementReading(MovementState.UNKNOWN, "신호 없음", 0)
        private set
    private var parkedSinceMs: Long? = null
    private var parkedReason = ""

    fun onSignal(state: MovementState, reason: String, nowMs: Long): MovementReading {
        if (state != MovementState.PARKED) {
            parkedSinceMs = null
            reading = if (state == reading.state) {
                reading.copy(reason = reason)
            } else {
                MovementReading(state, reason, nowMs)
            }
        } else if (reading.state != MovementState.PARKED) {
            if (parkedSinceMs == null) parkedSinceMs = nowMs
            parkedReason = reason
        }
        return tick(nowMs)
    }

    fun tick(nowMs: Long): MovementReading {
        val since = parkedSinceMs
        if (since != null && nowMs - since >= holdMs) {
            reading = MovementReading(MovementState.PARKED, parkedReason, nowMs)
            parkedSinceMs = null
        }
        return reading
    }
}

/**
 * `CarUxRestrictionsManager` via reflection (android.car is not on the compile classpath).
 * Connects asynchronously; until connected (and on any failure) the screen stays restricted
 * because the tracker starts at UNKNOWN.
 */
class CarUxMovementSource(private val context: Context) : MovementSignalSource {
    private var car: Any? = null
    private var manager: Any? = null
    private var listener: Any? = null
    private var stopped = true
    private val main = Handler(Looper.getMainLooper())

    override fun start(onSignal: (MovementState, String) -> Unit) {
        stopped = false
        fun emit(state: MovementState, reason: String) {
            main.post { if (!stopped) onSignal(state, reason) }
        }
        fun emitRestrictions(r: Any) {
            val restricted = r.javaClass.getMethod(
                "isRequiresDistractionOptimization"
            ).invoke(r) as Boolean
            emit(
                if (restricted) MovementState.DRIVING else MovementState.PARKED,
                "CarUxRestrictions requiresDistractionOptimization=$restricted"
            )
        }
        fun attach(carClass: Class<*>, c: Any) {
            val service = carClass.getField("CAR_UX_RESTRICTION_SERVICE").get(null) as String
            val m = carClass.getMethod("getCarManager", String::class.java).invoke(c, service)
                ?: return emit(MovementState.UNKNOWN, "CarUxRestrictionsManager 없음")
            manager = m
            val listenerClass = Class.forName(
                "android.car.drivingstate.CarUxRestrictionsManager\$OnUxRestrictionsChangedListener"
            )
            val l = Proxy.newProxyInstance(listenerClass.classLoader, arrayOf(listenerClass)) {
                    proxy,
                    method,
                    args
                ->
                when (method.name) {
                    "onUxRestrictionsChanged" -> runCatching { emitRestrictions(args[0]) }
                        .onFailure { emit(MovementState.UNKNOWN, "CarUx 값 읽기 실패") }
                    "hashCode" -> return@newProxyInstance System.identityHashCode(proxy)
                    "equals" -> return@newProxyInstance proxy === args[0]
                    "toString" -> return@newProxyInstance "UxListener"
                }
                null
            }
            listener = l
            m.javaClass.getMethod("registerListener", listenerClass).invoke(m, l)
            m.javaClass.getMethod("getCurrentCarUxRestrictions").invoke(m)?.let(::emitRestrictions)
                ?: emit(MovementState.UNKNOWN, "현재 제한 값 없음")
        }
        try {
            val carClass = Class.forName("android.car.Car")
            val conn = object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                    val c = car
                    if (stopped || c == null) return
                    try {
                        attach(carClass, c)
                    } catch (e: Exception) {
                        emit(MovementState.UNKNOWN, "CarUx 사용 불가: ${e.javaClass.simpleName}")
                    }
                }

                override fun onServiceDisconnected(name: ComponentName?) {
                    emit(MovementState.UNKNOWN, "Car 서비스 연결 끊김")
                }
            }
            car =
                carClass.getMethod("createCar", Context::class.java, ServiceConnection::class.java)
                    .invoke(null, context, conn)
            carClass.getMethod("connect").invoke(car)
        } catch (e: Exception) {
            emit(MovementState.UNKNOWN, "CarUx 사용 불가: ${e.javaClass.simpleName}")
        } catch (e: LinkageError) {
            emit(MovementState.UNKNOWN, "CarUx 사용 불가: ${e.javaClass.simpleName}")
        }
    }

    override fun stop() {
        stopped = true
        runCatching {
            val m = manager
            val l = listener
            if (m != null && l != null) {
                m.javaClass.methods.first { it.name == "unregisterListener" }.let {
                    if (it.parameterCount == 0) it.invoke(m) else it.invoke(m, l)
                }
            }
        }
        runCatching { car?.let { it.javaClass.getMethod("disconnect").invoke(it) } }
        main.removeCallbacksAndMessages(null)
        car = null
        manager = null
        listener = null
    }
}
