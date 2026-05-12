package ru.illine.drinking.ponies.service.statistic

import ru.illine.drinking.ponies.model.base.StatisticsPeriodType
import ru.illine.drinking.ponies.model.dto.StatisticsDto
import ru.illine.drinking.ponies.model.dto.internal.WaterStatisticDto

interface StatisticsService {

    fun getToday(telegramUserId: Long): List<WaterStatisticDto>

    fun getStatistics(telegramUserId: Long, period: StatisticsPeriodType): StatisticsDto

}
