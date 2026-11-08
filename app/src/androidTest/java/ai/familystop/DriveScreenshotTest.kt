package ai.familystop

import ai.familystop.core.NaviGateway
import ai.familystop.core.StopPlace
import ai.familystop.core.designsystem.AppTheme
import ai.familystop.core.designsystem.Status
import ai.familystop.feature.drive.DriveCandidate
import ai.familystop.feature.drive.DriveScreen
import ai.familystop.feature.drive.MovementSignalSource
import ai.familystop.feature.drive.MovementState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Renders [DriveScreen] states with a fake [NaviGateway] so UndoBar / failure / pending can be
 * captured without Pleos Navi coming to the foreground (#50). PNGs go to the app's private
 * `files/shots/` (`scripts/capture-drive-states.sh` pulls them with `run-as`).
 * The frame is the real app area on the Pleos Connect emulator (1680x950dp, mdpi).
 */
class DriveScreenshotTest {
    @get:Rule val rule = createComposeRule()

    private enum class Navi { Success, Fail, Hang }

    private val cards = listOf(
        DriveCandidate(
            StopPlace("가상 휴게소 C", 37.25, 127.08, null),
            "식사·아이 휴식 모두 한 번에 해결",
            Status.Ok to "필요 조건 충족",
            listOf("+2분" to "도착 지연", "0.3km" to "우회 거리")
        ),
        DriveCandidate(
            StopPlace("가상 가족식당 A", 37.30, 127.05, null),
            "식사 해결 · 아이 휴식 불가",
            Status.Danger to "일부 미충족",
            listOf("+4분" to "도착 지연", "1.2km" to "우회 거리")
        )
    )

    private fun navi(mode: Navi) = object : NaviGateway {
        override fun addWaypoint(place: StopPlace, onResult: (Result<Unit>) -> Unit) {
            when (mode) {
                Navi.Success -> onResult(Result.success(Unit))
                Navi.Fail -> onResult(Result.failure(IllegalStateException("fake")))
                Navi.Hang -> Unit
            }
        }

        override fun removeWaypoint(place: StopPlace, onResult: (Result<Unit>) -> Unit) =
            onResult(Result.success(Unit))
    }

    private val driving = object : MovementSignalSource {
        override fun start(onSignal: (MovementState, String) -> Unit) =
            onSignal(MovementState.DRIVING, "test")

        override fun stop() = Unit
    }

    private fun shot(
        name: String,
        night: Boolean,
        mode: Navi,
        pick: Boolean,
        list: List<DriveCandidate> = cards
    ) {
        rule.setContent {
            AppTheme(night = night) {
                Box(Modifier.size(1680.dp, 950.dp).testTag("shot")) {
                    DriveScreen(list, navi(mode), driving, sourceLabel = "가상 데이터(시연)")
                }
            }
        }
        if (pick) {
            rule.onAllNodesWithText("경유지로 추가")[0].performClick()
        }
        rule.waitForIdle()
        rule.mainClock.advanceTimeBy(1_000) // let the press ripple fade before capturing
        val bmp = rule.onNodeWithTag("shot").captureToImage().asAndroidBitmap()
        val dir = File(InstrumentationRegistry.getInstrumentation().targetContext.filesDir, "shots")
            .apply { mkdirs() }
        File(dir, "$name.png").outputStream().use {
            bmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
        }
        assertTrue(File(dir, "$name.png").length() > 0)
    }

    @Test fun idleNight() = shot("drive-idle-night", true, Navi.Success, false)

    @Test fun idleDay() = shot("drive-idle-day", false, Navi.Success, false)

    @Test fun undoNight() = shot("drive-undo-night", true, Navi.Success, true)

    @Test fun undoDay() = shot("drive-undo-day", false, Navi.Success, true)

    @Test fun failedNight() = shot("drive-failed-night", true, Navi.Fail, true)

    @Test fun pendingNight() = shot("drive-pending-night", true, Navi.Hang, true)

    @Test fun emptyNight() = shot("drive-empty-night", true, Navi.Success, false, emptyList())
}
