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
