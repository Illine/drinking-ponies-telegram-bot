package ru.illine.drinking.ponies.dao.access

import ru.illine.drinking.ponies.model.dto.internal.WaterStatisticDto

interface WaterStatisticAccessService {

    fun save(dto: WaterStatisticDto): WaterStatisticDto

}