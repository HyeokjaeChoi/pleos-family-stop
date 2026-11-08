package ai.familystop.feature.fallback

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private class Palette(night: Boolean) { // DESIGN.md 2절
    val raised = if (night) Color(0xFF272E34) else Color(0xFFEEF1F4)
    val surface = if (night) Color(0xFF1B2126) else Color(0xFFFFFFFF)
    val text = if (night) Color(0xFFF2F5F7) else Color(0xFF11181D)
    val textSub = if (night) Color(0xFFB4BEC6) else Color(0xFF46535C)
    val accent = if (night) Color(0xFF5CB8FF) else Color(0xFF0A62BA)
    val onAccent = if (night) Color(0xFF06121C) else Color(0xFFFFFFFF)
    val danger = if (night) Color(0xFFFF6B6B) else Color(0xFFB71C1C)
    val unknown = if (night) Color(0xFF9AA7B0) else Color(0xFF4F5E68)
}

private val Title = TextStyle(fontSize = 32.sp, lineHeight = 44.sp, fontWeight = FontWeight.Bold)
private val Body = TextStyle(fontSize = 24.sp, lineHeight = 34.sp, fontWeight = FontWeight.Medium)

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
            statusText = "권한 꺼짐",
            danger = true,
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
            statusText = if (f.last == null) "미확인" else "최신 아님",
            danger = f.last != null,
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
    statusText: String,
    danger: Boolean,
    primaryLabel: String,
    onPrimary: () -> Unit,
    secondaryLabel: String,
    onSecondary: () -> Unit,
    modifier: Modifier
) {
    val c = Palette(isSystemInDarkTheme())
    val tint = if (danger) c.danger else c.unknown
    val icon: ImageVector = if (danger) Icons.Rounded.Warning else Icons.Rounded.Info
    Column(
        modifier.fillMaxWidth().clip(
            RoundedCornerShape(24.dp)
        ).background(c.surface).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(36.dp))
            Text(statusText, style = Body, color = tint)
        }
        Text(title, style = Title, color = c.text)
        lines.forEach { Text(it, style = Body, color = c.textSub) }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            FallbackButton(primaryLabel, c.accent, c.onAccent, onPrimary, Modifier.weight(1f))
            FallbackButton(secondaryLabel, c.raised, c.text, onSecondary, Modifier.weight(1f))
        }
    }
}

@Composable
private fun FallbackButton(
    label: String,
    bg: Color,
    fg: Color,
    onClick: () -> Unit,
    modifier: Modifier
) {
    Box(
        modifier.heightIn(min = 76.dp).clip(RoundedCornerShape(16.dp)).background(bg)
            .clickable(role = Role.Button, onClick = onClick).padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) { Text(label, style = Body, color = fg) }
}
