package ai.familystop.core

// Swappable seams (ADR-0001, ADR-0002). Each Pleos SDK that is still "준비 중"
// gets a platform/fake implementation now and a Pleos one when it opens.

data class GeoPoint(val lat: Double, val lon: Double, val timeMs: Long)

data class StopPlace(val name: String, val lat: Double, val lon: Double, val poiId: String? = null)

/** Current position. Impl: platform LocationManager now, Pleos FusedLocation later. */
interface LocationSource {
    fun start(onUpdate: (GeoPoint) -> Unit, onUnavailable: (String) -> Unit)
    fun stop()
}

/** Short spoken notices (1~2 sentences). Impl: Pleos TTS ON_DEVICE or platform TextToSpeech. */
interface VoiceOutput {
    fun speak(text: String, onError: (String) -> Unit)
    fun stop()
}

/** Hands a chosen stop to the navigation app as a waypoint (ADR-0003). */
interface NaviGateway {
    fun addWaypoint(place: StopPlace, onResult: (Result<Unit>) -> Unit)
    fun removeWaypoint(place: StopPlace, onResult: (Result<Unit>) -> Unit)
}

/** EV state of charge in percent; null = unknown (never shown as fact, PL-UJFQXQ). */
interface VehicleEnergySource {
    fun start(onSoc: (Int?) -> Unit)
    fun stop()
}
