package ai.familystop.feature.recommend

import ai.familystop.core.DataSource
import ai.familystop.core.StopCandidate
import ai.familystop.core.StopPlace
import ai.familystop.core.designsystem.App
import ai.familystop.core.designsystem.AppButton
import ai.familystop.core.designsystem.AppIcons
import ai.familystop.core.designsystem.CompareRow
import ai.familystop.core.designsystem.Status
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * 정차 중 후보 비교 화면(스크롤 허용). 주행 중에는 이 화면을 쓰지 않고
 * [driveCards]를 큰 카드로 보여 준다(ADR-0003). 앱 셸이 주행/정차를 골라 호출한다.
 * 후보는 [CompareRow] 한 행씩, 추가 버튼은 선택된 행 기준으로 하나만 둔다(#50).
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
            is RecommendState.Success -> CompareList(state.candidates, onPick)
        }
    }
}

@Composable
private fun Notice(message: String, onRetry: () -> Unit) {
    Text(message, style = App.type.stopTitle)
    AppButton("다시 찾기", onRetry)
}

private fun StopCandidate.key() = "${place.name}@${place.lat},${place.lon}"

@Composable
private fun ColumnScope.CompareList(candidates: List<StopCandidate>, onPick: (StopPlace) -> Unit) {
    // Selection is remembered by key so a refreshed list keeps it when the candidate is still there.
    var selectedKey by rememberSaveable { mutableStateOf<String?>(null) }
    val selected = candidates.firstOrNull { it.key() == selectedKey } ?: candidates.first()
    val source = if (selected.source == DataSource.REAL) "실제 데이터" else "가상 데이터(시연)"
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("정차 후보 비교", style = App.type.stopTitle, modifier = Modifier.weight(1f))
        Text("출처: $source", style = App.type.stopBody, color = App.colors.textSecondary)
    }
    LazyColumn(
        Modifier.weight(1f).selectableGroup(),
        verticalArrangement = Arrangement.spacedBy(App.space.md)
    ) {
        items(candidates, key = { it.key() }) { c ->
            CompareRow(
                name = c.place.name,
                minutes = "+${c.extraMinutes}분",
                distance = "${c.detourKm}km",
                status = if (c.missed.isEmpty()) Status.Ok else Status.Danger,
                statusText = if (c.missed.isEmpty()) "필요 조건 충족" else "일부 미충족",
                selected = c.key() == selected.key(),
                modifier = Modifier.fillMaxWidth()
            ) { selectedKey = c.key() }
        }
    }
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(App.space.lg)
    ) {
        Column(Modifier.weight(1f)) {
            Text("선택: ${selected.place.name}", style = App.type.stopTitle)
            Text(detail(selected), style = App.type.stopBody, color = App.colors.textSecondary)
        }
        AppButton(
            "경유지로 추가",
            { onPick(selected.place) },
            Modifier.weight(1f),
            icon = AppIcons.AddLocationAlt
        )
    }
}

/** 선택한 후보의 충족·미충족 필요를 한 줄로(칩은 요약만 보여 주므로 여기서 구체화). */
private fun detail(c: StopCandidate): String =
    (c.met.map { "${it.label} 충족" } + c.missed.map { "${it.label} 미충족" })
        .joinToString(" · ").ifEmpty { c.reason }
