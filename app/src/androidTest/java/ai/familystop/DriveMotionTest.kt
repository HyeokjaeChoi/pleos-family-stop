package ai.familystop

import ai.familystop.core.NaviGateway
import ai.familystop.core.StopPlace
import ai.familystop.core.designsystem.AppTheme
import ai.familystop.core.designsystem.Status
import ai.familystop.feature.drive.DriveCandidate
import ai.familystop.feature.drive.DriveScreen
import ai.familystop.feature.drive.MovementSignalSource
import ai.familystop.feature.drive.MovementState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** 주행 중 모션이 카드 위치를 움직이지 않고(ADR-0003), reduce-motion에서는 즉시 교체되는지 확인한다(#59). */
class DriveMotionTest {
    @get:Rule val rule = createComposeRule()

    private val card = DriveCandidate(
        StopPlace("가상 휴게소 C", 37.25, 127.08, null),
        "요약",
        Status.Ok to "필요 조건 충족"
    )
    private val navi = object : NaviGateway {
        override fun addWaypoint(place: StopPlace, onResult: (Result<Unit>) -> Unit) =
            onResult(Result.success(Unit))

        override fun removeWaypoint(place: StopPlace, onResult: (Result<Unit>) -> Unit) =
            onResult(Result.success(Unit))
    }
    private val driving = object : MovementSignalSource {
        override fun start(onSignal: (MovementState, String) -> Unit) =
            onSignal(MovementState.DRIVING, "test")

        override fun stop() = Unit
    }

    private fun run(reduce: Boolean, settleMs: Long) {
        rule.mainClock.autoAdvance = false
        rule.setContent {
            AppTheme(night = true, reduceMotion = reduce) {
                DriveScreen(listOf(card), navi, driving)
            }
        }
        rule.mainClock.advanceTimeBy(500)
        val before = rule.onNodeWithText("가상 휴게소 C").getUnclippedBoundsInRoot()
        rule.onAllNodesWithText("경유지로 추가")[0].performClick()
        rule.mainClock.advanceTimeBy(settleMs)
        rule.onNodeWithText("초 안에 취소할 수 있어요", substring = true).assertIsDisplayed()
        assertEquals(before, rule.onNodeWithText("가상 휴게소 C").getUnclippedBoundsInRoot())
    }

    /** 시스템 애니메이션 끔(snap 경로): 카드 위치 고정, UndoBar 표시. 선택 상태 갱신이 250ms 틱이라 300ms 후 확인. */
    @Test fun reduceMotionSwapsInstantly() = run(reduce = true, settleMs = 300)

    /** 애니메이션 켬: 입장 200ms 안에 끝나고, 그동안 카드 위치는 그대로다. */
    @Test fun animatedEnterKeepsCardsStill() = run(reduce = false, settleMs = 300)
}
