package ai.familystop.feature.recommend

import ai.familystop.core.Facility
import ai.familystop.core.StopCandidate
import ai.familystop.core.StopReason
import ai.familystop.core.StopRequest
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

const val MAX_CANDIDATES = 3
const val DRIVING_MAX_CANDIDATES = 2 // ADR-0003: 주행 중 큰 카드 최대 2개

// ponytail: 시연 구간 반경 고정. 실제 경로 기반 탐색은 외부 제공처 연동 시 교체.
private const val RADIUS_KM = 30.0

/** 필요를 하나라도 충족하는 시설만, 충족 수 많은 순 → 도착 지연 짧은 순으로 최대 3개. */
fun recommend(request: StopRequest, all: List<Facility>): List<StopCandidate> = all
    .filter {
        distanceKm(request.origin.lat, request.origin.lon, it.place.lat, it.place.lon) <=
            RADIUS_KM
    }
    .map { it to request.needs.intersect(it.provides) }
    .filter { (_, met) -> met.isNotEmpty() }
    .sortedWith(compareBy({ -it.second.size }, { it.first.extraMinutes }))
    .take(MAX_CANDIDATES)
    .map { (f, met) ->
        StopCandidate(
            place = f.place,
            met = met,
            missed = request.needs - met,
            reason = StopReason(met.toList(), coversAll = met.size == request.needs.size),
            extraMinutes = f.extraMinutes,
            detourKm = f.detourKm,
            source = f.source
        )
    }

private fun distanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val p = Math.PI / 180
    val a = sin((lat2 - lat1) * p / 2).pow(2) +
        cos(lat1 * p) * cos(lat2 * p) * sin((lon2 - lon1) * p / 2).pow(2)
    return 2 * 6371.0 * asin(sqrt(a))
}
