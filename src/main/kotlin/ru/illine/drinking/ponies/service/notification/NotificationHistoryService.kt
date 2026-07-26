package ru.illine.drinking.ponies.service.notification

import ru.illine.drinking.ponies.model.base.NotificationHistoryStatus
import ru.illine.drinking.ponies.model.dto.response.NotificationHistoryEvent
import ru.illine.drinking.ponies.model.dto.response.NotificationHistoryResponse
import java.time.LocalDate

interface NotificationHistoryService {
    fun getHistory(
        externalUserId: Long,
        from: LocalDate,
        to: LocalDate,
    ): NotificationHistoryResponse

    fun updateEntry(
        externalUserId: Long,
        entryId: Long,
        status: NotificationHistoryStatus,
        amountMl: Int?,
    ): NotificationHistoryEvent
}
