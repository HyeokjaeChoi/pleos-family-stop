package ai.familystop.feature.drive

import android.content.Context
import android.os.Handler
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
 * Any failure reports UNKNOWN, which keeps the driving restrictions.
 */
class CarUxMovementSource(private val context: Context) : MovementSignalSource {
    private var car: Any? = null
    private var manager: Any? = null
    private var listener: Any? = null
    private val main = Handler(Looper.getMainLooper())

    // ponytail: createCar blocks the caller; move to async Car.createCar(ctx, conn) if it janks.
    override fun start(onSignal: (MovementState, String) -> Unit) {
        fun emit(state: MovementState, reason: String) {
            main.post { onSignal(state, reason) }
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
        try {
            val carClass = Class.forName("android.car.Car")
            val c = carClass.getMethod("createCar", Context::class.java).invoke(null, context)
                ?: return emit(MovementState.UNKNOWN, "Car 서비스 없음")
            car = c
            val service = carClass.getField("CAR_UX_RESTRICTION_SERVICE").get(null) as String
            val m = carClass.getMethod("getCarManager", String::class.java).invoke(c, service)
                ?: return emit(MovementState.UNKNOWN, "CarUxRestrictionsManager 없음")
            manager = m
            val listenerClass =
                Class.forName(
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
        } catch (e: Exception) {
            emit(MovementState.UNKNOWN, "CarUx 사용 불가: ${e.javaClass.simpleName}")
        } catch (e: LinkageError) {
            emit(MovementState.UNKNOWN, "CarUx 사용 불가: ${e.javaClass.simpleName}")
        }
    }

    override fun stop() {
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
