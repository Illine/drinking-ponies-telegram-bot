package ru.illine.drinking.ponies.builder

import ru.illine.drinking.ponies.model.dto.BestDayDto
import ru.illine.drinking.ponies.model.dto.StatisticsDto
import ru.illine.drinking.ponies.model.dto.StatisticsPointDto
import ru.illine.drinking.ponies.model.dto.response.BestDayInfo
import ru.illine.drinking.ponies.model.dto.response.InsightInfo
import ru.illine.drinking.ponies.model.dto.response.StatisticsPoint
import ru.illine.drinking.ponies.model.dto.response.StatisticsResponse

object StatisticsBuilder {

    fun toResponse(dto: StatisticsDto): StatisticsResponse =
        StatisticsResponse(
            points = dto.points.map(::toPoint),
            dailyGoalMl = dto.dailyGoalMl,
            averageMlPerDay = dto.averageMlPerDay,
            bestDay = dto.bestDay?.let(::toBestDay),
            currentStreakDays = dto.currentStreakDays,
            insight = InsightInfo(text = dto.insightText),
        )

    private fun toPoint(point: StatisticsPointDto): StatisticsPoint =
        StatisticsPoint(label = point.label, valueMl = point.valueMl)

    private fun toBestDay(bestDay: BestDayDto): BestDayInfo =
        BestDayInfo(date = bestDay.date, valueMl = bestDay.valueMl, weekday = bestDay.weekday)

}
