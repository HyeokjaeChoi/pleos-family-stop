package ai.familystop.core

import java.time.LocalDate
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class FormatTest {
    // JDK 20+ CLDR puts U+202F before AM/PM; Android ICU may too. Compare with a plain space.
    private fun String.plain() = replace(' ', ' ')

    @Test
    fun koreanClockIsUnchanged() {
        assertEquals("오전 12:05", formatClock(0, 5, Locale.KOREAN))
        assertEquals("오전 9:30", formatClock(9, 30, Locale.KOREA))
        assertEquals("오후 12:00", formatClock(12, 0, Locale.KOREAN))
        assertEquals("오후 2:05", formatClock(14, 5))
    }

    @Test
    fun englishClockUsesLocaleFormat() {
        assertEquals("12:05 AM", formatClock(0, 5, Locale.US).plain())
        assertEquals("2:05 PM", formatClock(14, 5, Locale.ENGLISH).plain())
    }

    @Test
    fun koreanDateIsUnchanged() {
        assertEquals("1월 1일", formatDate(LocalDate.of(2026, 1, 1), Locale.KOREAN))
        assertEquals("11월 16일", formatDate(LocalDate.of(2026, 11, 16)))
    }

    @Test
    fun englishDateHasNoYear() {
        assertEquals("Jan 1", formatDate(LocalDate.of(2026, 1, 1), Locale.US))
    }
}
