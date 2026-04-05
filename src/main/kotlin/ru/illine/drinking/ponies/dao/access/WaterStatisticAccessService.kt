package ru.illine.drinking.ponies.dao.access

import ru.illine.drinking.ponies.model.dto.internal.WaterStatisticDto

interface WaterStatisticAccessService {

    fun save(dto: WaterStatisticDto): WaterStatisticDto

    /**
     * Saves water statistic records in batch.
     * Each externalUserId in the collection must be unique - duplicate IDs will result in only the last entry being saved.
     * Records with unknown externalUserId are skipped with a warning.
     */
    fun saveAll(statistics: Collection<WaterStatisticDto>): List<WaterStatisticDto>

}
