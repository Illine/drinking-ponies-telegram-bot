package ru.illine.drinking.ponies.dao.access

import ru.illine.drinking.ponies.model.dto.internal.WaterStatisticDto
import java.time.LocalDateTime

interface WaterStatisticAccessService {
    
    fun findByUserAndEventTimeBetween(
        telegramUserId: Long,
        startInclusive: LocalDateTime,
        endExclusive: LocalDateTime
    ): List<WaterStatisticDto>

    fun save(dto: WaterStatisticDto): WaterStatisticDto

    fun saveAll(statistics: Collection<WaterStatisticDto>): List<WaterStatisticDto>

}
