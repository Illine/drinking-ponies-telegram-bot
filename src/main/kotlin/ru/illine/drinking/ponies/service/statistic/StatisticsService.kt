package ru.illine.drinking.ponies.service.statistic

import ru.illine.drinking.ponies.model.dto.StatisticsDto
import ru.illine.drinking.ponies.model.dto.internal.WaterStatisticDto
import java.time.LocalDate

interface StatisticsService {

    fun getToday(externalUserId: Long): List<WaterStatisticDto>

    fun getStatistics(externalUserId: Long, from: LocalDate, to: LocalDate): StatisticsDto

}
