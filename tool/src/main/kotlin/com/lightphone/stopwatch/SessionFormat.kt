package com.lightphone.stopwatch

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** e.g. "Mon, Jul 8" — the year appears only when it isn't the current one. */
fun formatSessionDate(wallMs: Long): String {
    val cal = java.util.Calendar.getInstance()
    val nowYear = cal.get(java.util.Calendar.YEAR)
    cal.timeInMillis = wallMs
    val pattern = if (cal.get(java.util.Calendar.YEAR) == nowYear) "EEE, MMM d" else "EEE, MMM d yyyy"
    return SimpleDateFormat(pattern, Locale.getDefault()).format(Date(wallMs))
}

/** e.g. "3:47 PM" */
fun formatSessionTime(wallMs: Long): String {
    val fmt = SimpleDateFormat("h:mm a", Locale.getDefault())
    return fmt.format(Date(wallMs))
}

fun lapCountLabel(count: Int): String =
    if (count == 1) "1 lap" else "$count laps"

/**
 * Full row format, zero-padded — 00:01.66 → "00:01.66", 01:15.16 → "01:15.16",
 * 1:02:15.16 → "01:02:15.16" (feedback 2026-10-12: laps read like the timer).
 */
internal fun lapTime(ms: Long): String {
    val clamped = if (ms < 0) 0L else ms
    val h = clamped / 3_600_000
    val m = (clamped % 3_600_000) / 60_000
    val s = (clamped % 60_000) / 1_000
    val c = (clamped % 1_000) / 10
    return when {
        h > 0 -> "%02d:%02d:%02d.%02d".format(h, m, s, c)
        else -> "%02d:%02d.%02d".format(m, s, c)
    }
}
