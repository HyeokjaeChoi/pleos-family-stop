package ai.familystop

import ai.familystop.core.designsystem.AppTheme
import ai.familystop.feature.activity.ActivityCandidate
import ai.familystop.feature.activity.ActivityKind
import ai.familystop.feature.activity.ChargingActivityScreen
import ai.familystop.feature.fallback.Capability
import ai.familystop.feature.fallback.FallbackCard
import ai.familystop.feature.fallback.FallbackCause
import ai.familystop.feature.metrics.ConsentScreen
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import java.time.LocalDateTime
import org.junit.Rule
import org.junit.Test

/** Night/Day captures of charging activity, fallback and consent (#56) with fixed demo data. */
class ParkedScreensScreenshotTest {
    @get:Rule val rule = createComposeRule()

    private fun shot(
        name: String,
        night: Boolean,
        content: @Composable () -> Unit,
        act: () -> Unit = {}
    ) {
        rule.setContent {
            AppTheme(night = night) {
                Box(Modifier.size(1680.dp, 950.dp).testTag("shot")) { content() }
            }
        }
        act()
        rule.waitForIdle()
        rule.onNodeWithTag("shot").saveShot(name)
    }

    private val activity: @Composable () -> Unit = {
        ChargingActivityScreen(
            parked = true,
            candidates = listOf(
                ActivityCandidate("가상 가족식당 A", ActivityKind.MEAL, 12, 40),
                ActivityCandidate("가상 놀이터 B", ActivityKind.REST, 8, 25)
            ),
            chargeSessionId = "t1",
            autoCharging = false,
            autoChargeEndsAt = null,
            now = LocalDateTime.of(2026, 11, 8, 12, 0)
        )
    }
    private val fallback: @Composable () -> Unit = {
        FallbackCard(FallbackCause.Permission(Capability.LOCATION), 0L, {}, {})
    }
    private val consent: @Composable () -> Unit = {
        ConsentScreen(parked = true, tripId = "t1", consented = false, onResult = {
        }, onWithdraw = {})
    }

    @Test fun activityNight() = shot("parked-activity-night", true, activity)

    @Test fun activityDay() = shot("parked-activity-day", false, activity)

    @Test fun fallbackNight() = shot("parked-fallback-night", true, fallback)

    @Test fun fallbackDay() = shot("parked-fallback-day", false, fallback)

    @Test fun consentNight() = shot("parked-consent-night", true, consent)

    @Test fun consentDay() = shot("parked-consent-day", false, consent)

    private val pickDriver: () -> Unit = { rule.onNodeWithText("저는 운전자 본인이에요").performClick() }

    @Test fun consentDriverNight() = shot("parked-consent-driver-night", true, consent, pickDriver)

    @Test fun consentDriverDay() = shot("parked-consent-driver-day", false, consent, pickDriver)
}
