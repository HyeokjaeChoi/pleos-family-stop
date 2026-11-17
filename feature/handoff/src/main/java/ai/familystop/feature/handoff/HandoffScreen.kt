package ai.familystop.feature.handoff

import ai.familystop.core.StopPlace
import ai.familystop.core.designsystem.App
import ai.familystop.core.designsystem.AppButton
import ai.familystop.core.designsystem.AppCard
import ai.familystop.core.designsystem.AppMotion
import ai.familystop.core.designsystem.Status
import ai.familystop.core.designsystem.StatusChip
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics

/**
 * 단일 공개 화면. 주행 중에도 쓰므로 스크롤·펼치기 없이 큰 버튼만 둔다(ADR-0003).
 * 경유지 선택은 최대 2개 버튼 + 취소. [HandoffState.Idle]이면 아무것도 그리지 않는다.
 */
@Composable
fun HandoffScreen(
    state: HandoffState,
    onReplace: (StopPlace) -> Unit,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    onUndo: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (state is HandoffState.Idle) return
    val reduce = AppMotion.reduce
    AppCard(modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite }) {
        // 4상태 전환: 150ms 크로스페이드(DESIGN.md 11절). 같은 상태 안의 값 변화는 재전환하지 않는다.
        AnimatedContent(
            state,
            contentKey = { it::class },
            transitionSpec = {
                (
                    fadeIn(AppMotion.spec(reduce, AppMotion.FADE_MS)) togetherWith
                        fadeOut(AppMotion.spec(reduce, AppMotion.FADE_MS))
                    ).using(
                    SizeTransform(clip = false) { _, _ ->
                        AppMotion.spec(reduce, AppMotion.FADE_MS)
                    }
                )
            },
            label = "handoff"
        ) { state ->
            Column(verticalArrangement = Arrangement.spacedBy(App.space.md)) {
                when (state) {
                    HandoffState.Idle -> Unit
                    is HandoffState.Sending -> {
                        Text(
                            stringResource(R.string.handoff_sending, state.place.name),
                            style = App.type.stopTitle
                        )
                        Text(
                            stringResource(R.string.handoff_sending_wait),
                            color = App.colors.textSecondary
                        )
                    }
                    is HandoffState.NeedReplace -> {
                        Text(
                            stringResource(R.string.handoff_full_title),
                            style = App.type.stopTitle
                        )
                        Text(
                            stringResource(R.string.handoff_full_body, state.place.name),
                            color = App.colors.textSecondary
                        )
                        state.options.forEach {
                            AppButton(stringResource(R.string.handoff_replace_option, it.name), {
                                onReplace(it)
                            }, Modifier.fillMaxWidth(), primary = false)
                        }
                        AppButton(
                            stringResource(R.string.handoff_cancel),
                            onBack,
                            Modifier.fillMaxWidth(),
                            primary = false
                        )
                    }
                    is HandoffState.Done -> {
                        StatusChip(Status.Ok, stringResource(R.string.handoff_done_chip))
                        Text(
                            stringResource(R.string.handoff_done_body, state.place.name),
                            color = App.colors.textSecondary
                        )
                        if (state.undoFailed) {
                            StatusChip(
                                Status.Danger,
                                stringResource(R.string.handoff_undo_failed)
                            )
                        }
                        if (state.undoable) {
                            AppButton(
                                stringResource(R.string.handoff_cancel),
                                onUndo,
                                Modifier.fillMaxWidth()
                            )
                        }
                    }
                    is HandoffState.Failed -> {
                        StatusChip(Status.Danger, stringResource(state.reason.res()))
                        Text(
                            stringResource(R.string.handoff_failed_body, state.place.name),
                            color = App.colors.textSecondary
                        )
                        AppButton(
                            stringResource(R.string.handoff_retry),
                            onRetry,
                            Modifier.fillMaxWidth()
                        )
                        AppButton(
                            stringResource(R.string.handoff_back_to_candidates),
                            onBack,
                            Modifier.fillMaxWidth(),
                            primary = false
                        )
                    }
                }
            }
        }
    }
}

private fun HandoffFail.res() = when (this) {
    HandoffFail.SendFailed -> R.string.handoff_fail_send
    HandoffFail.ReplaceFailed -> R.string.handoff_fail_replace
    HandoffFail.StaleRemoved -> R.string.handoff_fail_stale
}
