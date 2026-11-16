package ai.familystop.feature.fallback

import ai.familystop.core.designsystem.App
import ai.familystop.core.designsystem.AppButton
import ai.familystop.core.designsystem.AppCard
import ai.familystop.core.designsystem.Status
import ai.familystop.core.designsystem.StatusChip
import ai.familystop.core.formatClock
import ai.familystop.core.formatDate
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource

/**
 * The single public entry point. A denied or revoked permission re-asks, and a failed data item
 * reconnects, only when the user taps [onRetry]; [onManual] opens the manual-input path. A last
 * known value carries its source and time and is never shown as current.
 */
@Composable
fun FallbackCard(
    cause: FallbackCause,
    nowMs: Long,
    onRetry: () -> Unit,
    onManual: () -> Unit,
    modifier: Modifier = Modifier
) = when (cause) {
    is FallbackCause.Permission -> {
        val c = cause.capability
        CardContent(
            title = stringResource(R.string.fallback_permission_title, stringResource(c.label())),
            lines = listOf(c.purpose(), c.limit(), c.alternative()).map { stringResource(it) },
            status = Status.Danger,
            statusText = stringResource(R.string.fallback_permission_off),
            primaryLabel = stringResource(R.string.fallback_permission_retry),
            onPrimary = onRetry,
            secondaryLabel = stringResource(R.string.fallback_manual),
            onSecondary = onManual,
            modifier = modifier
        )
    }
    is FallbackCause.Data -> {
        val f = cause.failure
        val value = valueText(describeValue(f.last, nowMs))
        CardContent(
            title = stringResource(R.string.fallback_data_title, f.item),
            lines = listOf(
                f.limit,
                stringResource(R.string.fallback_data_item_value, f.item, value)
            ),
            status = if (f.last == null) Status.Unknown else Status.Warn,
            statusText = stringResource(
                if (f.last ==
                    null
                ) {
                    R.string.fallback_status_unknown
                } else {
                    R.string.fallback_status_stale
                }
            ),
            primaryLabel = stringResource(R.string.fallback_data_reconnect),
            onPrimary = onRetry,
            secondaryLabel = stringResource(R.string.fallback_manual),
            onSecondary = onManual,
            modifier = modifier
        )
    }
}

private fun Capability.label() = when (this) {
    Capability.MICROPHONE -> R.string.fallback_mic_label
    Capability.LOCATION -> R.string.fallback_location_label
}

private fun Capability.purpose() = when (this) {
    Capability.MICROPHONE -> R.string.fallback_mic_purpose
    Capability.LOCATION -> R.string.fallback_location_purpose
}

private fun Capability.limit() = when (this) {
    Capability.MICROPHONE -> R.string.fallback_mic_limit
    Capability.LOCATION -> R.string.fallback_location_limit
}

private fun Capability.alternative() = when (this) {
    Capability.MICROPHONE -> R.string.fallback_mic_alternative
    Capability.LOCATION -> R.string.fallback_location_alternative
}

@Composable
private fun valueText(d: ValueDescription): String {
    val locale = LocalConfiguration.current.locales[0]
    return when (d) {
        ValueDescription.Unknown -> stringResource(R.string.fallback_status_unknown)
        is ValueDescription.Known -> {
            val time = formatClock(d.at.hour, d.at.minute, locale)
            if (d.otherDay) {
                val date = formatDate(d.at.toLocalDate(), locale)
                stringResource(
                    R.string.fallback_value_known_other_day,
                    d.value,
                    d.source,
                    date,
                    time
                )
            } else {
                stringResource(R.string.fallback_value_known, d.value, d.source, time)
            }
        }
    }
}

@Composable
private fun CardContent(
    title: String,
    lines: List<String>,
    status: Status,
    statusText: String,
    primaryLabel: String,
    onPrimary: () -> Unit,
    secondaryLabel: String,
    onSecondary: () -> Unit,
    modifier: Modifier
) {
    AppCard(modifier.fillMaxWidth(0.7f)) {
        StatusChip(status, statusText)
        Text(title, style = App.type.stopTitle, color = App.colors.textPrimary)
        lines.forEachIndexed { i, line ->
            if (i == 0) {
                Text(line, style = App.type.stopBody, color = App.colors.textPrimary)
            } else {
                Text(line, color = App.colors.textSecondary)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(App.space.lg)) {
            AppButton(primaryLabel, onPrimary, Modifier.weight(1f))
            AppButton(secondaryLabel, onSecondary, Modifier.weight(1f), primary = false)
        }
    }
}
