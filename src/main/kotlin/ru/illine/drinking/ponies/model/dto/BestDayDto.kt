package ru.illine.drinking.ponies.model.dto

import java.time.DayOfWeek
import java.time.LocalDate

data class BestDayDto(
    val date: LocalDate,
    val valueMl: Int,
    val weekday: DayOfWeek
)
