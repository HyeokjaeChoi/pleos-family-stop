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
 * Shown when [PermissionNotice.capability]'s permission is denied or revoked. Re-asking happens
 * only when the user taps [onRequestAgain]; [onUseAlternative] opens the manual path.
 */
@Composable
fun PermissionDeniedCard(
    notice: PermissionNotice,
    onRequestAgain: () -> Unit,
    onUseAlternative: () -> Unit,
    modifier: Modifier = Modifier
) = FallbackCard(
    title = notice.title,
    lines = notice.lines,
    statusText = "권한 꺼짐",
    danger = true,
    primary = "권한 다시 요청" to onRequestAgain,
    secondary = "직접 입력" to onUseAlternative,
    modifier = modifier
)

/**
 * Shown when a data item failed to arrive. The last known value carries its source and time and
 * is labelled as such; nothing is shown as current. Reconnecting happens only on [onReconnect].
 */
@Composable
fun DataUnavailableCard(
    failure: DataFailure,
    onReconnect: () -> Unit,
    onManualInput: () -> Unit,
    modifier: Modifier = Modifier
) = FallbackCard(
    title = "${failure.item}을(를) 받지 못했어요",
    lines = listOf(failure.limit, "${failure.item}: ${describeValue(failure.last)}"),
    statusText = "미확인",
    danger = failure.last != null,
    primary = "다시 연결" to onReconnect,
    secondary = "직접 입력" to onManualInput,
    modifier = modifier
)

@Composable
private fun FallbackCard(
    title: String,
    lines: List<String>,
    statusText: String,
    danger: Boolean,
    primary: Pair<String, () -> Unit>,
    secondary: Pair<String, () -> Unit>,
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
            FallbackButton(primary.first, c.accent, c.onAccent, primary.second, Modifier.weight(1f))
            FallbackButton(
                secondary.first,
                c.surface,
                c.text,
                secondary.second,
                Modifier.weight(1f)
            )
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
