package ai.familystop.feature.metrics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Touch = 76.dp // DESIGN.md 4절 touch/min

/**
 * 측정 동의·철회 화면(공개 composable 1개). 정차 중에만 보인다(주행·미확인은 안내만, ADR-0004).
 * 동의하지 않아도 앱 기능은 그대로 쓴다.
 *
 * 운전자 확인은 화면의 자기 선언이다. SDK에 운전자 식별값이 없어 동승자 탭을 기술적으로
 * 막을 수 없다(한계). 선언 상태는 [tripId]에 묶여 새 여행에서 다시 묻는다.
 *
 * @param parked 정차 확인됨. 미확인이면 false.
 * @param tripId 이번 여행 표식([MetricsCollector.startTrip]).
 * @param consented 이미 동의했으면 true → 철회 화면을 보인다.
 * @param onResult 동의 true / 거절 false. true는 운전자 선언 뒤에만 호출되므로
 *   호출 측은 `grantConsent(id, isDriver = true)`로 연결한다.
 * @param onWithdraw 철회. 호출 측은 [MetricsCollector.withdraw]로 연결한다.
 */
@Composable
fun ConsentScreen(
    parked: Boolean,
    tripId: String,
    consented: Boolean,
    onResult: (consent: Boolean) -> Unit,
    onWithdraw: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isDriver by rememberSaveable(tripId) { mutableStateOf(false) }
    ProvideTextStyle(MaterialTheme.typography.bodyLarge.copy(fontSize = 24.sp)) {
        if (!parked) {
            Text("정차 후에 확인할 수 있어요.", modifier.fillMaxSize().padding(24.dp))
            return@ProvideTextStyle
        }
        Column(
            modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            if (consented) {
                Text("측정 동의를 철회하면 이번 여행 기록이 모두 지워져요.")
                OutlinedButton(
                    onClick = onWithdraw,
                    modifier = Modifier.fillMaxWidth().heightIn(min = Touch)
                ) { Text("동의 철회하고 기록 삭제") }
                return@Column
            }
            Text("이번 여행의 정차 도움 정도를 기록해도 될까요?")
            Text(
                "계획 시간·선택 이유·남은 필요를 이 기기에만 저장해요. " +
                    "이름·계정과 연결하지 않고, 철회하면 이번 여행 기록이 지워져요. " +
                    "동의하지 않아도 모든 기능을 쓸 수 있어요."
            )
            FilterChip(
                selected = isDriver,
                onClick = { isDriver = !isDriver },
                label = { Text((if (isDriver) "✓ " else "") + "저는 운전자 본인이에요") },
                modifier = Modifier.heightIn(min = Touch)
            )
            Button(
                onClick = { onResult(true) },
                enabled = isDriver,
                modifier = Modifier.fillMaxWidth().heightIn(min = Touch)
            ) { Text("동의하고 기록") }
            OutlinedButton(
                onClick = { onResult(false) },
                modifier = Modifier.fillMaxWidth().heightIn(min = Touch)
            ) { Text("기록하지 않을게요") }
        }
    }
}
