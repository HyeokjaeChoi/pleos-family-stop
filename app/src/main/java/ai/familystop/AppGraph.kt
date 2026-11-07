package ai.familystop

import ai.familystop.core.*
import ai.familystop.fake.*

/** Single wiring point: swap an implementation here, nowhere else. */
object AppGraph {
    var location: LocationSource = FakeLocationSource()
    var voice: VoiceOutput = FakeVoiceOutput()
    var navi: NaviGateway = FakeNaviGateway()
    var energy: VehicleEnergySource = FakeVehicleEnergySource()
}
