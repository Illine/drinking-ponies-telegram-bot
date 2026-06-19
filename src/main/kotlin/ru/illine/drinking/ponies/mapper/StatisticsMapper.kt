package ru.illine.drinking.ponies.mapper

import io.mcarle.konvert.api.Konvert
import io.mcarle.konvert.api.Konverter
import io.mcarle.konvert.api.Mapping
import ru.illine.drinking.ponies.model.dto.BestDayDto
import ru.illine.drinking.ponies.model.dto.StatisticsDto
import ru.illine.drinking.ponies.model.dto.StatisticsPointDto
import ru.illine.drinking.ponies.model.dto.response.BestDayInfo
import ru.illine.drinking.ponies.model.dto.response.StatisticsPoint
import ru.illine.drinking.ponies.model.dto.response.StatisticsResponse

@Konverter
interface StatisticsMapper {
    @Konvert(
        mappings = [
            Mapping(
                target = "insight",
                expression = "ru.illine.drinking.ponies.model.dto.response.InsightInfo(it.insightText)",
            ),
        ]
    )
    fun toResponse(dto: StatisticsDto): StatisticsResponse

    fun toPoint(dto: StatisticsPointDto): StatisticsPoint

    fun toBestDay(dto: BestDayDto): BestDayInfo

    companion object : StatisticsMapper by Konverter.get<StatisticsMapper>()
}
