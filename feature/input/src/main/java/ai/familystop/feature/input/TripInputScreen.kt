package ai.familystop.feature.input

import ai.familystop.core.designsystem.App
import ai.familystop.core.designsystem.AppButton
import ai.familystop.core.designsystem.AppTextField
import ai.familystop.core.designsystem.AppToggleChip
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import java.time.LocalDateTime

private val Gap @Composable get() = Arrangement.spacedBy(App.space.lg) // 터치 대상 간격 24dp 이상

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
    var d by rememberSaveable { mutableStateOf(TripDraft()) }
    var submitted by rememberSaveable { mutableStateOf(false) }

    if (!parked) {
        Text(
            "정차 후 이용할 수 있어요. 입력하던 내용은 그대로 남아 있어요.",
            modifier.fillMaxSize().padding(App.space.lg)
        )
        return
    }

    // 첫 제출 이후엔 입력할 때마다 다시 검증해, 고친 항목의 오류가 바로 사라지게 한다.
    val errors =
        if (submitted) {
            (
                validate(
                    d,
                    autoSocPercent,
                    now()
                ) as? ValidationResult.Invalid
                )?.errors.orEmpty()
        } else {
            emptyList()
        }
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(App.space.lg),
        verticalArrangement = Gap
    ) {
        AppTextField(d.destination, { d = d.copy(destination = it) }, "목적지")
        AppTextField(d.timeText, { d = d.copy(timeText = it) }, "도착 희망 시간 (예: 13:30)")

        Text("정차에서 필요한 것", style = App.type.stopTitle)
        FlowRow(horizontalArrangement = Gap, verticalArrangement = Gap) {
            StopNeed.entries.forEach { n ->
                AppToggleChip(n in d.needs, n.label) {
                    d = d.copy(needs = if (n in d.needs) d.needs - n else d.needs + n)
                }
            }
            AppToggleChip(d.urgent, "급해요") { d = d.copy(urgent = !d.urgent) }
        }

        Text("충전", style = App.type.stopTitle)
        FlowRow(horizontalArrangement = Gap, verticalArrangement = Gap) {
            ChargeNeed.entries.forEach { c ->
                AppToggleChip(d.chargeNeed == c, c.label) {
                    d =
                        d.copy(chargeNeed = c)
                }
            }
        }

        // 범위 밖 값은 상태로 표시하지 않는다
        val manual = d.manualSocText.toIntOrNull()?.takeIf { it in 0..100 }
        if (autoSocPercent == null) {
            Text("배터리 잔량을 차량에서 받지 못했어요. 직접 입력하면 '직접 입력(자동 아님)'으로 표시돼요.")
            AppTextField(
                d.manualSocText,
                { d = d.copy(manualSocText = it.filter(Char::isDigit).take(3)) },
                "배터리 잔량 % (직접 입력)"
            )
        }
        Text(
            when (val soc = resolveSoc(autoSocPercent, manual)) {
                is Soc.Auto -> "배터리 ${soc.percent}% · 차량에서 자동 확인"
                is Soc.Manual -> "배터리 ${soc.percent}% · 직접 입력(자동 아님)"
                Soc.Unknown -> "잔량 미확인 · 도달 가능 여부는 알 수 없어요"
            }
        )
        // 차량값이 나중에 도착해 수동 입력과 다를 때(자동값이 우선 적용됨).
        if (socMismatch(autoSocPercent, manual)) {
            Text("직접 입력한 값($manual%)과 차량 데이터가 달라요. 차량 값을 사용해요.")
        }

        errors.forEach { Text(it.message, color = App.colors.statusDanger) }

        AppButton(
            "정차 추천 받기",
            {
                submitted = true
                (validate(d, autoSocPercent, now()) as? ValidationResult.Ok)?.let {
                    onSubmit(it.input)
                }
            },
            Modifier.fillMaxWidth()
        )
    }
}
