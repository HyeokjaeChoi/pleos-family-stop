package ai.familystop.feature.input

import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeParseException

/** F-GFBDZR: 가족이 정차에서 해결하려는 필요. */
enum class StopNeed(val label: String) {
    MEAL("식사"),
    RESTROOM("화장실"),
    KIDS_REST("아이 휴식")
}

/** F-QOZAMV: 충전(보충) 필요 여부. */
enum class ChargeNeed(val label: String) {
    YES("필요"),
    NO("불필요"),
    UNDECIDED("미정")
}

/** 잔량 출처. 자동값과 수동값을 화면에서 구분해 표시한다(PL-UJFQXQ). */
sealed interface Soc {
    data class Auto(val percent: Int) : Soc

    data class Manual(val percent: Int) : Soc

    /** 미확인: 도달 가능 여부를 보장하지 않는다. */
    data object Unknown : Soc
}

/** 차량 자동값이 있으면 그것을, 없으면 수동 입력을, 둘 다 없으면 Unknown. */
fun resolveSoc(auto: Int?, manual: Int?): Soc = when {
    auto != null -> Soc.Auto(auto)
    manual != null -> Soc.Manual(manual)
    else -> Soc.Unknown
}

// ponytail: 불일치 기준 10%p는 잠정값. 시연 측정 후 조정.
const val SOC_MISMATCH_PERCENT = 10

/** 수동 입력과 차량 자동값이 크게 다르면 true → 안내 문구를 보여 준다. */
fun socMismatch(auto: Int?, manual: Int?): Boolean =
    auto != null && manual != null && kotlin.math.abs(auto - manual) >= SOC_MISMATCH_PERCENT

enum class InputError(val message: String) {
    DESTINATION_MISSING("목적지를 입력해 주세요"),
    TIME_INVALID("도착 시간을 00:00 형식으로 입력해 주세요"),
    TIME_PAST("지난 시간이에요. 도착 시간을 다시 정해 주세요"),
    SOC_RANGE("배터리 잔량은 0~100 사이로 입력해 주세요")
}

data class TripInput(
    val destination: String,
    val arrival: LocalDateTime,
    val needs: Set<StopNeed>,
    val urgent: Boolean,
    val chargeNeed: ChargeNeed,
    val soc: Soc
)

sealed interface ValidationResult {
    data class Ok(val input: TripInput) : ValidationResult

    data class Invalid(val errors: List<InputError>) : ValidationResult
}

/** 폼 초안. 주행 중에도 보존되도록 화면에서 rememberSaveable로 들고 있다. */
data class TripDraft(
    val destination: String = "",
    val timeText: String = "",
    val needs: Set<StopNeed> = emptySet(),
    val urgent: Boolean = false,
    val chargeNeed: ChargeNeed = ChargeNeed.UNDECIDED,
    val manualSocText: String = ""
)

/**
 * 추천 요청 전 검증. 목적지 누락·지난 시간은 수정을 요구한다(F-GFBDZR).
 * ponytail: 도착 시각은 '오늘' 기준 HH:mm. 자정을 넘기는 여행은 날짜 입력이 필요할 때 추가.
 */
fun validate(draft: TripDraft, autoSoc: Int?, now: LocalDateTime): ValidationResult {
    val errors = mutableListOf<InputError>()
    if (draft.destination.isBlank()) errors += InputError.DESTINATION_MISSING

    val time =
        try {
            LocalTime.parse(draft.timeText.trim().padStart(5, '0'))
        } catch (_: DateTimeParseException) {
            null
        }
    val arrival = time?.let { now.toLocalDate().atTime(it) }
    when {
        arrival == null -> errors += InputError.TIME_INVALID
        arrival.isBefore(now) -> errors += InputError.TIME_PAST
    }

    val manual = draft.manualSocText.trim().takeIf {
        it.isNotEmpty()
    }?.let { it.toIntOrNull() ?: -1 }
    if (autoSoc == null && manual != null && manual !in 0..100) errors += InputError.SOC_RANGE

    if (errors.isNotEmpty()) return ValidationResult.Invalid(errors)
    return ValidationResult.Ok(
        TripInput(
            destination = draft.destination.trim(),
            arrival = arrival!!,
            needs = draft.needs,
            urgent = draft.urgent,
            chargeNeed = draft.chargeNeed,
            soc = resolveSoc(autoSoc, manual)
        )
    )
}
