package ru.illine.drinking.ponies.service.statistic

import ru.illine.drinking.ponies.model.dto.internal.WaterStatisticDto

interface StatisticsService {

    fun getToday(telegramUserId: Long): List<WaterStatisticDto>

}
