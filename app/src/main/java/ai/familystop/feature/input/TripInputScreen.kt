package ai.familystop.feature.input

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDateTime

private val Touch = 76.dp // DESIGN.md 4절 touch/min
private val Body = 24.sp // stop/body

/**
 * 가족 여행 조건·정차 필요 입력 + 에너지 수동 입력 (F-GFBDZR, F-QOZAMV).
 * 정차 중에만 입력 UI를 보인다. 주행 중(또는 미확인, ADR-0004)엔 초안만 보존하고 안내만 표시.
 *
 * @param parked 정차 확인됨. 미확인이면 false로 넘긴다.
 * @param autoSocPercent 차량이 준 잔량. null이면 미수신 → 수동 입력.
 */
@Composable
fun TripInputScreen(
    parked: Boolean,
    autoSocPercent: Int?,
    onSubmit: (TripInput) -> Unit,
    modifier: Modifier = Modifier,
    now: () -> LocalDateTime = { LocalDateTime.now() }
) {
    // 화면이 주행 모드로 바뀌어도 초안이 남도록 컴포지션 밖 상태(saveable)로 둔다.
    var destination by rememberSaveable { mutableStateOf("") }
    var timeText by rememberSaveable { mutableStateOf("") }
    var needs by rememberSaveable { mutableStateOf(emptyList<StopNeed>()) }
    var urgent by rememberSaveable { mutableStateOf(false) }
    var chargeNeed by rememberSaveable { mutableStateOf(ChargeNeed.UNDECIDED) }
    var manualSoc by rememberSaveable { mutableStateOf("") }
    var errors by remember { mutableStateOf(emptyList<InputError>()) }

    if (!parked) {
        Text(
            "정차 후 이용할 수 있어요. 입력하던 내용은 그대로 남아 있어요.",
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = Body),
            modifier = modifier.fillMaxSize().padding(24.dp)
        )
        return
    }

    val draft = TripDraft(destination, timeText, needs.toSet(), urgent, chargeNeed, manualSoc)
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        OutlinedTextField(
            destination,
            { destination = it },
            label = { Text("목적지") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            timeText,
            { timeText = it },
            label = { Text("도착 희망 시간 (예: 13:30)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Text("정차에서 필요한 것", fontSize = Body)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            StopNeed.entries.forEach { n ->
                FilterChip(
                    selected = n in needs,
                    onClick = { needs = if (n in needs) needs - n else needs + n },
                    label = { Text((if (n in needs) "✓ " else "") + n.label, fontSize = Body) },
                    modifier = Modifier.heightIn(min = Touch)
                )
            }
            FilterChip(
                selected = urgent,
                onClick = { urgent = !urgent },
                label = { Text((if (urgent) "✓ " else "") + "급해요", fontSize = Body) },
                modifier = Modifier.heightIn(min = Touch)
            )
        }

        Text("충전", fontSize = Body)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            ChargeNeed.entries.forEach { c ->
                FilterChip(
                    selected = chargeNeed == c,
                    onClick = { chargeNeed = c },
                    label = {
                        Text(
                            (
                                if (chargeNeed ==
                                    c
                                ) {
                                    "✓ "
                                } else {
                                    ""
                                }
                                ) + c.label,
                            fontSize = Body
                        )
                    },
                    modifier = Modifier.heightIn(min = Touch)
                )
            }
        }

        if (autoSocPercent != null) {
            Text("배터리 $autoSocPercent% · 차량에서 자동 확인", fontSize = Body)
        } else {
            Text("배터리 잔량을 차량에서 받지 못했어요. 직접 입력하면 '직접 입력(자동 아님)'으로 표시돼요.", fontSize = Body)
            OutlinedTextField(
                manualSoc,
                { manualSoc = it.filter(Char::isDigit).take(3) },
                label = { Text("배터리 잔량 % (직접 입력)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            if (manualSoc.isBlank()) Text("잔량 미확인 · 도달 가능 여부는 알 수 없어요", fontSize = Body)
        }
        // 차량값이 나중에 도착해 수동 입력과 다를 때(자동값이 우선 적용됨).
        if (socMismatch(autoSocPercent, manualSoc.toIntOrNull())) {
            Text("직접 입력한 값과 차량 데이터가 달라요. 차량 값을 사용해요.", fontSize = Body)
        }

        errors.forEach {
            Text(it.message, fontSize = Body, color = MaterialTheme.colorScheme.error)
        }

        Button(
            onClick = {
                when (val r = validate(draft, autoSocPercent, now())) {
                    is ValidationResult.Ok -> {
                        errors = emptyList()
                        onSubmit(r.input)
                    }
                    is ValidationResult.Invalid -> errors = r.errors
                }
            },
            modifier = Modifier.fillMaxWidth().heightIn(min = Touch)
        ) { Text("정차 추천 받기", fontSize = Body) }
    }
}
