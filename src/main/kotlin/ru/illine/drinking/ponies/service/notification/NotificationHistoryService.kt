package ru.illine.drinking.ponies.service.notification

import ru.illine.drinking.ponies.model.base.NotificationHistoryStatus
import ru.illine.drinking.ponies.model.dto.internal.NotificationHistoryDto
import ru.illine.drinking.ponies.model.dto.internal.NotificationHistoryEventDto
import java.time.LocalDate

interface NotificationHistoryService {
    fun getHistory(
        externalUserId: Long,
        from: LocalDate,
        to: LocalDate,
    ): NotificationHistoryDto

    fun updateEntry(
        externalUserId: Long,
        entryId: Long,
        status: NotificationHistoryStatus,
        amountMl: Int?,
    ): NotificationHistoryEventDto
}
