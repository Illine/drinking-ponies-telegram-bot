package ru.illine.drinking.ponies.util.statistics

import ru.illine.drinking.ponies.model.dto.BestDayDto
import ru.illine.drinking.ponies.model.dto.StatisticsPointDto
import ru.illine.drinking.ponies.model.dto.internal.WaterStatisticDto
import java.time.LocalDate
import java.time.ZoneId

object StatisticsAggregator {
    private const val HOURS_IN_DAY = 24
    private const val HOUR_LABEL_FORMAT = "%02d:00"

    // 366 (not 365) covers a leap year, so a full year of streak isn't off by one
    const val STREAK_LIMIT_DAYS = 366L

    fun sumByLocalDate(
        events: List<WaterStatisticDto>,
        zone: ZoneId,
    ): Map<LocalDate, Int> =
        events
            .groupBy { StatisticsPeriodHelper.toLocal(it.eventTime, zone).toLocalDate() }
            .mapValues { entry -> entry.value.sumOf { it.waterAmountMl } }

    fun aggregateByHour(
        events: List<WaterStatisticDto>,
        zone: ZoneId,
    ): List<StatisticsPointDto> {
        val byHour = IntArray(HOURS_IN_DAY)
        for (event in events) {
            val localHour = StatisticsPeriodHelper.toLocal(event.eventTime, zone).hour
            byHour[localHour] += event.waterAmountMl
        }
        return (0 until HOURS_IN_DAY).map { hour ->
            StatisticsPointDto(label = HOUR_LABEL_FORMAT.format(hour), valueMl = byHour[hour])
        }
    }

    fun aggregateByDay(
        byDate: Map<LocalDate, Int>,
        startLocal: LocalDate,
        days: Int,
    ): List<StatisticsPointDto> =
        (0 until days).map { offset ->
            val date = startLocal.plusDays(offset.toLong())
            StatisticsPointDto(label = date.toString(), valueMl = byDate[date] ?: 0)
        }

    fun averageMlPerDay(
        days: Int,
        points: List<StatisticsPointDto>,
    ): Int {
        val total = points.sumOf { it.valueMl }
        return total / days
    }

    fun bestDay(
        days: Int,
        byDate: Map<LocalDate, Int>,
    ): BestDayDto? {
        if (days == 1) return null
        val best =
            byDate.entries
                .filter { it.value > 0 }
                .sortedBy { it.key }
                .maxByOrNull { it.value } ?: return null
        return BestDayDto(date = best.key, valueMl = best.value, weekday = best.key.dayOfWeek)
    }

    fun calculateStreak(
        byDate: Map<LocalDate, Int>,
        today: LocalDate,
        dailyGoalMl: Int,
    ): Int {
        // Hanging streak: an unfinished today does NOT break the chain - we start counting from
        // yesterday in that case. The streak only resets to 0 on a real gap (yesterday below goal too).
        var cursor = if ((byDate[today] ?: 0) >= dailyGoalMl) today else today.minusDays(1)
        val earliest = today.minusDays(STREAK_LIMIT_DAYS)
        var streak = 0
        while (!cursor.isBefore(earliest)) {
            if ((byDate[cursor] ?: 0) < dailyGoalMl) break
            streak++
            cursor = cursor.minusDays(1)
        }
        return streak
    }
}
