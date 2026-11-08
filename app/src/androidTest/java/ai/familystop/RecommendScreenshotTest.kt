package ai.familystop

import ai.familystop.core.DataSource
import ai.familystop.core.StopCandidate
import ai.familystop.core.StopNeed
import ai.familystop.core.StopPlace
import ai.familystop.core.designsystem.AppTheme
import ai.familystop.feature.recommend.RecommendScreen
import ai.familystop.feature.recommend.RecommendState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test

/** Captures (PNG → app filesDir; `scripts/capture-drive-states.sh` pulls them into docs/design-qa/shots/drive-states).
 * Captures the parked recommend list (Figma 20:402 / 20:463) with fixed demo candidates. */
class RecommendScreenshotTest {
    @get:Rule val rule = createComposeRule()

    private fun c(name: String, min: Int, km: Double, missed: Set<StopNeed>) = StopCandidate(
        StopPlace(name, 37.3, 127.0),
        met = StopNeed.entries.toSet() - missed,
        missed = missed,
        reason = "식사 · 화장실 · 아이 휴식",
        extraMinutes = min,
        detourKm = km,
        source = DataSource.VIRTUAL
    )

    private fun shot(name: String, night: Boolean) {
        val list = listOf(
            c("가상 휴게소 A", 4, 2.1, emptySet()),
            c("가상 휴게소 B", 7, 3.4, emptySet()),
            c("가상 휴게소 C", 11, 5.8, setOf(StopNeed.KIDS_REST))
        )
        rule.setContent {
            AppTheme(night = night) {
                Box(Modifier.size(1680.dp, 950.dp).testTag("shot")) {
                    RecommendScreen(RecommendState.Success(list), onPick = {}, onRetry = {})
                }
            }
        }
        rule.onNodeWithText("가상 휴게소 B").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("선택: 가상 휴게소 B").assertIsDisplayed()
        rule.onNodeWithText("선택됨").assertIsDisplayed()
        rule.onNodeWithText("경유지로 추가").assertIsDisplayed()
        rule.onNodeWithText("일부 미충족").assertIsDisplayed()
        rule.onNodeWithTag("shot").saveShot(name)
    }

    @Test fun compareNight() = shot("parked-compare-night", true)

    @Test fun compareDay() = shot("parked-compare-day", false)
}
