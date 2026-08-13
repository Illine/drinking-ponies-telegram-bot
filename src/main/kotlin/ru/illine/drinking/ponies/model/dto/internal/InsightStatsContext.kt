package ru.illine.drinking.ponies.model.dto.internal

data class InsightStatsContext(
    val avgMlPerDay: Int,
    val bestDay: BestDayDto?,
    val currentStreakDays: Int,
    val dailyGoalMl: Int,
) : MessageContext
