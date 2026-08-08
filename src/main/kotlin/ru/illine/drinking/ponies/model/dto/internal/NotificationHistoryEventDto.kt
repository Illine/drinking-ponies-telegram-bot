package ru.illine.drinking.ponies.model.dto.internal

import ru.illine.drinking.ponies.model.base.NotificationHistoryStatus
import ru.illine.drinking.ponies.model.base.WaterEntrySourceType
import java.time.Instant

data class NotificationHistoryEventDto(
    val id: Long,
    val eventTime: Instant,
    val status: NotificationHistoryStatus,
    val amountMl: Int,
    val source: WaterEntrySourceType,
    val editable: Boolean,
)
