package ai.familystop.fake

import ai.familystop.core.*

// Temporary implementations so screens can be built before the real ones land.

class FakeLocationSource(private val point: GeoPoint = GeoPoint(37.5665, 126.9780, 0)) : LocationSource {
    override fun start(onUpdate: (GeoPoint) -> Unit, onUnavailable: (String) -> Unit) = onUpdate(point)
    override fun stop() {}
}

class FakeVoiceOutput(private val log: (String) -> Unit = {}) : VoiceOutput {
    override fun speak(text: String, onError: (String) -> Unit) = log(text)
    override fun stop() {}
}

class FakeNaviGateway : NaviGateway {
    val waypoints = mutableListOf<StopPlace>()
    override fun addWaypoint(place: StopPlace, onResult: (Result<Unit>) -> Unit) =
        if (waypoints.size >= 2) onResult(Result.failure(IllegalStateException("waypoints full")))
        else { waypoints += place; onResult(Result.success(Unit)) }
    override fun removeWaypoint(place: StopPlace, onResult: (Result<Unit>) -> Unit) {
        waypoints.remove(place); onResult(Result.success(Unit))
    }
}

class FakeVehicleEnergySource(private val soc: Int? = 62) : VehicleEnergySource {
    override fun start(onSoc: (Int?) -> Unit) = onSoc(soc)
    override fun stop() {}
}
