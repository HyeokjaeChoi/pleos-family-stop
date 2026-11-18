package ai.familystop.feature.input

import ai.familystop.core.Destination
import ai.familystop.core.DestinationKind
import ai.familystop.core.RouteEta
import ai.familystop.core.designsystem.App
import ai.familystop.core.designsystem.AppButton
import ai.familystop.core.designsystem.AppIcons
import ai.familystop.core.designsystem.AppStepChip
import ai.familystop.core.designsystem.AppToggleChip
import ai.familystop.core.designsystem.AppToggleTile
import ai.familystop.core.designsystem.ProgressBar
import ai.familystop.core.designsystem.SelectRow
import ai.familystop.core.designsystem.Status
import ai.familystop.core.designsystem.StatusChip
import ai.familystop.core.designsystem.Stepper
import ai.familystop.core.designsystem.TimeWheel
import ai.familystop.core.formatClock
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import java.time.LocalDateTime

private val Gap = Arrangement.spacedBy(App.space.lg) // 터치 대상 간격 24dp 이상

/** 목적지 목록을 불러오는 상태. */
sealed interface DestinationsState {
    data object Loading : DestinationsState

    /** 호출 실패 또는 비어 있음 → 안내 + 내비 열기. */
    data object Unavailable : DestinationsState

    data class Ready(val list: List<Destination>) : DestinationsState
}

/** 상단 요약 칩 줄(Figma 요약 칩): 진행 표시이자 되돌아가기. 현재 단계의 칩은 강조한다. */
@Composable
fun PlanSummaryBar(
    draft: TripDraft,
    destinations: DestinationsState,
    step: PlanStep,
    onStep: (PlanStep) -> Unit,
    modifier: Modifier = Modifier
) {
    val chips = remember(draft, destinations) {
        summaryChips(draft, (destinations as? DestinationsState.Ready)?.list.orEmpty())
    }
    Row(modifier.fillMaxWidth(), horizontalArrangement = Gap) {
        chips.forEach { chip ->
            AppStepChip(chip.text(), current = chip.step == step, filled = chip.filled) {
                onStep(chip.step)
            }
        }
    }
}

/** 1단계: 목적지 목록(Figma 단계 1). 타이핑 없이 내비 목록에서 고르고 "다음"으로 넘어간다. */
@Composable
fun DestinationStep(
    destinations: DestinationsState,
    draft: TripDraft,
    onDraft: (TripDraft) -> Unit,
    onRetry: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ready = draft.destinationKey != null
    Column(modifier, verticalArrangement = Gap) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Section(stringResource(R.string.input_where)) {
                Destinations(destinations, draft, onDraft, onRetry)
            }
        }
        if (!ready) StatusChip(Status.Unknown, stringResource(R.string.input_choose_destination))
        AppButton(
            stringResource(R.string.input_next),
            onNext,
            Modifier.fillMaxWidth(),
            enabled = ready
        )
    }
}

/**
 * 2단계: 도착 시각·필요한 것·충전(Figma 단계 2). 시각은 휠로 정하고, 둘 다 정해야 "후보 보기"가 켜진다.
 * 초안은 호출자가 들고 있어(주행 중에도 보존) 바뀔 때마다 [onDraft]로 올린다.
 *
 * @param eta 내비가 안내 중인 경로의 남은 시간. null이면 [etaFailed]에 따라 폼 안에 오류 칩을 보인다.
 * @param autoSocPercent 차량이 준 잔량. null이면 수동 입력.
 */
@Composable
fun ConditionsStep(
    destinations: DestinationsState,
    eta: RouteEta?,
    etaFailed: Boolean,
    autoSocPercent: Int?,
    draft: TripDraft,
    onDraft: (TripDraft) -> Unit,
    onNext: () -> Unit,
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
    val timeSet = draft.hour24 != null
    val ready = timeSet && draft.needs.isNotEmpty()
    Column(modifier, verticalArrangement = Gap) {
        Row(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
            horizontalArrangement = Gap
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Gap) {
                Section(stringResource(R.string.input_arrival_title)) {
                    if (eta == null && etaFailed) {
                        StatusChip(Status.Warn, stringResource(R.string.input_eta_failed))
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
                    if (!timeSet) {
                        AppButton(
                            stringResource(R.string.input_set_this_time),
                            { onDraft(draft.copy(hour24 = shown, minute = shownMinute)) },
                            primary = false
                        )
                    }
                }
            }
            Column(Modifier.weight(1.2f), verticalArrangement = Gap) {
                Section(stringResource(R.string.input_needs_title)) {
                    // Figma Tiles: 같은 폭·높이 3등분
                    Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Gap) {
                        StopNeed.entries.forEach { n ->
                            AppToggleTile(
                                n in draft.needs,
                                stringResource(n.labelRes()),
                                n.icon(),
                                Modifier.weight(1f).fillMaxHeight()
                            ) { onDraft(draft.copy(needs = draft.needs.toggle(n))) }
                        }
                    }
                }
                Section(stringResource(R.string.input_charge_title)) {
                    Charge(draft, autoSocPercent, onDraft)
                }
            }
        }
        if (!timeSet) StatusChip(Status.Unknown, stringResource(R.string.input_set_arrival))
        if (draft.needs.isEmpty()) {
            StatusChip(
                Status.Unknown,
                stringResource(R.string.input_choose_needs)
            )
        }
        AppButton(
            stringResource(R.string.input_see_candidates),
            onNext,
            Modifier.fillMaxWidth(),
            enabled = ready
        )
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(App.space.md)) {
        Text(title, style = App.type.stopBody, color = App.colors.textSecondary)
        content()
    }
}

@Composable
private fun SummaryChip.text(): String {
    val unset = stringResource(R.string.input_unset)
    return when (this) {
        is SummaryChip.Destination ->
            stringResource(R.string.input_chip_destination, name ?: unset)
        is SummaryChip.Arrival -> {
            val locale = LocalConfiguration.current.locales[0]
            stringResource(
                R.string.input_chip_arrival,
                time?.let { formatClock(it.hour, it.minute, locale) } ?: unset
            )
        }
        is SummaryChip.Needs -> {
            val sep = stringResource(R.string.input_needs_separator)
            val list = needs.map { stringResource(it.labelRes()) }.joinToString(sep)
            stringResource(R.string.input_chip_needs, list.ifEmpty { unset })
        }
    }
}

/** 필요 항목의 화면 이름. 다른 화면(충전 활동)이 같은 이름을 쓰도록 공개한다. */
@Composable
fun StopNeed.label(): String = stringResource(labelRes())

private fun StopNeed.labelRes() = when (this) {
    StopNeed.MEAL -> R.string.input_need_meal
    StopNeed.RESTROOM -> R.string.input_need_restroom
    StopNeed.KIDS_REST -> R.string.input_need_kids_rest
}

private fun ChargeNeed.labelRes() = when (this) {
    ChargeNeed.YES -> R.string.input_charge_yes
    ChargeNeed.NO -> R.string.input_charge_no
    ChargeNeed.UNDECIDED -> R.string.input_charge_undecided
}

private fun DestinationKind.labelRes() = when (this) {
    DestinationKind.CURRENT -> R.string.input_kind_current
    DestinationKind.HOME -> R.string.input_kind_home
    DestinationKind.OFFICE -> R.string.input_kind_office
    DestinationKind.RECENT -> R.string.input_kind_recent
}

@Composable
private fun Destinations(
    state: DestinationsState,
    draft: TripDraft,
    onDraft: (TripDraft) -> Unit,
    onRetry: () -> Unit
) {
    when (state) {
        DestinationsState.Loading -> Text(
            stringResource(R.string.input_loading),
            style = App.type.stopBody
        )
        DestinationsState.Unavailable -> {
            StatusChip(Status.Danger, stringResource(R.string.input_load_failed))
            AppButton(stringResource(R.string.input_reload), onRetry, primary = false)
        }
        is DestinationsState.Ready -> Column(
            verticalArrangement = Arrangement.spacedBy(App.space.md)
        ) {
            state.list.forEach { d ->
                val key = destinationKey(d)
                SelectRow(
                    title = d.name,
                    subtitle = stringResource(d.kind.labelRes()),
                    selected = key == draft.destinationKey,
                    onClick = { onDraft(draft.copy(destinationKey = key)) }
                )
            }
        }
    }
}

@Composable
private fun Charge(draft: TripDraft, auto: Int?, onDraft: (TripDraft) -> Unit) {
    Row(horizontalArrangement = Gap) {
        ChargeNeed.entries.forEach { c ->
            AppToggleChip(
                draft.chargeNeed == c,
                stringResource(c.labelRes()),
                role = Role.RadioButton
            ) {
                onDraft(draft.copy(chargeNeed = c))
            }
        }
    }
    val soc = resolveSoc(auto, draft.manualSoc)
    val percent = soc.percentOrNull
    if (percent != null) ProgressBar(percent / 100f)
    Text(
        when (soc) {
            is Soc.Auto -> stringResource(R.string.input_soc_auto, soc.percent)
            is Soc.Manual -> stringResource(R.string.input_soc_manual, soc.percent)
            Soc.Unknown -> stringResource(R.string.input_soc_unknown)
        },
        style = App.type.stopBody,
        color = App.colors.textSecondary
    )
    if (auto == null) {
        val m = draft.manualSoc
        Stepper(
            value = m?.let { stringResource(R.string.input_soc_value, it) }
                ?: stringResource(R.string.input_soc_unset),
            onMinus = { onDraft(draft.copy(manualSoc = stepSoc(m, -1))) },
            onPlus = { onDraft(draft.copy(manualSoc = stepSoc(m, 1))) },
            modifier = Modifier.fillMaxWidth(),
            minusLabel = stringResource(R.string.input_soc_minus, SOC_STEP),
            plusLabel = stringResource(R.string.input_soc_plus, SOC_STEP)
        )
    } else if (socMismatch(auto, draft.manualSoc)) {
        Text(stringResource(R.string.input_soc_mismatch, draft.manualSoc ?: 0))
    }
}

private fun StopNeed.icon() = when (this) {
    StopNeed.MEAL -> AppIcons.Restaurant
    StopNeed.RESTROOM -> AppIcons.Wc
    StopNeed.KIDS_REST -> AppIcons.Face
}

private fun Set<StopNeed>.toggle(n: StopNeed) = if (n in this) this - n else this + n
