package dev.velocore.console.ui

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

object Format {
    /** 14:12, or 1:02:03 past an hour. */
    fun clock(seconds: Int): String {
        val s = seconds.coerceAtLeast(0)
        val h = s / 3600
        val m = (s % 3600) / 60
        val r = s % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, r) else "%d:%02d".format(m, r)
    }

    /** 1:15 for an hour and a quarter. */
    fun hoursMinutes(minutes: Int): String = "%d:%02d".format(minutes / 60, minutes % 60)

    fun goal(minutes: Int): String {
        val h = minutes / 60
        val m = minutes % 60
        return when {
            h == 0 -> "$m min"
            m == 0 -> "$h h"
            else -> "$h h $m min"
        }
    }

    private val time = DateTimeFormatter.ofPattern("h:mm", Locale.US)
    private val timeAmPm = DateTimeFormatter.ofPattern("h:mm a", Locale.US)
    private val dayName = DateTimeFormatter.ofPattern("EEEE", Locale.US)
    private val longDate = DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.US)

    fun now(epochMs: Long): String = time.format(Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault()))
    fun longDate(date: LocalDate): String = longDate.format(date)

    /** "Yesterday, 6:10 PM", "Today, 7:45 AM" or "Monday, 6:10 PM". */
    fun relative(epochMs: Long, today: LocalDate): String {
        val at = Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault())
        val days = ChronoUnit.DAYS.between(at.toLocalDate(), today)
        val day = when (days) {
            0L -> "Today"
            1L -> "Yesterday"
            in 2L..6L -> dayName.format(at)
            else -> DateTimeFormatter.ofPattern("MMM d", Locale.US).format(at)
        }
        return "$day, ${timeAmPm.format(at)}"
    }

    /** "Wednesday, 7:45 to 8:15 AM". */
    fun span(startMs: Long, endMs: Long): String {
        val zone = ZoneId.systemDefault()
        val a = Instant.ofEpochMilli(startMs).atZone(zone)
        val b = Instant.ofEpochMilli(endMs).atZone(zone)
        val sameHalf = (a.hour < 12) == (b.hour < 12)
        val from = if (sameHalf) time.format(a) else timeAmPm.format(a)
        return "${dayName.format(a)}, $from to ${timeAmPm.format(b)}"
    }

    fun greeting(hour: Int): String = when (hour) {
        in 5..11 -> "Good morning"
        in 12..17 -> "Good afternoon"
        else -> "Good evening"
    }

    fun miles(m: Double): String = "%.1f".format(m)
}
