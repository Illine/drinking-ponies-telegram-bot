package ru.illine.drinking.ponies.mapper

import io.mcarle.konvert.api.Konverter
import ru.illine.drinking.ponies.model.dto.internal.NotificationHistoryDayDto
import ru.illine.drinking.ponies.model.dto.internal.NotificationHistoryDto
import ru.illine.drinking.ponies.model.dto.internal.NotificationHistoryEventDto
import ru.illine.drinking.ponies.model.dto.response.NotificationHistoryDay
import ru.illine.drinking.ponies.model.dto.response.NotificationHistoryEvent
import ru.illine.drinking.ponies.model.dto.response.NotificationHistoryResponse

@Konverter
interface NotificationHistoryResponseMapper {
    fun toResponse(dto: NotificationHistoryDto): NotificationHistoryResponse

    fun toDay(dto: NotificationHistoryDayDto): NotificationHistoryDay

    fun toEvent(dto: NotificationHistoryEventDto): NotificationHistoryEvent

    companion object : NotificationHistoryResponseMapper by Konverter.get<NotificationHistoryResponseMapper>()
}
