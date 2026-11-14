package ai.familystop.core

/** 앱 전체 시각 표기 "오전/오후 h:mm" (#79). 24시간제와 섞어 쓰지 않는다. */
fun formatClock(hour24: Int, minute: Int): String {
    val h12 = if (hour24 % 12 == 0) 12 else hour24 % 12
    return "${if (hour24 < 12) "오전" else "오후"} $h12:${"%02d".format(minute)}"
}
