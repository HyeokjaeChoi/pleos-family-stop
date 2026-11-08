package ai.familystop.feature.recommend

import ai.familystop.core.DataSource
import ai.familystop.core.StopNeed.KIDS_REST
import ai.familystop.core.StopNeed.MEAL
import ai.familystop.core.StopNeed.RESTROOM
import ai.familystop.core.StopPlace

// 시연 구간(서울 남쪽 경부축 근처) 시설. ADR-0001: 외부 POI 제공처가 없는 동안 쓰는 내장 데이터.
// ponytail: 아래는 전부 가상. 현장 확인한 실제 시설은 DataSource.REAL로 추가한다(좌표·이름 검증 후).
val DemoFacilities: List<Facility> = listOf(
    Facility(
        StopPlace("가상 가족식당 A", 37.30, 127.05, "demo-a"),
        setOf(MEAL, RESTROOM),
        DataSource.VIRTUAL,
        1.2,
        4
    ),
    Facility(
        StopPlace("가상 키즈카페 B", 37.28, 127.02, "demo-b"),
        setOf(KIDS_REST, RESTROOM),
        DataSource.VIRTUAL,
        2.5,
        7
    ),
    Facility(
        StopPlace("가상 휴게소 C", 37.25, 127.08, "demo-c"),
        setOf(MEAL, RESTROOM, KIDS_REST),
        DataSource.VIRTUAL,
        0.3,
        2
    ),
    Facility(
        StopPlace("가상 화장실 D", 37.33, 127.10, "demo-d"),
        setOf(RESTROOM),
        DataSource.VIRTUAL,
        0.5,
        1
    ),
    Facility(
        StopPlace("가상 분식점 E", 37.20, 127.00, "demo-e"),
        setOf(MEAL),
        DataSource.VIRTUAL,
        3.0,
        9
    )
)

val DemoFacilitySource = FacilitySource { DemoFacilities }
