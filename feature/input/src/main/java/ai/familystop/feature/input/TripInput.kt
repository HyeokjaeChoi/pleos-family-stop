package ai.familystop.feature.input

import ai.familystop.core.Destination
import ai.familystop.core.DestinationKind
import ai.familystop.core.RouteEta
import java.time.LocalDateTime
import java.time.LocalTime

/** F-GFBDZR: 가족이 정차에서 해결하려는 필요. */
enum class StopNeed { MEAL, RESTROOM, KIDS_REST }

/** F-QOZAMV: 충전(보충) 필요 여부. */
enum class ChargeNeed { YES, NO, UNDECIDED }

/** 잔량 출처. 자동값과 수동값을 화면에서 구분해 표시한다(PL-UJFQXQ). */
sealed interface Soc {
    val percentOrNull: Int?

    data class Auto(val percent: Int) : Soc {
        override val percentOrNull get() = percent
    }

    data class Manual(val percent: Int) : Soc {
        override val percentOrNull get() = percent
    }

    /** 미확인: 도달 가능 여부를 보장하지 않는다. */
    data object Unknown : Soc {
        override val percentOrNull: Int? = null
    }
}

// ponytail: 수동 입력 시작값 50%와 5%p 단위는 잠정값.
const val SOC_STEP = 5

/** 수동 잔량 스테퍼 증감. 처음 누르면 50%에서 시작하고 0~100을 벗어나지 않는다. */
fun stepSoc(current: Int?, delta: Int): Int = ((current ?: 50) + delta * SOC_STEP).coerceIn(0, 100)

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

enum class InputError { DESTINATION_MISSING, TIME_MISSING }

data class TripInput(
    val destination: Destination,
    val arrival: LocalDateTime,
    val needs: Set<StopNeed>,
    val chargeNeed: ChargeNeed,
    val soc: Soc
)

sealed interface ValidationResult {
    data class Ok(val input: TripInput) : ValidationResult

    data class Invalid(val errors: List<InputError>) : ValidationResult
}

/**
 * 폼 초안. 목적지는 내비 목록에서 고르기만 한다(#65) — [destinationKey]로 기억한다.
 * 도착 시각은 [hour24]가 null이면 아직 정하지 않은 것(현재+1시간 같은 폴백 없음).
 */
data class TripDraft(
    val destinationKey: String? = null,
    val hour24: Int? = null,
    val minute: Int = 0,
    val needs: Set<StopNeed> = emptySet(),
    val chargeNeed: ChargeNeed = ChargeNeed.UNDECIDED,
    val manualSoc: Int? = null
) : java.io.Serializable

fun destinationKey(d: Destination) = "${d.name}@${d.lat},${d.lon}"

/** 선택한 시각이 이미 지났으면 내일로 해석한다(#65). */
fun arrivalFrom(hour24: Int, minute: Int, now: LocalDateTime): LocalDateTime {
    val today = now.toLocalDate().atTime(LocalTime.of(hour24, minute))
    return if (today.isAfter(now)) today else today.plusDays(1)
}

/** 내비 예상 도착 시각(지금 + 남은 시간)의 시·분. 경로가 없으면 null. */
fun etaTime(eta: RouteEta?, now: LocalDateTime): LocalTime? = eta?.takeIf {
    it.remainSeconds > 0
}?.let { now.plusSeconds(it.remainSeconds.toLong()).toLocalTime() }

/** 안내 중인 목적지가 목록에 없으면 맨 앞에 [DestinationKind.CURRENT]로 넣고, 있으면 그 항목을 앞으로 올린다. */
fun mergeCurrent(list: List<Destination>, eta: RouteEta?): List<Destination> {
    if (eta == null || eta.destinationName.isBlank()) return list
    val same = list.firstOrNull { it.name == eta.destinationName }
    val current =
        (same ?: Destination(eta.destinationName, eta.lat, eta.lon, DestinationKind.RECENT))
            .copy(kind = DestinationKind.CURRENT)
    return listOf(current) + list.filter { it !== same }
}

/** 추천 요청 전 검증. 목적지·시각이 비면 [ValidationResult.Invalid]. */
fun validate(
    draft: TripDraft,
    destinations: List<Destination>,
    autoSoc: Int?,
    now: LocalDateTime
): ValidationResult {
    val errors = mutableListOf<InputError>()
    val dest = destinations.firstOrNull { destinationKey(it) == draft.destinationKey }
    if (dest == null) errors += InputError.DESTINATION_MISSING
    val hour = draft.hour24
    if (hour == null) errors += InputError.TIME_MISSING
    if (dest == null || hour == null) return ValidationResult.Invalid(errors)
    return ValidationResult.Ok(
        TripInput(
            destination = dest,
            arrival = arrivalFrom(hour, draft.minute, now),
            needs = draft.needs,
            chargeNeed = draft.chargeNeed,
            soc = resolveSoc(autoSoc, draft.manualSoc)
        )
    )
}

/** 정차 계획 3단계(#72). 상단 요약 칩이 진행 표시이자 되돌아가기다. */
enum class PlanStep { DESTINATION, CONDITIONS, CANDIDATES }

/** 요약 칩 한 개(ADR-0007: 의미 값, 문장은 화면이 만든다). [filled]가 false면 아직 정하지 않은 값. 누르면 [step]으로 돌아간다. */
sealed interface SummaryChip {
    val filled: Boolean
    val step: PlanStep

    data class Destination(val name: String?) : SummaryChip {
        override val filled get() = name != null
        override val step get() = PlanStep.DESTINATION
    }

    data class Arrival(val time: LocalTime?) : SummaryChip {
        override val filled get() = time != null
        override val step get() = PlanStep.CONDITIONS
    }

    data class Needs(val needs: List<StopNeed>) : SummaryChip {
        override val filled get() = needs.isNotEmpty()
        override val step get() = PlanStep.CONDITIONS
    }
}

/** 목적지 · 도착 시각 · 필요 항목 요약 칩 3개. */
fun summaryChips(draft: TripDraft, destinations: List<Destination>): List<SummaryChip> {
    val dest = destinations.firstOrNull { destinationKey(it) == draft.destinationKey }
    return listOf(
        SummaryChip.Destination(dest?.name),
        SummaryChip.Arrival(draft.hour24?.let { LocalTime.of(it, draft.minute) }),
        SummaryChip.Needs(StopNeed.entries.filter { it in draft.needs })
    )
}
