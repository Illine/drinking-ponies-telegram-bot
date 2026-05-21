package ru.illine.drinking.ponies.model.dto

import java.time.Instant

data class StatisticsDto(
    val points: List<StatisticsPointDto>,
    val dailyGoalMl: Int,
    val averageMlPerDay: Int,
    val bestDay: BestDayDto?,
    val currentStreakDays: Int,
    val insightText: String,
    val firstEntryAt: Instant?
)
