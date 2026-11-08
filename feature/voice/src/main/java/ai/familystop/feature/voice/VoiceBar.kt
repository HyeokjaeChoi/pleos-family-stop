package ai.familystop.feature.voice

import ai.familystop.core.designsystem.App
import ai.familystop.core.designsystem.AppButton
import ai.familystop.core.designsystem.AppCard
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * 공개 진입점. 음성 실패 한 줄 상태와 "다음"(3번째 후보 안내, STT 오픈 전 입력 수단) 버튼.
 * 보일 것이 없으면 아무것도 그리지 않는다. 주행 중에도 쓸 수 있게 한 번 탭으로 끝난다(ADR-0003).
 */
@Composable
fun VoiceBar(guide: VoiceGuide, modifier: Modifier = Modifier) {
    val status = guide.status
    if (status == null && !guide.hasNext) return
    AppCard(modifier.fillMaxWidth()) {
        if (status != null) Text(status, style = App.type.driveBody)
        if (guide.hasNext) AppButton("다음 후보", guide::next, Modifier.fillMaxWidth(), large = true)
    }
}
