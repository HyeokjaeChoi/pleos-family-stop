package ai.familystop.feature.activity

import ai.familystop.core.EnergyState
import ai.familystop.core.designsystem.App
import ai.familystop.core.designsystem.AppCard
import ai.familystop.core.designsystem.AppIcons
import ai.familystop.core.designsystem.CheckRow
import ai.familystop.core.designsystem.ProgressBar
import ai.familystop.core.designsystem.Status
import ai.familystop.core.designsystem.StatusChip
import ai.familystop.core.formatClock
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import java.time.LocalDateTime

/**
 * 경유지 추가 후 보여 주는 여정. [arrival]은 내비 ETA에 정차 우회만 더한 도착 예상이고 충전 시간은
 * 들어 있지 않다(차량이 충전 완료 시각을 주지 않음, #79). 알 수 없으면 null → "미확인".
 */
data class Journey(
    val stopName: String,
    val stopDetail: String,
    val destinationName: String,
    val arrival: LocalDateTime?,
    val wishedArrival: LocalDateTime
)

private val Gap @Composable get() = Arrangement.spacedBy(App.space.lg)

private fun LocalDateTime.clock() = formatClock(hour, minute)

/**
 * 충전 중 화면(Figma v5 41:696 충전 중 · 41:773 충전 끝 · 41:850 정보 없음). 왼쪽 여정·할 일,
 * 오른쪽은 차량에서 받은 충전 정보만 보여 준다. 사용자가 충전 시간을 입력하지 않고, 충전 중이던
 * 상태가 꺼지면 자동으로 "충전 끝"이 된다. 충전 상태를 모르면 받지 못했다고만 표시한다(PL-UJFQXQ).
 *
 * @param energy 차량 에너지 상태. null = 아직 못 받음.
 * @param tasks 입력 화면에서 고른 필요 사항 이름. 비면 할 일 영역을 숨긴다.
 * @param sessionId 바뀌면 이전 충전 진행·체크를 버린다.
 */
@Composable
fun ChargingActivityScreen(
    journey: Journey,
    energy: EnergyState?,
    tasks: List<String>,
    sessionId: String,
    modifier: Modifier = Modifier
) {
    var sawCharging by rememberSaveable(sessionId) { mutableStateOf(false) }
    var done by rememberSaveable(sessionId) { mutableStateOf(emptySet<String>()) }
    LaunchedEffect(energy?.charging) { if (energy?.charging == true) sawCharging = true }
    val view = chargeView(energy, sawCharging || energy?.charging == true)

    Row(modifier.fillMaxSize().padding(App.space.lg), horizontalArrangement = Gap) {
        Column(
            Modifier.weight(1.3f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(App.space.lg)
        ) {
            Label("여정 (예상)")
            Timeline(journey, view)
            if (tasks.isNotEmpty()) {
                Label("정차에서 할 일 · ${tasks.count { it in done }}/${tasks.size}")
                AppCard(spacing = App.space.sm) {
                    tasks.forEach { t ->
                        CheckRow(t, t in done, { done = if (t in done) done - t else done + t })
                    }
                }
            }
        }
        Column(Modifier.weight(1f).fillMaxSize(), verticalArrangement = Gap) {
            Label("충전 정보")
            ChargeCard(view, energy)
        }
    }
}

@Composable
private fun ChargeCard(view: ChargeView, energy: EnergyState?) {
    AppCard(Modifier.fillMaxWidth()) {
        when (view) {
            ChargeView.UNAVAILABLE -> {
                StatusChip(Status.Unknown, "충전 정보를 받지 못했어요")
                Body("차량에서 충전 상태를 받지 못했어요. 충전기 화면에서 확인해 주세요.")
            }
            ChargeView.BEFORE -> {
                StatusChip(Status.Unknown, "충전 전")
                Battery(energy)
                Body("충전을 시작하면 자동으로 바뀌어요.")
            }
            ChargeView.CHARGING -> {
                StatusChip(Status.Ok, "충전 중")
                Battery(energy)
                rangeText(energy)?.let { Body(it) }
            }
            ChargeView.DONE -> {
                StatusChip(Status.Ok, "충전 끝")
                Text(
                    "출발해도 돼요. 내비 안내를 이어가세요.",
                    style = App.type.stopTitle,
                    color = App.colors.textPrimary
                )
                Battery(energy)
                rangeText(energy)?.let { Body(it) }
            }
        }
    }
}

/** 잔량 큰 숫자 + 막대. 잔량을 모르면 "배터리 미확인"만. */
@Composable
private fun Battery(energy: EnergyState?) {
    val soc = energy?.socPercent
    Text(
        if (soc != null) "배터리 $soc%" else "배터리 미확인",
        style = App.type.driveTitle,
        color = App.colors.textPrimary
    )
    if (soc != null) ProgressBar(soc / 100f)
}

/** 주행 가능 거리를 받았을 때만 한 줄. 못 받으면 줄을 숨긴다(미확인 값을 채우지 않음). */
private fun rangeText(energy: EnergyState?): String? =
    energy?.rangeKm?.let { "주행 가능 거리 ${it}km · 차량에서 받은 값이에요." }

@Composable
private fun Body(text: String) =
    Text(text, style = App.type.stopBody, color = App.colors.textSecondary)

@Composable
private fun Label(text: String) =
    Text(text, style = App.type.stopBody, color = App.colors.textSecondary)

@Composable
private fun Timeline(journey: Journey, view: ChargeView) {
    val arrival = journey.arrival
    AppCard(Modifier.fillMaxWidth(), spacing = App.space.lg) {
        Stop(AppIcons.Navigation, false, "현재 위치", journey.stopDetail)
        Stop(AppIcons.Bolt, true, journey.stopName, stopStatus(view))
        Column {
            Stop(
                AppIcons.Place,
                false,
                journey.destinationName,
                arrival?.let { "도착 예상 ${it.clock()} · 충전 시간 제외" } ?: "도착 예상 미확인"
            )
            Text(
                "도착 시 배터리 예상 · 미확인",
                style = App.type.stopBody,
                color = App.colors.textSecondary,
                modifier = Modifier.padding(start = MARKER + App.space.md)
            )
            val late = arrival?.let { lateMinutes(it, journey.wishedArrival) }
            if (late != null) {
                StatusChip(
                    Status.Warn,
                    "희망 도착 ${journey.wishedArrival.clock()}보다 ${late}분 늦어요",
                    Modifier.padding(start = MARKER + App.space.md)
                )
            }
        }
    }
}

private val MARKER @Composable get() = App.space.xl + App.space.lg

@Composable
private fun Stop(icon: ImageVector, accent: Boolean, title: String, sub: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(App.space.md)) {
        Box(
            Modifier.size(MARKER).clip(CircleShape)
                .background(if (accent) App.colors.accentPrimary else App.colors.bgRaised),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (accent) App.colors.accentOn else App.colors.textPrimary,
                modifier = Modifier.size(App.icon.secondary)
            )
        }
        Column {
            Text(title, style = App.type.stopTitle, color = App.colors.textPrimary)
            Text(sub, style = App.type.stopBody, color = App.colors.textSecondary)
        }
    }
}
