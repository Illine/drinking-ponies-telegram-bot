package ru.illine.drinking.ponies.util

import java.time.Clock
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

object TimeHelper {

    val DEFAULT_TIME_PATTERN = "HH:mm"
    val DEFAULT_TIME_FORMATTER = DateTimeFormatter.ofPattern(DEFAULT_TIME_PATTERN)

    fun timeToString(dateTime: LocalDateTime): String = timeToString(dateTime.toLocalTime())

    fun timeToString(time: LocalTime): String = time.format(DEFAULT_TIME_FORMATTER)

    // The scheduler fires when: timeOfLastNotification + interval <= now
    // To make the next notification fire exactly snoozeMinutes from now:
    //   timeOfLastNotification + interval = now + snoozeMinutes
    //   timeOfLastNotification = now - interval + snoozeMinutes
    fun nextNotificationTimeByNow(clock: Clock, intervalMinutes: Long, snoozeMinutes: Long): LocalDateTime {
        val now = LocalDateTime.now(clock)
        return now.minusMinutes(intervalMinutes).plusMinutes(snoozeMinutes)
    }
}