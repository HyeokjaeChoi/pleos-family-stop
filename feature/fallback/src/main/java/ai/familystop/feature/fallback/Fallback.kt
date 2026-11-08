package ai.familystop.feature.fallback

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Permissions requested on first use of the feature that needs them, never all at once (F-JUUGZR). */
enum class Capability(
    val label: String,
    val purpose: String,
    val limit: String,
    val alternative: String
) {
    MICROPHONE(
        label = "마이크",
        purpose = "음성으로 정차 요청을 말하려면 마이크가 필요해요.",
        limit = "지금은 음성 입력을 쓸 수 없어요.",
        alternative = "정차 중에 화면으로 입력해 주세요."
    ),
    LOCATION(
        label = "위치",
        purpose = "가까운 정차 후보를 찾으려면 현재 위치가 필요해요.",
        limit = "지금은 현재 위치 기준 추천을 할 수 없어요.",
        alternative = "목적지나 경유지를 직접 입력해 주세요."
    )
}

/** Denied and revoked are handled the same way: stop the task, explain, never resume on its own. */
data class PermissionNotice(val capability: Capability) {
    val title = "${capability.label} 권한이 꺼져 있어요"
    val lines = listOf(capability.purpose, capability.limit, capability.alternative)
}

/** A value last received from [source] at [atMs] (epoch ms). Never shown as the current value. */
data class LastKnown(val value: String, val source: String, val atMs: Long)

private val sameDay = DateTimeFormatter.ofPattern("HH:mm")
private val otherDay = DateTimeFormatter.ofPattern("M월 d일 HH:mm")

/**
 * `"72% · 마지막 확인값 · 차량 14:05"`, or `"미확인"` when nothing was ever received. A value from
 * another day also carries its date so it is never mistaken for today's.
 */
fun describeValue(known: LastKnown?, nowMs: Long, zone: ZoneId = ZoneId.systemDefault()): String {
    if (known == null) return "미확인"
    val at = Instant.ofEpochMilli(known.atMs).atZone(zone)
    val fmt = if (at.toLocalDate() ==
        Instant.ofEpochMilli(nowMs).atZone(zone).toLocalDate()
    ) {
        sameDay
    } else {
        otherDay
    }
    return "${known.value} · 마지막 확인값 · ${known.source} ${fmt.format(at)}"
}

/** What a failed data item looks like: which item, why, and the last value if any. */
data class DataFailure(val item: String, val limit: String, val last: LastKnown? = null)
