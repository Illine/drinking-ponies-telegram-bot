package ru.illine.drinking.ponies.dao.access

import ru.illine.drinking.ponies.model.base.AnswerNotificationType
import ru.illine.drinking.ponies.model.base.WaterEntrySourceType
import ru.illine.drinking.ponies.model.dto.internal.WaterStatisticDto
import java.time.LocalDateTime

interface WaterStatisticAccessService {
    fun findByUserAndEventTimeBetween(
        externalUserId: Long,
        startInclusive: LocalDateTime,
        endExclusive: LocalDateTime,
    ): List<WaterStatisticDto>

    fun findByUserAndTypesAndEventTimeBetween(
        externalUserId: Long,
        sources: Collection<WaterEntrySourceType>,
        eventTypes: Collection<AnswerNotificationType>,
        startInclusive: LocalDateTime,
        endExclusive: LocalDateTime,
    ): List<WaterStatisticDto>

    fun findByIdAndUser(
        id: Long,
        externalUserId: Long,
    ): WaterStatisticDto?

    fun findEarliestEventTimeByUser(externalUserId: Long): LocalDateTime?

    fun save(dto: WaterStatisticDto): WaterStatisticDto

    fun saveAll(statistics: Collection<WaterStatisticDto>): List<WaterStatisticDto>
}
