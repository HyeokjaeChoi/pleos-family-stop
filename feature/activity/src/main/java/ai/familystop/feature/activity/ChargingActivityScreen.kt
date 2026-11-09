package ai.familystop.feature.activity

import ai.familystop.core.designsystem.App
import ai.familystop.core.designsystem.AppButton
import ai.familystop.core.designsystem.AppCard
import ai.familystop.core.designsystem.AppIcons
import ai.familystop.core.designsystem.CheckRow
import ai.familystop.core.designsystem.ProgressBar
import ai.familystop.core.designsystem.Status
import ai.familystop.core.designsystem.StatusChip
import ai.familystop.core.designsystem.Stepper
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay

/**
 * 경유지 추가 후 보여 주는 여정. [baseArrival]은 충전 시간을 빼고 정차 우회만 더한 도착 예상(내비 ETA 기반),
 * 알 수 없으면 null → 도착 시각은 "미확인". 충전 시간은 화면에서 정한다.
 */
data class Journey(
    val stopName: String,
    val stopDetail: String,
    val destinationName: String,
    val baseArrival: LocalDateTime?,
    val wishedArrival: LocalDateTime
)

private val HHMM = DateTimeFormatter.ofPattern("HH:mm")
private val Gap @Composable get() = Arrangement.spacedBy(App.space.lg)

private fun LocalDateTime.epoch() = atZone(ZoneId.systemDefault()).toEpochSecond()

private fun epochToLocal(s: Long) =
    LocalDateTime.ofInstant(Instant.ofEpochSecond(s), ZoneId.systemDefault())

/**
 * 충전 중 화면(Figma 30:936 입력 → 35:656 카운트다운). 왼쪽 여정·할 일, 오른쪽은 충전 시간 설정 → 카운트다운.
 * 충전 정보는 차량에서 받지 못하므로 항상 "직접 정한 값"으로 표시한다(PL-UJFQXQ).
 *
 * @param tasks 입력 화면에서 고른 필요 사항 이름. 비면 할 일 영역을 숨긴다.
 * @param sessionId 바뀌면 이전 충전 설정·체크를 버린다.
 */
@Composable
fun ChargingActivityScreen(
    journey: Journey,
    tasks: List<String>,
    sessionId: String,
    modifier: Modifier = Modifier,
    now: () -> LocalDateTime = { LocalDateTime.now() }
) {
    var minutes by rememberSaveable(sessionId) { mutableStateOf(DEFAULT_MINUTES) }
    var startEpoch by rememberSaveable(sessionId) { mutableStateOf<Long?>(null) }
    var endEpoch by rememberSaveable(sessionId) { mutableStateOf<Long?>(null) }
    var done by rememberSaveable(sessionId) { mutableStateOf(emptySet<String>()) }
    var tick by rememberSaveable(sessionId) { mutableStateOf(now().epoch()) }
    val started = startEpoch != null
    LaunchedEffect(started) {
        while (started) {
            tick = now().epoch()
            delay(1_000)
        }
    }
    val clock = startEpoch?.let { ChargeClock(epochToLocal(it), epochToLocal(endEpoch ?: it)) }
    val current = epochToLocal(tick)
    val chargeMin = if (clock ==
        null
    ) {
        minutes
    } else {
        (clock.endsAt.epoch() - clock.startedAt.epoch()).toInt() / 60
    }
    val arrival = journey.baseArrival?.plusMinutes(chargeMin.toLong())

    Row(modifier.fillMaxSize().padding(App.space.lg), horizontalArrangement = Gap) {
        Column(
            Modifier.weight(1.3f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(App.space.lg)
        ) {
            Label("여정 (예상)")
            Timeline(journey, started, chargeMin, arrival)
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
            Label(if (started) "충전 중" else "충전 중 활동")
            AppCard(Modifier.fillMaxWidth()) {
                StatusChip(Status.Unknown, "충전 정보 미확인")
                if (clock == null) {
                    Text(
                        "충전이 끝날 때까지 남은 시간",
                        style = App.type.stopTitle,
                        color = App.colors.textPrimary
                    )
                    Stepper(
                        "${minutes}분",
                        { minutes = stepMinutes(minutes, -1) },
                        { minutes = stepMinutes(minutes, 1) },
                        Modifier.fillMaxWidth(),
                        "충전 시간 ${STEP_MIN}분 줄이기",
                        "충전 시간 ${STEP_MIN}분 늘리기"
                    )
                    Text(
                        "차량에서 받지 못해 직접 정해요. 자동으로 확인한 값이 아니에요. " +
                            "${STEP_MIN}분 단위이고, 길게 누르면 계속 바뀌어요.",
                        style = App.type.stopBody,
                        color = App.colors.textSecondary
                    )
                } else {
                    val remaining = clock.remainingSeconds(current)
                    Text("충전 끝까지 남은 시간", style = App.type.stopTitle, color = App.colors.textPrimary)
                    Text(
                        formatRemaining(remaining),
                        style = App.type.driveTitle,
                        color = App.colors.textPrimary
                    )
                    ProgressBar(clock.progress(current))
                    Text(
                        if (clock.finished(current)) {
                            "예상 시간이 지났어요. 충전이 끝났는지 확인해 주세요."
                        } else {
                            "예상 종료 ${clock.endsAt.format(HHMM)} · 직접 정한 시간이라 오차가 있어요."
                        },
                        style = App.type.stopBody,
                        color = App.colors.textSecondary
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            if (clock == null) {
                AppButton(
                    "충전 시작했어요",
                    {
                        val t = now()
                        startEpoch = t.epoch()
                        endEpoch = t.plusMinutes(minutes.toLong()).epoch()
                        tick = t.epoch()
                    },
                    Modifier.fillMaxWidth(),
                    icon = AppIcons.Bolt
                )
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Gap) {
                    AppButton(
                        "${STEP_MIN}분 연장",
                        { endEpoch = clock.extend(STEP_MIN).endsAt.epoch() },
                        Modifier.weight(1f),
                        primary = false
                    )
                    AppButton(
                        "충전 끝났어요",
                        { endEpoch = tick },
                        Modifier.weight(2f),
                        icon = AppIcons.Bolt
                    )
                }
            }
        }
    }
}

@Composable
private fun Label(text: String) =
    Text(text, style = App.type.stopBody, color = App.colors.textSecondary)

@Composable
private fun Timeline(journey: Journey, charging: Boolean, chargeMin: Int, arrival: LocalDateTime?) {
    AppCard(Modifier.fillMaxWidth(), spacing = App.space.lg) {
        Stop(AppIcons.Navigation, false, "현재 위치", journey.stopDetail)
        Stop(
            AppIcons.Bolt,
            true,
            journey.stopName,
            (if (charging) "충전 중" else "충전 ${chargeMin}분")
        )
        Column {
            Stop(
                AppIcons.Place,
                false,
                journey.destinationName,
                arrival?.let { "도착 예상 ${it.format(HHMM)}" } ?: "도착 예상 미확인"
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
                    "희망 도착 ${journey.wishedArrival.format(HHMM)}보다 ${late}분 늦어요",
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
