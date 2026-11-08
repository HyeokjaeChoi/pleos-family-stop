package ai.familystop.feature.recommend

import ai.familystop.core.DataSource
import ai.familystop.core.StopCandidate
import ai.familystop.core.StopPlace
import ai.familystop.core.designsystem.App
import ai.familystop.core.designsystem.AppButton
import ai.familystop.core.designsystem.AppCard
import ai.familystop.core.designsystem.Status
import ai.familystop.core.designsystem.StatusChip
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * 정차 중 후보 비교 화면(스크롤 허용). 주행 중에는 이 화면을 쓰지 않고
 * [driveCards]를 큰 카드로 보여 준다(ADR-0003). 앱 셸이 주행/정차를 골라 호출한다.
 */
@Composable
fun RecommendScreen(
    state: RecommendState,
    onPick: (StopPlace) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier.padding(App.space.md),
        verticalArrangement = Arrangement.spacedBy(App.space.md)
    ) {
        when (state) {
            RecommendState.Idle -> Unit
            RecommendState.Loading -> Text("근처 시설을 찾는 중…", style = App.type.stopTitle)
            RecommendState.Empty -> Notice("조건에 맞는 정차 후보가 없어요.", onRetry)
            is RecommendState.Failed -> Notice(state.reason, onRetry)
            is RecommendState.Success -> LazyColumn(
                verticalArrangement = Arrangement.spacedBy(App.space.lg)
            ) {
                items(state.candidates, key = {
                    "${it.place.name}@${it.place.lat},${it.place.lon}"
                }) { CandidateCard(it, onPick) }
            }
        }
    }
}

@Composable
private fun Notice(message: String, onRetry: () -> Unit) {
    Text(message, style = App.type.stopTitle)
    AppButton("다시 찾기", onRetry)
}

@Composable
private fun CandidateCard(c: StopCandidate, onPick: (StopPlace) -> Unit) {
    AppCard(Modifier.fillMaxWidth()) {
        // DESIGN.md 3절: 정차 중 제목 32sp, 본문·보조 24sp. 상태는 아이콘+문구+색 3중.
        Text(c.place.name, style = App.type.stopTitle)
        c.met.forEach { StatusChip(Status.Ok, "${it.label} 충족") }
        c.missed.forEach { StatusChip(Status.Danger, "${it.label} 미충족") }
        Text(c.reason)
        Text("도착 시간 +${c.extraMinutes}분")
        Text("우회 ${c.detourKm}km")
        Text(if (c.source == DataSource.REAL) "출처: 실제 데이터" else "출처: 가상 데이터(시연)")
        AppButton("경유지로 추가", { onPick(c.place) })
    }
}
