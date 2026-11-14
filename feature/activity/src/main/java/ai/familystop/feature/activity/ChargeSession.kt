package ai.familystop.feature.activity

import ai.familystop.core.EnergyState
import java.time.Duration
import java.time.LocalDateTime

/** 충전 카드 상태 (#79). 차량에서 받은 값으로만 정하고, 사용자가 시간을 입력하지 않는다. */
enum class ChargeView { UNAVAILABLE, BEFORE, CHARGING, DONE }

/**
 * 충전 상태를 모르면 [ChargeView.UNAVAILABLE]. 충전 중이면 CHARGING, 이 정차에서 충전 중이었다가
 * 꺼졌으면([sawCharging]) DONE, 아직 시작 전이면 BEFORE.
 */
fun chargeView(state: EnergyState?, sawCharging: Boolean): ChargeView = when (state?.charging) {
    null -> ChargeView.UNAVAILABLE
    true -> ChargeView.CHARGING
    false -> if (sawCharging) ChargeView.DONE else ChargeView.BEFORE
}

/** 여정 카드 정차지 아래 문구. 충전 카드와 같은 상태를 따른다. */
fun stopStatus(view: ChargeView): String = when (view) {
    ChargeView.UNAVAILABLE -> "충전 정보 없음"
    ChargeView.BEFORE -> "충전 전"
    ChargeView.CHARGING -> "충전 중"
    ChargeView.DONE -> "충전 끝"
}

/** 희망 도착보다 늦으면 늦은 분, 아니면 null. */
fun lateMinutes(arrival: LocalDateTime, wished: LocalDateTime): Long? =
    Duration.between(wished, arrival).toMinutes().takeIf { it > 0 }
