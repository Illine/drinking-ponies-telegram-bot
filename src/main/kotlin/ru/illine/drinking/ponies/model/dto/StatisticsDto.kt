package ru.illine.drinking.ponies.model.dto

data class StatisticsDto(
    val points: List<StatisticsPointDto>,
    val dailyGoalMl: Int,
    val averageMlPerDay: Int,
    val bestDay: BestDayDto?,
    val currentStreakDays: Int,
    val insightText: String
)
