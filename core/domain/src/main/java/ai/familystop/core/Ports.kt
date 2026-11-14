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

/** EV 잔량·충전 상태·주행 가능 거리. 값이 바뀔 때마다 main에서 [EnergyState]를 준다 (PL-UJFQXQ, #79). */
interface VehicleEnergySource {
    fun start(onState: (EnergyState) -> Unit)
    fun stop()
}

/** 시설 제공처. Pleos SDK에 POI 검색이 없어(ADR-0001) 외부 제공처/내장 JSON이 구현한다. */
fun interface FacilitySource {
    suspend fun facilities(): List<Facility>
}

/** 내비의 최근·저장 목적지와 현재 경로 ETA. Impl: Pleos NaviHelper (#65). 결과는 main에서 돌려준다. */
interface DestinationSource {
    /** 집·회사(저장된 것만)와 최근 목적지. 실패하면 failure. */
    fun load(onResult: (Result<List<Destination>>) -> Unit)

    /** 안내 중인 경로의 남은 시간. 경로가 없으면 success(null), 모르면 failure. */
    fun currentEta(onResult: (Result<RouteEta?>) -> Unit)
}
