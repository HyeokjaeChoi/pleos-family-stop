package ai.familystop.impl.pleos

import ai.familystop.core.EnergyState
import ai.familystop.core.VehicleEnergySource
import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.listener.EvBatteryStateOfChargeListener
import ai.pleos.playground.vehicle.listener.RangeRemainingListener
import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper

/** Valid percent or null (unknown is never shown as fact, PL-UJFQXQ). */
internal fun socOrNull(raw: Int?): Int? = raw?.takeIf { it in 0..100 }

/** Range in metres (VHAL RANGE_REMAINING) → whole km, or null when missing/negative. */
internal fun rangeKmOrNull(meters: Float?): Int? =
    meters?.takeIf { it.isFinite() && it >= 0f }?.let { Math.round(it / 1000f) }

/**
 * [VehicleEnergySource] on Pleos Vehicle (ADR-0001, #79): SOC by listener, charge switch
 * (`getEvChargeSwitchState`, no listener in SDK 2.3.2) by polling — both under `CAR_ENERGY`.
 * Range (Odometer) needs `CAR_DRIVE` (declared since #81); it is still read only when granted, so a
 * build or vehicle without it shows no range instead of crashing (rangeKm stays null).
 * The SDK has no time-to-full API, so none is reported. Results are merged and delivered on main.
 */
class PleosVehicleEnergySource(context: Context) : VehicleEnergySource {
    private val canReadRange = context.checkSelfPermission(CAR_DRIVE) ==
        PackageManager.PERMISSION_GRANTED
    private val main = Handler(Looper.getMainLooper())
    private val vehicle = Vehicle(context.applicationContext)
    private var session: Any? = null
    private var socListener: EvBatteryStateOfChargeListener? = null
    private var rangeListener: RangeRemainingListener? = null
    private var poll: Runnable? = null

    override fun start(onState: (EnergyState) -> Unit) {
        stop()
        val token = Any()
        session = token
        var state = EnergyState()

        // Drop results from a stopped/restarted session; emit only on change.
        fun update(f: (EnergyState) -> EnergyState) {
            main.post {
                if (session !== token) return@post
                val next = f(state)
                if (next != state) {
                    state = next
                    onState(next)
                }
            }
        }
        val soc = object : EvBatteryStateOfChargeListener {
            override fun onEvBatteryStateOfChargeUpdated(soc: Int?) =
                update { it.copy(socPercent = socOrNull(soc)) }
            override fun onFailed(e: Exception) = update { it.copy(socPercent = null) }
        }
        val range = object : RangeRemainingListener {
            override fun onRangeRemainingChanged(meters: Float?) =
                update { it.copy(rangeKm = rangeKmOrNull(meters)) }
            override fun onFailed(e: Exception) = update { it.copy(rangeKm = null) }
        }
        socListener = soc
        rangeListener = range
        vehicle.initialize()
        val battery = vehicle.getEvBattery()
        val odometer = vehicle.getOdometer()
        battery.registerEvBatteryStateOfCharge(soc)
        battery.getEvBatteryStateOfCharge(
            { v -> update { it.copy(socPercent = socOrNull(v)) } },
            { update { it.copy(socPercent = null) } }
        )
        if (canReadRange) {
            odometer.registerRangeRemainingListener(range)
            odometer.getRangeRemainingDistance(
                { v -> update { it.copy(rangeKm = rangeKmOrNull(v)) } },
                { update { it.copy(rangeKm = null) } }
            )
        } else {
            rangeListener = null
        }
        val tick = object : Runnable {
            override fun run() {
                if (session !== token) return
                // A missing permission must read as "unknown", not crash the app (PL-UJFQXQ).
                runCatching {
                    battery.getEvChargeSwitchState(
                        { v -> update { it.copy(charging = v) } },
                        { update { it.copy(charging = null) } }
                    )
                }.onFailure { update { it.copy(charging = null) } }
                main.postDelayed(this, POLL_MS)
            }
        }
        poll = tick
        main.post(tick)
    }

    override fun stop() {
        if (session == null) return
        session = null
        poll?.let { main.removeCallbacks(it) }
        poll = null
        socListener?.let { vehicle.getEvBattery().unregisterEvBatteryStateOfCharge(it) }
        rangeListener?.let { vehicle.getOdometer().unregisterRangeRemainingListener(it) }
        socListener = null
        rangeListener = null
        vehicle.release()
    }

    private companion object {
        const val POLL_MS = 3_000L
        const val CAR_DRIVE = "pleos.car.permission.CAR_DRIVE"
    }
}
