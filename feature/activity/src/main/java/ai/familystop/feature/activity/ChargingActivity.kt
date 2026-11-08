package ai.familystop.feature.activity

import java.time.Duration
import java.time.LocalDateTime

/** F-LFACHH: 충전 중 가족이 할 수 있는 활동 종류. */
enum class ActivityKind(val label: String) {
    MEAL("식사"),
    SNACK("간식"),
    REST("휴식")
}

/** 주변 후보. 이동·소요 시간은 호출자(추천 모듈)가 채워 준다. */
data class ActivityCandidate(
    val name: String,
    val kind: ActivityKind,
    val roundTripMin: Int,
    val activityMin: Int
)

/** 충전 종료 예상. 자동 확인이 없으면 사용자 입력(자동 아님)으로 구분한다. */
sealed interface ChargeInfo {
    data class Auto(val endsAt: LocalDateTime) : ChargeInfo

    data class Manual(val endsAt: LocalDateTime) : ChargeInfo

    /** 미확인: 활동 완료 가능성을 단정하지 않는다. */
    data object Unknown : ChargeInfo
}

/** 차량 자동값이 있으면 그것을, 없으면 수동 입력을, 둘 다 없으면 Unknown. */
fun resolveCharge(auto: LocalDateTime?, manual: LocalDateTime?): ChargeInfo = when {
    auto != null -> ChargeInfo.Auto(auto)
    manual != null -> ChargeInfo.Manual(manual)
    else -> ChargeInfo.Unknown
}

// ponytail: 복귀 여유 5분은 잠정값. 시연 측정 후 조정.
private const val RETURN_BUFFER_MIN = 5

/** 활동 가능 시간 판정. [confirmed]=false면 사용자 입력 기준이라 보장하지 않는다. */
sealed interface Fit {
    data object Unknown : Fit

    /** [slackMin] 분 여유(0 이상). */
    data class Fits(val slackMin: Int, val confirmed: Boolean) : Fit

    /** [shortMin] 분 부족. */
    data class TooShort(val shortMin: Int, val confirmed: Boolean) : Fit

    /** 충전 종료 예상 시각이 이미 지남. 다시 입력이 필요하다. */
    data object Passed : Fit
}

/** 활동 가능 시간 = 충전 종료 예상 − 왕복 이동 − 활동 소요 − 복귀 여유. 정보가 바뀌면 다시 호출해 재계산한다. */
fun assess(c: ActivityCandidate, charge: ChargeInfo, now: LocalDateTime): Fit {
    val (endsAt, confirmed) =
        when (charge) {
            is ChargeInfo.Auto -> charge.endsAt to true
            is ChargeInfo.Manual -> charge.endsAt to false
            ChargeInfo.Unknown -> return Fit.Unknown
        }
    if (!endsAt.isAfter(now)) return Fit.Passed
    val slack =
        Duration.between(now, endsAt).toMinutes().toInt() - c.roundTripMin - c.activityMin -
            RETURN_BUFFER_MIN
    return if (slack >= 0) Fit.Fits(slack, confirmed) else Fit.TooShort(-slack, confirmed)
}

/** 화면 문구. 미확인 입력은 완료 가능성을 단정하지 않는다. */
fun Fit.message(): String = when (this) {
    Fit.Unknown -> "미확인 · 충전 종료 시간을 몰라 다녀올 수 있는지 알 수 없어요"
    Fit.Passed -> "충전 종료 예상 시각이 지났어요. 시간을 다시 확인해 주세요"
    is Fit.Fits -> "여유 ${slackMin}분" + if (confirmed) "" else " · 직접 입력 기준, 보장 안 돼요"
    is Fit.TooShort -> "${shortMin}분 부족해요" + if (confirmed) "" else " · 직접 입력 기준"
}

/** 사용자가 입력한 남은 분을 입력 시점의 절대 시각으로 고정한다(시계가 가도 밀리지 않게). */
fun manualEnd(minutesText: String, now: LocalDateTime): LocalDateTime? =
    minutesText.toIntOrNull()?.let { now.plusMinutes(it.toLong()) }
