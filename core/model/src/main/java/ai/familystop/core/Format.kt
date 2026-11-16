package ai.familystop.core

import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

// Default is Korean so call sites not yet migrated to i18n (#93) keep today's output.

/** 앱 전체 시각 표기. 한국어는 "오전/오후 h:mm" 고정(#79), 그 외는 로케일의 짧은 시각 형식. */
fun formatClock(hour24: Int, minute: Int, locale: Locale = Locale.KOREAN): String {
    if (locale.language == "ko") {
        val h12 = if (hour24 % 12 == 0) 12 else hour24 % 12
        return "${if (hour24 < 12) "오전" else "오후"} $h12:${"%02d".format(minute)}"
    }
    return DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale)
        .format(LocalTime.of(hour24, minute))
}

/** 연도 없는 날짜. 한국어 "M월 d일", 그 외 "MMM d". */
fun formatDate(date: LocalDate, locale: Locale = Locale.KOREAN): String {
    val pattern = if (locale.language == "ko") "M월 d일" else "MMM d"
    return DateTimeFormatter.ofPattern(pattern, locale).format(date)
}
