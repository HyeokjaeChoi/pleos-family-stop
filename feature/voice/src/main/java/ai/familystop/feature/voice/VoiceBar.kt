package ai.familystop.feature.voice

import ai.familystop.core.designsystem.App
import ai.familystop.core.designsystem.AppButton
import ai.familystop.core.designsystem.AppIcons
import ai.familystop.core.designsystem.R as DsR
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource

/**
 * 주행 중 음성 한 줄 상태(VoiceHint). 큰 카드가 아니라 한 줄이라 후보 카드 2장이 시각 1순위다.
 * 3번째 후보 "다음 후보"는 STT 오픈 전 유일한 입력 수단이라 보조 버튼으로 남긴다(ADR-0002·0003).
 * 보일 것이 없으면 아무것도 그리지 않는다.
 */
@Composable
fun VoiceBar(guide: VoiceGuide, modifier: Modifier = Modifier) {
    val status = guide.status
    if (status == null && !guide.hasNext) return
    Row(
        modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(App.space.md)
    ) {
        Icon(
            AppIcons.VolumeUp,
            contentDescription = null,
            tint = App.colors.textSecondary,
            modifier = Modifier.size(App.icon.secondary)
        )
        Text(
            status?.text() ?: stringResource(R.string.voice_on),
            style = App.type.stopBody,
            color = App.colors.textSecondary,
            modifier = Modifier.weight(1f)
        )
        if (guide.hasNext) {
            AppButton(
                stringResource(R.string.voice_next),
                guide::next,
                primary = false
            )
        }
    }
}

@Composable
private fun VoiceStatus.text(): String = when (this) {
    is VoiceStatus.Candidates ->
        pluralStringResource(R.plurals.voice_status_candidates, count, count)
    is VoiceStatus.NextCandidate -> stringResource(
        R.string.voice_status_next,
        stringResource(DsR.string.ds_unit_ordinal, ordinal)
    )
    VoiceStatus.Added -> stringResource(R.string.voice_status_added)
    VoiceStatus.AddFailed -> stringResource(R.string.voice_status_add_failed)
}
