package ai.familystop.feature.input

import ai.familystop.core.Destination
import ai.familystop.core.DestinationKind
import ai.familystop.core.RouteEta
import ai.familystop.core.designsystem.App
import ai.familystop.core.designsystem.AppButton
import ai.familystop.core.designsystem.AppToggleChip
import ai.familystop.core.designsystem.ProgressBar
import ai.familystop.core.designsystem.SelectRow
import ai.familystop.core.designsystem.Status
import ai.familystop.core.designsystem.StatusChip
import ai.familystop.core.designsystem.Stepper
import ai.familystop.core.designsystem.TimeWheel
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import java.time.LocalDateTime

private val DEST_MAX_HEIGHT @Composable get() = App.touchMin * 4
private val Gap @Composable get() = Arrangement.spacedBy(App.space.lg) // 터치 대상 간격 24dp 이상

/** 목적지 목록을 불러오는 상태. */
sealed interface DestinationsState {
    data object Loading : DestinationsState

    /** 호출 실패 또는 비어 있음 → 안내 + 내비 열기. */
    data object Unavailable : DestinationsState

    data class Ready(val list: List<Destination>) : DestinationsState
}

/**
 * 입력 창(Figma 27:620 왼쪽). 목적지는 내비 목록에서 고르고, 시각은 휠로 정한다(타이핑 없음, #65).
 * 초안은 호출자가 들고 있어(주행 중에도 보존) 바뀔 때마다 [onDraft]로 올린다.
 *
 * @param eta 내비가 안내 중인 경로의 남은 시간. null이면 [etaFailed]에 따라 폼 안에 오류 칩을 보인다.
 * @param autoSocPercent 차량이 준 잔량. null이면 수동 입력.
 */
@Composable
fun TripInputPane(
    destinations: DestinationsState,
    eta: RouteEta?,
    etaFailed: Boolean,
    autoSocPercent: Int?,
    draft: TripDraft,
    onDraft: (TripDraft) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    now: () -> LocalDateTime = { LocalDateTime.now() }
) {
    // 내비 예상 도착 시각이 있으면 시각 초기값으로(이미 정했으면 건드리지 않는다).
    LaunchedEffect(eta, draft.destinationKey) {
        // 안내 중인 목적지의 ETA라서, 고른 목적지가 그것일 때만 초기값으로 쓴다.
        val picked = (destinations as? DestinationsState.Ready)?.list
            ?.firstOrNull { destinationKey(it) == draft.destinationKey }
        if (draft.hour24 == null && picked?.name == eta?.destinationName) {
            etaTime(eta, now())?.let { onDraft(draft.copy(hour24 = it.hour, minute = it.minute)) }
        }
    }
    Column(modifier, verticalArrangement = Gap) {
        // 목적지는 3행 높이까지만 보이고 안에서 스크롤: 시각·필요 사항이 한 화면에 함께 보이게(Figma 27:620).
        Section("목적지") {
            Column(
                Modifier.heightIn(max = DEST_MAX_HEIGHT).verticalScroll(rememberScrollState())
            ) { Destinations(destinations, draft, onDraft, onRetry) }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Gap) {
            Column(Modifier.weight(1f), verticalArrangement = Gap) {
                Section("도착 희망 시각") {
                    if (eta == null && etaFailed) {
                        StatusChip(Status.Warn, "내비 예상 도착 시각을 받지 못했어요. 직접 정해 주세요.")
                    }
                    val shown = draft.hour24 ?: now().hour
                    val shownMinute = if (draft.hour24 == null) now().minute else draft.minute
                    TimeWheel(
                        hour = shown,
                        minute = shownMinute,
                        onChange = { h, m -> onDraft(draft.copy(hour24 = h, minute = m)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    // 휠이 보여 주는 값은 아직 정한 값이 아니다 → 그대로 쓰려면 직접 확정한다.
                    if (draft.hour24 == null) {
                        AppButton(
                            "이 시각으로 정하기",
                            { onDraft(draft.copy(hour24 = shown, minute = shownMinute)) },
                            primary = false
                        )
                    }
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Gap) {
                Section("정차에서 필요한 것") {
                    FlowRow(horizontalArrangement = Gap, verticalArrangement = Gap) {
                        StopNeed.entries.forEach { n ->
                            AppToggleChip(n in draft.needs, n.label) {
                                onDraft(
                                    draft.copy(
                                        needs = if (n in
                                            draft.needs
                                        ) {
                                            draft.needs - n
                                        } else {
                                            draft.needs + n
                                        }
                                    )
                                )
                            }
                        }
                    }
                    SwitchRow("급해요", draft.urgent) { onDraft(draft.copy(urgent = it)) }
                }
                Section("충전") { Charge(draft, autoSocPercent, onDraft) }
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(App.space.md)) {
        Text(title, style = App.type.stopBody, color = App.colors.textSecondary)
        content()
    }
}

private fun Destination.kindLabel() = when (kind) {
    DestinationKind.CURRENT -> "내비에서 안내 중"
    DestinationKind.HOME -> "집"
    DestinationKind.OFFICE -> "회사"
    DestinationKind.RECENT -> "최근 목적지"
}

@Composable
private fun Destinations(
    state: DestinationsState,
    draft: TripDraft,
    onDraft: (TripDraft) -> Unit,
    onRetry: () -> Unit
) {
    when (state) {
        DestinationsState.Loading -> Text("목적지를 불러오는 중…", style = App.type.stopBody)
        DestinationsState.Unavailable -> {
            StatusChip(Status.Danger, "내비에서 목적지를 불러오지 못했어요")
            AppButton("다시 불러오기", onRetry, primary = false)
        }
        is DestinationsState.Ready -> Column(
            verticalArrangement = Arrangement.spacedBy(App.space.md)
        ) {
            state.list.forEach { d ->
                val key = destinationKey(d)
                SelectRow(
                    title = d.name,
                    subtitle = d.kindLabel(),
                    selected = key == draft.destinationKey,
                    onClick = { onDraft(draft.copy(destinationKey = key)) }
                )
            }
        }
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val c = App.colors
    Row(
        Modifier.fillMaxWidth().heightIn(min = App.touchMin)
            .toggleable(checked, role = Role.Switch, onValueChange = onChange),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = App.type.stopTitle, color = c.textPrimary)
        Switch(
            checked,
            null,
            colors = SwitchDefaults.colors(
                checkedThumbColor = c.accentOn,
                checkedTrackColor = c.accentPrimary,
                uncheckedThumbColor = c.textSecondary,
                uncheckedTrackColor = c.bgRaised,
                uncheckedBorderColor = c.bgRaised
            )
        )
    }
}

@Composable
private fun Charge(draft: TripDraft, auto: Int?, onDraft: (TripDraft) -> Unit) {
    Row(horizontalArrangement = Gap) {
        ChargeNeed.entries.forEach { c ->
            AppToggleChip(draft.chargeNeed == c, c.label, role = Role.RadioButton) {
                onDraft(draft.copy(chargeNeed = c))
            }
        }
    }
    val soc = resolveSoc(auto, draft.manualSoc)
    val percent = when (soc) {
        is Soc.Auto -> soc.percent
        is Soc.Manual -> soc.percent
        Soc.Unknown -> null
    }
    if (percent != null) ProgressBar(percent / 100f)
    Text(
        when (soc) {
            is Soc.Auto -> "배터리 ${soc.percent}% · 차량에서 자동 확인"
            is Soc.Manual -> "배터리 ${soc.percent}% · 직접 정함(자동 아님)"
            Soc.Unknown -> "잔량 미확인 · 도달 가능 여부는 알 수 없어요"
        },
        style = App.type.stopBody,
        color = App.colors.textSecondary
    )
    if (auto == null) {
        val m = draft.manualSoc
        Stepper(
            value = m?.let { "$it%" } ?: "미정",
            onMinus = { onDraft(draft.copy(manualSoc = ((m ?: 50) - SOC_STEP).coerceAtLeast(0))) },
            onPlus = { onDraft(draft.copy(manualSoc = ((m ?: 50) + SOC_STEP).coerceAtMost(100))) },
            modifier = Modifier.fillMaxWidth(),
            minusLabel = "배터리 ${SOC_STEP}% 줄이기",
            plusLabel = "배터리 ${SOC_STEP}% 늘리기"
        )
    } else if (socMismatch(auto, draft.manualSoc)) {
        Text("직접 정한 값(${draft.manualSoc}%)과 차량 데이터가 달라요. 차량 값을 사용해요.")
    }
}

private const val SOC_STEP = 5
