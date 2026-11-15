package ai.familystop.feature.fallback

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/** Permissions requested on first use of the feature that needs them, never all at once (F-JUUGZR). */
enum class Capability { MICROPHONE, LOCATION }

/** A value last received from [source] at [atMs] (epoch ms). Never shown as the current value. */
data class LastKnown(val value: String, val source: String, val atMs: Long)

/**
 * What the screen says about a data item's last value (ADR-0007: semantic value, no sentence).
 * [Known.otherDay] is true when the value is from another day, so the screen also shows its date and it
 * is never mistaken for today's.
 */
sealed interface ValueDescription {
    /** Nothing was ever received. */
    data object Unknown : ValueDescription

    data class Known(
        val value: String,
        val source: String,
        val at: LocalDateTime,
        val otherDay: Boolean
    ) : ValueDescription
}

fun describeValue(
    known: LastKnown?,
    nowMs: Long,
    zone: ZoneId = ZoneId.systemDefault()
): ValueDescription {
    if (known == null) return ValueDescription.Unknown
    val at = Instant.ofEpochMilli(known.atMs).atZone(zone)
    val today = at.toLocalDate() == Instant.ofEpochMilli(nowMs).atZone(zone).toLocalDate()
    val local = at.toLocalDateTime()
    return ValueDescription.Known(known.value, known.source, local, otherDay = !today)
}

/** What a failed data item looks like: which item, why, and the last value if any. */
data class DataFailure(val item: String, val limit: String, val last: LastKnown? = null)

/** Why the fallback is shown. Denied and revoked are the same: stop, explain, never resume on its own. */
sealed interface FallbackCause {
    data class Permission(val capability: Capability) : FallbackCause

    data class Data(val failure: DataFailure) : FallbackCause
}
