package ai.familystop

import ai.familystop.core.LocationSource
import ai.familystop.core.NaviGateway
import ai.familystop.core.VehicleEnergySource
import ai.familystop.core.VoiceOutput
import ai.familystop.fake.FakeLocationSource
import ai.familystop.fake.FakeNaviGateway
import ai.familystop.fake.FakeVehicleEnergySource
import ai.familystop.fake.FakeVoiceOutput

/** Single wiring point: swap an implementation here, nowhere else. */
object AppGraph {
    var location: LocationSource = FakeLocationSource()
    var voice: VoiceOutput = FakeVoiceOutput()
    var navi: NaviGateway = FakeNaviGateway()
    var energy: VehicleEnergySource = FakeVehicleEnergySource()
}
