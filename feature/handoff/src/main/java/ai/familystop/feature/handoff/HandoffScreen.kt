package ai.familystop.feature.handoff

import ai.familystop.core.StopPlace
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
}

private val Title = TextStyle(fontSize = 32.sp, lineHeight = 44.sp, fontWeight = FontWeight.Bold)
private val Body = TextStyle(fontSize = 24.sp, lineHeight = 34.sp, fontWeight = FontWeight.Medium)

/**
 * 단일 공개 화면. 주행 중에도 쓰므로 스크롤·펼치기 없이 큰 버튼만 둔다(ADR-0003).
 * 경유지 선택은 최대 2개 버튼 + 취소. [HandoffState.Idle]이면 아무것도 그리지 않는다.
 */
@Composable
fun HandoffScreen(
    state: HandoffState,
    onReplace: (StopPlace) -> Unit,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    onUndo: () -> Unit,
    modifier: Modifier = Modifier
) {
    val c = Palette(isSystemInDarkTheme())
    if (state is HandoffState.Idle) return
    Column(
        modifier.fillMaxWidth().clip(
            RoundedCornerShape(24.dp)
        ).background(c.surface).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        when (state) {
            HandoffState.Idle -> Unit
            is HandoffState.Sending -> {
                Text("${state.place.name} 전달 중…", style = Title, color = c.text)
                Text("응답을 기다리고 있어요.", style = Body, color = c.textSub)
            }
            is HandoffState.NeedReplace -> {
                Text("경유지가 가득 찼어요", style = Title, color = c.text)
                Text("새 경유지: ${state.place.name}. 바꿀 경유지를 골라 주세요.", style = Body, color = c.textSub)
                state.options.forEach { Btn("${it.name} 대신", c.raised, c.text, { onReplace(it) }) }
                Btn("취소", c.raised, c.text, onBack)
            }
            is HandoffState.Done -> {
                Text("✓ 경유지로 추가했어요", style = Title, color = c.text)
                Text(
                    "${state.place.name}이(가) 길 안내에 전달됐어요. 도착 여부와는 별개예요.",
                    style = Body,
                    color = c.textSub
                )
                if (state.undoFailed) Text("되돌리지 못했어요.", style = Body, color = c.danger)
                if (state.undoable) Btn("취소", c.accent, c.onAccent, onUndo)
            }
            is HandoffState.Failed -> {
                Text("! ${state.reason}", style = Title, color = c.danger)
                Text("${state.place.name}은 아직 추가되지 않았어요.", style = Body, color = c.textSub)
                Btn("다시 시도", c.accent, c.onAccent, onRetry)
                Btn("후보로 돌아가기", c.raised, c.text, onBack)
            }
        }
    }
}

@Composable
private fun Btn(label: String, bg: Color, fg: Color, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().heightIn(min = 76.dp).clip(RoundedCornerShape(16.dp)).background(bg)
            .clickable(role = Role.Button, onClick = onClick).padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) { Text(label, style = Body, color = fg) }
}
