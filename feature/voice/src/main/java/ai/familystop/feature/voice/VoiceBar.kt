package ai.familystop.feature.voice

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

private val Body = TextStyle(fontSize = 32.sp, lineHeight = 44.sp, fontWeight = FontWeight.Medium)

/**
 * 공개 진입점. 음성 실패 한 줄 상태와 "다음"(3번째 후보 안내, STT 오픈 전 입력 수단) 버튼.
 * 보일 것이 없으면 아무것도 그리지 않는다. 주행 중에도 쓸 수 있게 한 번 탭으로 끝난다(ADR-0003).
 */
@Composable
fun VoiceBar(guide: VoiceGuide, modifier: Modifier = Modifier) {
    val status = guide.status
    if (status == null && !guide.hasNext) return
    val night = isSystemInDarkTheme() // DESIGN.md 2절
    val surface = if (night) Color(0xFF1B2126) else Color(0xFFFFFFFF)
    val text = if (night) Color(0xFFF2F5F7) else Color(0xFF11181D)
    val accent = if (night) Color(0xFF5CB8FF) else Color(0xFF0A62BA)
    val onAccent = if (night) Color(0xFF06121C) else Color(0xFFFFFFFF)
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(surface).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        if (status != null) Text(status, style = Body, color = text)
        if (guide.hasNext) {
            Box(
                Modifier.fillMaxWidth().heightIn(min = 76.dp).clip(RoundedCornerShape(16.dp))
                    .background(accent).clickable(role = Role.Button, onClick = guide::next)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) { Text("다음 후보", style = Body, color = onAccent) }
        }
    }
}
