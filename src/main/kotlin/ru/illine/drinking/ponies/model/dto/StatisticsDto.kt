package ru.illine.drinking.ponies.model.dto

import ru.illine.drinking.ponies.model.base.StatisticsPeriodType

data class StatisticsDto(
    val period: StatisticsPeriodType,
    val points: List<StatisticsPointDto>,
    val dailyGoalMl: Int,
    val averageMlPerDay: Int,
    val bestDay: BestDayDto?,
    val currentStreakDays: Int,
    val goalProgress: Double?,
    val insightText: String
)
