package ai.familystop.feature.handoff

import ai.familystop.core.StopPlace
import ai.familystop.core.designsystem.App
import ai.familystop.core.designsystem.AppButton
import ai.familystop.core.designsystem.AppCard
import ai.familystop.core.designsystem.AppMotion
import ai.familystop.core.designsystem.Status
import ai.familystop.core.designsystem.StatusChip
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

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
    AppCard(modifier.fillMaxWidth()) {
        // 4상태 전환: 150ms 크로스페이드(DESIGN.md 11절). 같은 상태 안의 값 변화는 재전환하지 않는다.
        AnimatedContent(
            state,
            contentKey = { it::class },
            transitionSpec = {
                fadeIn(AppMotion.spec(reduce, AppMotion.FADE_MS)) togetherWith
                    fadeOut(AppMotion.spec(reduce, AppMotion.FADE_MS))
            },
            label = "handoff"
        ) { state ->
            Column(verticalArrangement = Arrangement.spacedBy(App.space.md)) {
                when (state) {
                    HandoffState.Idle -> Unit
                    is HandoffState.Sending -> {
                        Text("${state.place.name} 전달 중…", style = App.type.stopTitle)
                        Text("응답을 기다리고 있어요.", color = App.colors.textSecondary)
                    }
                    is HandoffState.NeedReplace -> {
                        Text("경유지가 가득 찼어요", style = App.type.stopTitle)
                        Text(
                            "새 경유지: ${state.place.name}. 바꿀 경유지를 골라 주세요.",
                            color = App.colors.textSecondary
                        )
                        state.options.forEach {
                            AppButton("${it.name} 대신", {
                                onReplace(it)
                            }, Modifier.fillMaxWidth(), primary = false)
                        }
                        AppButton("취소", onBack, Modifier.fillMaxWidth(), primary = false)
                    }
                    is HandoffState.Done -> {
                        StatusChip(Status.Ok, "경유지로 추가했어요")
                        Text(
                            "${state.place.name}이(가) 길 안내에 전달됐어요. 도착 여부와는 별개예요.",
                            color = App.colors.textSecondary
                        )
                        if (state.undoFailed) StatusChip(Status.Danger, "되돌리지 못했어요.")
                        if (state.undoable) AppButton("취소", onUndo, Modifier.fillMaxWidth())
                    }
                    is HandoffState.Failed -> {
                        StatusChip(Status.Danger, state.reason)
                        Text("${state.place.name}은 아직 추가되지 않았어요.", color = App.colors.textSecondary)
                        AppButton("다시 시도", onRetry, Modifier.fillMaxWidth())
                        AppButton("후보로 돌아가기", onBack, Modifier.fillMaxWidth(), primary = false)
                    }
                }
            }
        }
    }
}
