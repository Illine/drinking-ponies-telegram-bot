package ru.illine.drinking.ponies.util

import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

object TimeMessageHelper {

    val DEFAULT_TIME_PATTERN = "HH:mm"
    val DEFAULT_TIME_FORMATTER = DateTimeFormatter.ofPattern(DEFAULT_TIME_PATTERN)

    fun timeToString(dateTime: LocalDateTime): String = timeToString(dateTime.toLocalTime())

    fun timeToString(time: LocalTime): String = time.format(DEFAULT_TIME_FORMATTER)
}