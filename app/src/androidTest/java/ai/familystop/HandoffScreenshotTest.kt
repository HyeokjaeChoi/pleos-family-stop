package ai.familystop

import ai.familystop.core.NaviGateway
import ai.familystop.core.StopPlace
import ai.familystop.core.designsystem.AppTheme
import ai.familystop.feature.handoff.HandoffController
import ai.familystop.feature.handoff.HandoffScreen
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Renders [HandoffScreen] (Sending / Done / Failed / NeedReplace) driven by a real
 * [HandoffController] over a fake [NaviGateway], so no Pleos Navi foreground switch (#56).
 * PNGs → app `files/shots/`; `scripts/capture-drive-states.sh docs/design-qa/shots/handoff` pulls them.
 */
class HandoffScreenshotTest {
    @get:Rule val rule = createComposeRule()

    private enum class Navi { Success, Fail, Hang }

    private val a = StopPlace("가상 휴게소 A", 37.25, 127.08, null)
    private val b = StopPlace("가상 휴게소 B", 37.30, 127.05, null)
    private val c = StopPlace("가상 휴게소 C", 37.35, 127.02, null)

    private fun shot(
        name: String,
        night: Boolean,
        mode: Navi,
        picks: List<StopPlace>,
        expect: String
    ) {
        var current = Navi.Success
        val navi = object : NaviGateway {
            override fun addWaypoint(place: StopPlace, onResult: (Result<Unit>) -> Unit) =
                when (current) {
                    Navi.Success -> onResult(Result.success(Unit))
                    Navi.Fail -> onResult(Result.failure(IllegalStateException("fake")))
                    Navi.Hang -> Unit
                }

            override fun removeWaypoint(place: StopPlace, onResult: (Result<Unit>) -> Unit) =
                onResult(Result.success(Unit))
        }
        val scope = CoroutineScope(Dispatchers.Main)
        val ctl = HandoffController(scope, navi)
        rule.setContent {
            AppTheme(night = night) {
                Box(Modifier.size(1680.dp, 950.dp).testTag("shot")) {
                    HandoffScreen(
                        ctl.state.collectAsState().value,
                        ctl::replace,
                        ctl::retry,
                        ctl::backToCompare,
                        ctl::undo
                    )
                }
            }
        }
        rule.runOnUiThread {
            picks.dropLast(1).forEach(ctl::pick) // fill held waypoints with success
            current = mode
            ctl.pick(picks.last())
        }
        rule.waitForIdle()
        rule.onNodeWithText(expect, substring = true).assertIsDisplayed()
        val bmp = rule.onNodeWithTag("shot").captureToImage().asAndroidBitmap()
        val dir = File(InstrumentationRegistry.getInstrumentation().targetContext.filesDir, "shots")
            .apply { mkdirs() }
        File(dir, "$name.png").outputStream().use {
            bmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
        }
        assertTrue(File(dir, "$name.png").length() > 0)
        scope.cancel()
    }

    @Test fun sendingNight() = shot("handoff-sending-night", true, Navi.Hang, listOf(a), "전달 중")

    @Test fun sendingDay() = shot("handoff-sending-day", false, Navi.Hang, listOf(a), "전달 중")

    @Test fun doneNight() = shot("handoff-done-night", true, Navi.Success, listOf(a), "경유지로 추가했어요")

    @Test fun doneDay() = shot("handoff-done-day", false, Navi.Success, listOf(a), "경유지로 추가했어요")

    @Test fun failedNight() = shot("handoff-failed-night", true, Navi.Fail, listOf(a), "전달하지 못했어요")

    @Test fun failedDay() = shot("handoff-failed-day", false, Navi.Fail, listOf(a), "전달하지 못했어요")

    @Test fun replaceNight() =
        shot("handoff-replace-night", true, Navi.Success, listOf(a, b, c), "경유지가 가득 찼어요")

    @Test fun replaceDay() =
        shot("handoff-replace-day", false, Navi.Success, listOf(a, b, c), "경유지가 가득 찼어요")
}
