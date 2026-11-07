package ai.familystop.impl.pleos

import ai.familystop.core.VehicleEnergySource
import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.listener.EvBatteryStateOfChargeListener
import android.content.Context

/** Valid percent or null (unknown is never shown as fact, PL-UJFQXQ). */
internal fun socOrNull(raw: Int?): Int? = raw?.takeIf { it in 0..100 }

/** [VehicleEnergySource] on Pleos Vehicle `EvBatteryStateOfChargeListener` (ADR-0001). */
class PleosVehicleEnergySource(context: Context) : VehicleEnergySource {
    private val vehicle = Vehicle(context)
    private var listener: EvBatteryStateOfChargeListener? = null

    override fun start(onSoc: (Int?) -> Unit) {
        stop()
        val l = object : EvBatteryStateOfChargeListener {
            override fun onEvBatteryStateOfChargeUpdated(soc: Int?) = onSoc(socOrNull(soc))
            override fun onFailed(e: Exception) = onSoc(null)
        }
        listener = l
        vehicle.initialize()
        vehicle.getEvBattery().registerEvBatteryStateOfCharge(l)
        vehicle.getEvBattery().getEvBatteryStateOfCharge({ onSoc(socOrNull(it)) }, { onSoc(null) })
    }

    override fun stop() {
        val l = listener ?: return
        listener = null
        vehicle.getEvBattery().unregisterEvBatteryStateOfCharge(l)
        vehicle.release()
    }
}
