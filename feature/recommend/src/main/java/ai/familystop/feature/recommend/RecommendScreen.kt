package ai.familystop.feature.recommend

import ai.familystop.core.DataSource
import ai.familystop.core.StopCandidate
import ai.familystop.core.StopNeed
import ai.familystop.core.StopPlace
import ai.familystop.core.StopReason
import ai.familystop.core.designsystem.App
import ai.familystop.core.designsystem.AppButton
import ai.familystop.core.designsystem.AppIcons
import ai.familystop.core.designsystem.CompareRow
import ai.familystop.core.designsystem.R as DsR
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource

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
            RecommendState.Loading ->
                Text(stringResource(R.string.recommend_loading), style = App.type.stopTitle)
            RecommendState.Empty -> Notice(stringResource(R.string.recommend_empty), onRetry)
            is RecommendState.Failed -> Notice(
                stringResource(
                    when (state.kind) {
                        FailKind.TIMEOUT -> R.string.recommend_fail_timeout
                        FailKind.LOAD_ERROR -> R.string.recommend_fail_load
                    }
                ),
                onRetry
            )
            is RecommendState.Success -> CompareList(state.candidates, onPick)
        }
    }
}

@Composable
private fun Notice(message: String, onRetry: () -> Unit) {
    Text(message, style = App.type.stopTitle)
    AppButton(stringResource(R.string.recommend_retry), onRetry)
}

private fun StopCandidate.key() = "${place.name}@${place.lat},${place.lon}"

@Composable
private fun ColumnScope.CompareList(candidates: List<StopCandidate>, onPick: (StopPlace) -> Unit) {
    // Selection is remembered by key so a refreshed list keeps it when the candidate is still there.
    var selectedKey by rememberSaveable { mutableStateOf<String?>(null) }
    val selected = candidates.firstOrNull { it.key() == selectedKey } ?: candidates.first()
    val source = stringResource(
        if (selected.source == DataSource.REAL) {
            R.string.recommend_source_real
        } else {
            R.string.recommend_source_virtual
        }
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            stringResource(R.string.recommend_title),
            style = App.type.stopTitle,
            modifier = Modifier.weight(1f)
        )
        Text(
            stringResource(R.string.recommend_source, source),
            style = App.type.stopBody,
            color = App.colors.textSecondary
        )
    }
    LazyColumn(
        Modifier.weight(1f).selectableGroup(),
        verticalArrangement = Arrangement.spacedBy(App.space.md)
    ) {
        items(candidates, key = { it.key() }) { c ->
            CompareRow(
                name = c.place.name,
                minutes = "+" +
                    pluralStringResource(
                        DsR.plurals.ds_unit_minutes,
                        c.extraMinutes,
                        c.extraMinutes
                    ),
                distance = stringResource(R.string.recommend_distance_km, c.detourKm.toString()),
                status = if (c.missed.isEmpty()) Status.Ok else Status.Danger,
                statusText = stringResource(
                    if (c.missed.isEmpty()) {
                        R.string.recommend_chip_met
                    } else {
                        R.string.recommend_chip_unmet
                    }
                ),
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
            Text(
                stringResource(R.string.recommend_selected, selected.place.name),
                style = App.type.stopTitle
            )
            Text(detail(selected), style = App.type.stopBody, color = App.colors.textSecondary)
        }
        AppButton(
            stringResource(R.string.recommend_add_waypoint),
            { onPick(selected.place) },
            Modifier.weight(1f),
            icon = AppIcons.AddLocationAlt
        )
    }
}

/** 선택한 후보의 충족·미충족 필요를 한 줄로(칩은 요약만 보여 주므로 여기서 구체화). */
@Composable
private fun detail(c: StopCandidate): String = (
    c.met.map { stringResource(R.string.recommend_need_met, it.label()) } +
        c.missed.map { stringResource(R.string.recommend_need_unmet, it.label()) }
    )
    .joinToString(" · ").ifEmpty { stopReasonText(c.reason) }

/** 추천 이유 문장. 화면·주행 카드가 공유한다(음성 안내는 #102에서 같은 값으로 따로 조립). */
@Composable
fun stopReasonText(reason: StopReason): String {
    // joinToString의 transform은 inline이 아니라 label()을 먼저 map으로 푼다.
    val names = reason.needs.map { it.label() }
        .joinToString(stringResource(R.string.recommend_need_separator))
    return stringResource(
        if (reason.coversAll) R.string.recommend_reason_all else R.string.recommend_reason_some,
        names
    )
}

@Composable
private fun StopNeed.label() = stringResource(
    when (this) {
        StopNeed.MEAL -> R.string.recommend_need_meal
        StopNeed.RESTROOM -> R.string.recommend_need_restroom
        StopNeed.KIDS_REST -> R.string.recommend_need_kids_rest
    }
)
