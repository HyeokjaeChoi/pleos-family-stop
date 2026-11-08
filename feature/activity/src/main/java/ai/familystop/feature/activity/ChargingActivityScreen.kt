package ai.familystop.feature.activity

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDateTime

private val Touch = 76.dp // DESIGN.md 4절 touch/min
private val Body = 24.sp // stop/body
private val Gap = Arrangement.spacedBy(24.dp)

/**
 * 충전 중 가족 활동 안내 (F-LFACHH). 정차(충전) 중에만 후보를 보인다. 주행 중·미확인은 안내만(ADR-0003, 0004).
 *
 * @param parked 정차 확인됨. 미확인이면 false.
 * @param chargeSessionId 충전 세션 식별자. 바뀌면 이전 시작 확인·직접 입력값을 버린다.
 * @param autoCharging 차량이 충전 시작을 확인함. false면 사용자가 직접 확인한다(자동 아님).
 * @param autoChargeEndsAt 차량이 준 충전 종료 예상. null이면 사용자 입력(자동 아님).
 * @param now 현재 시각. 호출자가 갱신해 넘기면 재계산된다.
 */
@Composable
fun ChargingActivityScreen(
    parked: Boolean,
    candidates: List<ActivityCandidate>,
    chargeSessionId: String,
    autoCharging: Boolean,
    autoChargeEndsAt: LocalDateTime?,
    now: LocalDateTime,
    modifier: Modifier = Modifier
) {
    var started by rememberSaveable(chargeSessionId) { mutableStateOf(false) }
    var manualEndsAt by rememberSaveable(chargeSessionId) { mutableStateOf<LocalDateTime?>(null) }
    var minutesText by rememberSaveable(chargeSessionId) { mutableStateOf("") }

    ProvideTextStyle(MaterialTheme.typography.bodyLarge.copy(fontSize = Body, lineHeight = 34.sp)) {
        if (!parked) {
            Text("정차 후 이용할 수 있어요.", modifier.fillMaxSize().padding(24.dp))
            return@ProvideTextStyle
        }
        // 충전 시작이 자동 확인이 아니면 종료 예상도 보장하지 않는다.
        val charge =
            resolveCharge(autoChargeEndsAt, manualEndsAt).let {
                if (!autoCharging && it is ChargeInfo.Auto) ChargeInfo.Manual(it.endsAt) else it
            }

        Column(
            modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Gap
        ) {
            if (autoChargeEndsAt == null) {
                Text("충전 정보를 차량에서 받지 못했어요(자동 확인 아님). 충전이 끝나기까지 남은 시간을 직접 입력해 주세요.")
                OutlinedTextField(
                    minutesText,
                    {
                        minutesText = it.filter(Char::isDigit).take(3)
                        manualEndsAt = manualEnd(minutesText, now)
                    },
                    label = { Text("충전 종료까지 남은 분 (직접 입력)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().heightIn(min = Touch)
                )
            } else {
                Text("충전 종료 예상은 차량에서 자동 확인했어요.")
            }

            if (!autoCharging && !started) {
                Text("충전 시작을 확인하지 못했어요(자동 확인 아님). 충전을 시작했다면 눌러 주세요.")
                Button(
                    onClick = { started = true },
                    modifier = Modifier.fillMaxWidth().heightIn(min = Touch)
                ) { Text("충전 시작했어요") }
                return@Column
            }
            if (!autoCharging) Text("충전 시작은 직접 확인한 값이에요(자동 확인 아님).")

            if (candidates.isEmpty()) Text("주변에 확인된 식사·간식·휴식 후보가 없어요.")
            candidates.forEach { c ->
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "${c.kind.label} · ${c.name}",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 32.sp,
                            lineHeight = 44.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text("왕복 ${c.roundTripMin}분 · 활동 ${c.activityMin}분")
                    Text(assess(c, charge, now).message())
                }
            }
        }
    }
}
