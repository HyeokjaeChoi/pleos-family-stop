package ai.familystop.core.designsystem

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertTrue
import org.junit.Test

/** DESIGN.md 2절 대비 기준(글자·아이콘 ≥4.5:1, 버튼↔배경 ≥3:1)을 토큰이 지키는지 확인한다. */
class ContrastTest {
    private fun ratio(a: Color, b: Color): Double {
        val hi = maxOf(a.luminance(), b.luminance()) + 0.05
        val lo = minOf(a.luminance(), b.luminance()) + 0.05
        return (hi / lo).toDouble()
    }

    private fun check(c: AppColors) {
        val faces = listOf(c.bgBase, c.bgSurface, c.bgRaised)
        for (face in faces) {
            assertTrue(ratio(c.textPrimary, face) >= 4.5)
            assertTrue(ratio(c.textSecondary, face) >= 4.5)
        }
        listOf(c.statusOk, c.statusWarn, c.statusDanger, c.statusUnknown).forEach {
            assertTrue("$it", ratio(it, c.bgRaised) >= 4.5)
            assertTrue("$it", ratio(it, c.bgSurface) >= 4.5)
        }
        assertTrue(ratio(c.accentOn, c.accentPrimary) >= 4.5)
        assertTrue(ratio(c.accentPrimary, c.bgSurface) >= 3.0)
    }

    @Test fun night() = check(NightColors)

    @Test fun day() = check(DayColors)

    @Test fun nightIsDarkAndDayIsLight() {
        assertTrue(NightColors.bgBase.luminance() < 0.05f)
        assertTrue(DayColors.bgBase.luminance() > 0.5f)
    }
}
