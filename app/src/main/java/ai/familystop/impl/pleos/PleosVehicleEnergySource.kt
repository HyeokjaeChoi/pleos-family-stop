package ai.familystop.impl.pleos

import ai.familystop.core.VehicleEnergySource
import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.listener.EvBatteryStateOfChargeListener
import android.content.Context
import android.os.Handler
import android.os.Looper

/** Valid percent or null (unknown is never shown as fact, PL-UJFQXQ). */
internal fun socOrNull(raw: Int?): Int? = raw?.takeIf { it in 0..100 }

/** [VehicleEnergySource] on Pleos Vehicle `EvBatteryStateOfChargeListener` (ADR-0001). `onSoc` runs on main. */
class PleosVehicleEnergySource(context: Context) : VehicleEnergySource {
    private val main = Handler(Looper.getMainLooper())
    private val vehicle = Vehicle(context.applicationContext)
    private var listener: EvBatteryStateOfChargeListener? = null

    override fun start(onSoc: (Int?) -> Unit) {
        stop()
        lateinit var l: EvBatteryStateOfChargeListener

        // Drop results from a stopped/restarted session.
        fun deliver(raw: Int?) {
            main.post { if (listener === l) onSoc(socOrNull(raw)) }
        }
        l = object : EvBatteryStateOfChargeListener {
            override fun onEvBatteryStateOfChargeUpdated(soc: Int?) {
                deliver(soc)
            }
            override fun onFailed(e: Exception) {
                deliver(null)
            }
        }
        listener = l
        vehicle.initialize()
        vehicle.getEvBattery().registerEvBatteryStateOfCharge(l)
        vehicle.getEvBattery().getEvBatteryStateOfCharge({ deliver(it) }, { deliver(null) })
    }

    override fun stop() {
        val l = listener ?: return
        listener = null
        vehicle.getEvBattery().unregisterEvBatteryStateOfCharge(l)
        vehicle.release()
    }
}
