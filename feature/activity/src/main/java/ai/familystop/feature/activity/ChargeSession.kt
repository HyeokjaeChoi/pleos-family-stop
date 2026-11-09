package ai.familystop.feature.activity

import java.time.Duration
import java.time.LocalDateTime

/** 충전 종료까지 남기는 시간 단위(분). 스테퍼 한 번에 이만큼 바뀐다. */
const val STEP_MIN = 5
const val MIN_MINUTES = 5
const val MAX_MINUTES = 180

// ponytail: 차량에서 충전 시간을 받지 못해 25분은 시연용 시작값. 시연 측정 후 조정.
const val DEFAULT_MINUTES = 25

/** 스테퍼 증감. [MIN_MINUTES]~[MAX_MINUTES]를 벗어나지 않는다. */
fun stepMinutes(current: Int, delta: Int): Int =
    (current + delta * STEP_MIN).coerceIn(MIN_MINUTES, MAX_MINUTES)

/** 충전 시작 시각과 종료 예상. 종료는 시작 시점 입력값으로 고정한다(시계가 가도 밀리지 않게). */
data class ChargeClock(val startedAt: LocalDateTime, val endsAt: LocalDateTime) {
    fun remainingSeconds(now: LocalDateTime): Long =
        Duration.between(now, endsAt).seconds.coerceAtLeast(0)

    /** 0(시작)~1(종료). */
    fun progress(now: LocalDateTime): Float {
        val total = Duration.between(startedAt, endsAt).seconds
        if (total <= 0) return 1f
        return (Duration.between(startedAt, now).seconds.toFloat() / total).coerceIn(0f, 1f)
    }

    fun extend(minutes: Int) = copy(endsAt = endsAt.plusMinutes(minutes.toLong()))

    fun finished(now: LocalDateTime) = !endsAt.isAfter(now)
}

/** 남은 시간 표기. 1시간 미만 mm:ss, 이상 h:mm:ss. */
fun formatRemaining(seconds: Long): String {
    val s = seconds.coerceAtLeast(0)
    val h = s / 3600
    val m = s % 3600 / 60
    val sec = s % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, sec) else "%02d:%02d".format(m, sec)
}

/** 희망 도착보다 늦으면 늦은 분, 아니면 null. */
fun lateMinutes(arrival: LocalDateTime, wished: LocalDateTime): Long? =
    Duration.between(wished, arrival).toMinutes().takeIf { it > 0 }
