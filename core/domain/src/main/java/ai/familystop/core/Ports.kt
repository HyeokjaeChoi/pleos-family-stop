package ai.familystop.core

// Swappable seams (ADR-0001, ADR-0002). Each Pleos SDK that is still "준비 중"
// gets a platform/fake implementation now and a Pleos one when it opens.

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

/** 시설 제공처. Pleos SDK에 POI 검색이 없어(ADR-0001) 외부 제공처/내장 JSON이 구현한다. */
fun interface FacilitySource {
    suspend fun facilities(): List<Facility>
}
