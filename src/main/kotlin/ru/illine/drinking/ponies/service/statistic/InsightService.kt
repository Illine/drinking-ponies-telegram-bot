package ru.illine.drinking.ponies.service.statistic

import ru.illine.drinking.ponies.model.dto.InsightDto
import java.time.LocalDate
import java.time.ZoneId

interface InsightService {

    fun build(telegramUserId: Long, dailyGoalMl: Int, zone: ZoneId, today: LocalDate): InsightDto

}
