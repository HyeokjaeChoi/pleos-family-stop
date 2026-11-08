package ai.familystop.feature.recommend

import ai.familystop.core.DataSource
import ai.familystop.core.StopCandidate
import ai.familystop.core.StopPlace
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 정차 중 후보 비교 화면(스크롤 허용). 주행 중에는 이 화면을 쓰지 않고
 * [driveCards]를 큰 카드로 보여 준다(ADR-0003). 앱 셸이 주행/정차를 골라 호출한다.
 * ponytail: 색 토큰은 Material 기본값. :core:designsystem 토큰 연결은 통합 시.
 */
@Composable
fun RecommendScreen(
    state: RecommendState,
    onPick: (StopPlace) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        when (state) {
            RecommendState.Idle -> Unit
            RecommendState.Loading -> Text(
                "근처 시설을 찾는 중…",
                style = MaterialTheme.typography.titleLarge
            )
            RecommendState.Empty -> Notice("조건에 맞는 정차 후보가 없어요.", onRetry)
            is RecommendState.Failed -> Notice(state.reason, onRetry)
            is RecommendState.Success -> LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
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
    Text(message, style = MaterialTheme.typography.titleLarge)
    Button(onClick = onRetry, modifier = Modifier.heightIn(min = 76.dp)) { Text("다시 찾기") }
}

@Composable
private fun CandidateCard(c: StopCandidate, onPick: (StopPlace) -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            // DESIGN.md 3절: 정차 중 제목 32sp, 본문·보조 24sp. 상태는 아이콘+문구+색 3중.
            Text(c.place.name, fontSize = 32.sp, lineHeight = 44.sp, fontWeight = FontWeight.Bold)
            c.met.forEach {
                Text(
                    "✓ ${it.label} 충족",
                    fontSize = 24.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            c.missed.forEach {
                Text("✗ ${it.label} 미충족", fontSize = 24.sp, color = MaterialTheme.colorScheme.error)
            }
            Text(c.reason, fontSize = 24.sp)
            Text("도착 시간 +${c.extraMinutes}분", fontSize = 24.sp)
            Text("우회 ${c.detourKm}km", fontSize = 24.sp)
            Text(
                if (c.source ==
                    DataSource.REAL
                ) {
                    "출처: 실제 데이터"
                } else {
                    "출처: 가상 데이터(시연)"
                },
                fontSize = 24.sp
            )
            Button(onClick = { onPick(c.place) }, modifier = Modifier.heightIn(min = 76.dp)) {
                Text("경유지로 추가")
            }
        }
    }
}
