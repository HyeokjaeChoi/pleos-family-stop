package ai.familystop.feature.fallback

import ai.familystop.core.designsystem.App
import ai.familystop.core.designsystem.AppButton
import ai.familystop.core.designsystem.AppCard
import ai.familystop.core.designsystem.Status
import ai.familystop.core.designsystem.StatusChip
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

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
        val notice = PermissionNotice(cause.capability)
        CardContent(
            title = notice.title,
            lines = notice.lines,
            status = Status.Danger,
            statusText = "권한 꺼짐",
            primaryLabel = "권한 다시 요청",
            onPrimary = onRetry,
            secondaryLabel = "직접 입력",
            onSecondary = onManual,
            modifier = modifier
        )
    }
    is FallbackCause.Data -> {
        val f = cause.failure
        CardContent(
            title = "${f.item}을(를) 받지 못했어요",
            lines = listOf(f.limit, "${f.item}: ${describeValue(f.last, nowMs)}"),
            status = if (f.last == null) Status.Unknown else Status.Warn,
            statusText = if (f.last == null) "미확인" else "최신 아님",
            primaryLabel = "다시 연결",
            onPrimary = onRetry,
            secondaryLabel = "직접 입력",
            onSecondary = onManual,
            modifier = modifier
        )
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
