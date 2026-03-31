package com.example.fbuddy.utils

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

object DateUtils {

    private val zone: ZoneId get() = ZoneId.systemDefault()

    /** Midnight (00:00:00) of the day containing [millis]. */
    fun startOfDayMillis(millis: Long): Long {
        return Instant.ofEpochMilli(millis)
            .atZone(zone)
            .toLocalDate()
            .atStartOfDay(zone)
            .toInstant()
            .toEpochMilli()
    }

    /** Midnight of [daysAgo] days before the day containing [now]. */
    fun startOfDaysAgoMillis(daysAgo: Long, now: Long = System.currentTimeMillis()): Long {
        return Instant.ofEpochMilli(now)
            .atZone(zone)
            .toLocalDate()
            .minusDays(daysAgo)
            .atStartOfDay(zone)
            .toInstant()
            .toEpochMilli()
    }

    /** Midnight of the 1st day of the month containing [millis]. */
    fun startOfMonthMillis(millis: Long = System.currentTimeMillis()): Long {
        return Instant.ofEpochMilli(millis)
            .atZone(zone)
            .toLocalDate()
            .withDayOfMonth(1)
            .atStartOfDay(zone)
            .toInstant()
            .toEpochMilli()
    }

    /** Midnight of the 1st day of [monthsAgo] months before the month containing [now]. */
    fun startOfMonthsAgoMillis(monthsAgo: Long, now: Long = System.currentTimeMillis()): Long {
        return Instant.ofEpochMilli(now)
            .atZone(zone)
            .toLocalDate()
            .withDayOfMonth(1)
            .minusMonths(monthsAgo)
            .atStartOfDay(zone)
            .toInstant()
            .toEpochMilli()
    }

    /** Human-readable date label, e.g. "Today", "Yesterday", "26 Feb". */
    fun friendlyDateLabel(millis: Long): String {
        val today     = LocalDate.now(zone)
        val yesterday = today.minusDays(1)
        val date      = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()
        return when (date) {
            today     -> "Today"
            yesterday -> "Yesterday"
            else      -> "${date.dayOfMonth} ${date.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }}"
        }
    }

    /** Short time label, e.g. "3:45 PM". */
    fun friendlyTimeLabel(millis: Long): String {
        val time = Instant.ofEpochMilli(millis).atZone(zone).toLocalTime()
        val hour   = time.hour
        val minute = time.minute
        val amPm   = if (hour < 12) "AM" else "PM"
        val hour12 = when {
            hour == 0  -> 12
            hour > 12  -> hour - 12
            else       -> hour
        }
        return "%d:%02d %s".format(hour12, minute, amPm)
    }
}

