package ru.illine.drinking.ponies.model.dto.internal

import java.time.LocalDate

data class NotificationHistoryDayDto(
    val date: LocalDate,
    val events: List<NotificationHistoryEventDto>,
)
