package ai.familystop.core

data class GeoPoint(val lat: Double, val lon: Double, val timeMs: Long)

data class StopPlace(val name: String, val lat: Double, val lon: Double, val poiId: String? = null)

// --- Stop recommendation (#8). Shared by :feature:recommend, #9 (navigation), #12 (voice). ---

/** 가족이 정차에서 해결하려는 필요 (F-GFBDZR). */
enum class StopNeed { MEAL, RESTROOM, KIDS_REST }

/** 시설 데이터 출처. 화면에 실제/가상을 표시한다 (ADR-0001). */
enum class DataSource { REAL, VIRTUAL }

/** 추천 요청: 현재 위치와 해결하려는 필요. */
data class StopRequest(val origin: GeoPoint, val needs: Set<StopNeed>)

/**
 * 추천된 정차 후보. 카드 위계: 필요 충족([met]/[missed]) → 추천 이유([reason])
 * → 도착 시간 변화([extraMinutes]) → 이동 부담([detourKm]).
 */
data class StopCandidate(
    val place: StopPlace,
    val met: Set<StopNeed>,
    val missed: Set<StopNeed>,
    val reason: String,
    val extraMinutes: Int,
    val detourKm: Double,
    val source: DataSource
)

/** 정차 가능한 시설 하나. 우회 거리·시간은 경로 기준 계산값(시연 데이터는 사전 계산). */
data class Facility(
    val place: StopPlace,
    val provides: Set<StopNeed>,
    val source: DataSource,
    val detourKm: Double,
    val extraMinutes: Int
)

// --- Destination picking (#65). ---

/** 목적지 목록 항목의 출처. 집·회사는 내비에 저장된 것만 나온다. */
enum class DestinationKind { CURRENT, HOME, OFFICE, RECENT }

/** 내비에서 가져온 목적지([DestinationKind.CURRENT]=지금 안내 중인 목적지). 직접 입력은 받지 않는다(#65). */
data class Destination(
    val name: String,
    val lat: Double,
    val lon: Double,
    val kind: DestinationKind,
    val address: String? = null
)

/** 내비가 안내 중인 경로의 남은 시간(초). 도착 예상 시각 = 지금 + [remainSeconds]. */
data class RouteEta(
    val destinationName: String,
    val lat: Double,
    val lon: Double,
    val remainSeconds: Int
)
