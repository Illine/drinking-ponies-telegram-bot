package ru.illine.drinking.ponies.model.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

@Schema(description = "Single day of the notification journal")
data class NotificationHistoryDay(
    @Schema(description = "Calendar date in the user's timezone", example = "2026-07-25")
    val date: LocalDate,
    @Schema(description = "Entries of this day ordered by event time, ascending")
    val events: List<NotificationHistoryEvent>,
)
