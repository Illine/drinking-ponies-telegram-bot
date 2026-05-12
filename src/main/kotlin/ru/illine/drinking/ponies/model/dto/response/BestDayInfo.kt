package ru.illine.drinking.ponies.model.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import java.time.DayOfWeek
import java.time.LocalDate

@Schema(description = "Best day of the period by total water consumption")
data class BestDayInfo(
    @Schema(description = "Date in user's timezone", example = "2026-05-06")
    val date: LocalDate,
    @Schema(description = "Total water consumed on this day in milliliters", example = "2400")
    val valueMl: Int,
    @Schema(description = "Day of the week, frontend localizes to the user's locale", example = "WEDNESDAY")
    val weekday: DayOfWeek,
)
