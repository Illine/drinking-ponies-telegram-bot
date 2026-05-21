package ru.illine.drinking.ponies.model.dto.message

import ru.illine.drinking.ponies.model.dto.BestDayDto

data class InsightStatsContext(
    val avgMlPerDay: Int,
    val bestDay: BestDayDto?,
    val currentStreakDays: Int,
    val dailyGoalMl: Int
) : MessageContext
