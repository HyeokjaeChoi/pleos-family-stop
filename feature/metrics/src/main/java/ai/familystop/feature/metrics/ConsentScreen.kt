package ai.familystop.feature.metrics

import ai.familystop.core.designsystem.App
import ai.familystop.core.designsystem.AppButton
import ai.familystop.core.designsystem.AppToggleChip
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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

/**
 * 측정 동의·철회 화면(공개 composable 1개). 정차 중에만 보인다(주행·미확인은 안내만, ADR-0004).
 * 동의하지 않아도 앱 기능은 그대로 쓴다.
 *
 * 운전자 확인은 화면의 자기 선언이다. SDK에 운전자 식별값이 없어 동승자 탭을 기술적으로
 * 막을 수 없다(한계). 선언 상태는 [tripId]에 묶여 새 여행에서 다시 묻는다.
 *
 * @param parked 정차 확인됨. 미확인이면 false.
 * @param tripId 이번 여행 표식([MetricsCollector.startTrip]).
 * @param consented 이미 동의했으면 true → 철회 화면을 보인다.
 * @param onResult 동의 true / 거절 false. true는 운전자 선언 뒤에만 호출되므로
 *   호출 측은 `grantConsent(id, isDriver = true)`로 연결한다.
 * @param onWithdraw 철회. 호출 측은 [MetricsCollector.withdraw]로 연결한다.
 */
@Composable
fun ConsentScreen(
    parked: Boolean,
    tripId: String,
    consented: Boolean,
    onResult: (consent: Boolean) -> Unit,
    onWithdraw: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isDriver by rememberSaveable(tripId) { mutableStateOf(false) }
    if (!parked) {
        Text("정차 후에 확인할 수 있어요.", modifier.fillMaxSize().padding(App.space.lg))
        return
    }
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(App.space.lg),
        verticalArrangement = Arrangement.spacedBy(App.space.lg)
    ) {
        if (consented) {
            Text("측정 동의를 철회하면 이번 여행 기록이 모두 지워져요.")
            AppButton("동의 철회하고 기록 삭제", onWithdraw, Modifier.fillMaxWidth(), primary = false)
            return@Column
        }
        Text("이번 여행의 정차 도움 정도를 기록해도 될까요?")
        Text(
            "계획 시간·선택 이유·남은 필요를 이 기기에만 저장해요. " +
                "이름·계정과 연결하지 않고, 철회하면 이번 여행 기록이 지워져요. " +
                "동의하지 않아도 모든 기능을 쓸 수 있어요."
        )
        AppToggleChip(isDriver, "저는 운전자 본인이에요") { isDriver = !isDriver }
        AppButton("동의하고 기록", { onResult(true) }, Modifier.fillMaxWidth(), enabled = isDriver)
        AppButton("기록하지 않을게요", { onResult(false) }, Modifier.fillMaxWidth(), primary = false)
    }
}
