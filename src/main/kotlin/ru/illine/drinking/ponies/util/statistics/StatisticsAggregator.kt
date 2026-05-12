package ru.illine.drinking.ponies.util.statistics

import ru.illine.drinking.ponies.model.base.StatisticsPeriodType
import ru.illine.drinking.ponies.model.dto.BestDayDto
import ru.illine.drinking.ponies.model.dto.StatisticsPointDto
import ru.illine.drinking.ponies.model.dto.internal.WaterStatisticDto
import java.time.LocalDate
import java.time.ZoneId

object StatisticsAggregator {

    fun sumByLocalDate(events: List<WaterStatisticDto>, zone: ZoneId): Map<LocalDate, Int> =
        events.groupBy { StatisticsPeriodHelper.toLocal(it.eventTime, zone).toLocalDate() }
            .mapValues { entry -> entry.value.sumOf { it.waterAmountMl } }

    fun aggregateByHour(events: List<WaterStatisticDto>, zone: ZoneId): List<StatisticsPointDto> {
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
        daysInPeriod: Int
    ): List<StatisticsPointDto> =
        (0 until daysInPeriod).map { offset ->
            val date = startLocal.plusDays(offset.toLong())
            StatisticsPointDto(label = date.toString(), valueMl = byDate[date] ?: 0)
        }

    fun averageMlPerDay(period: StatisticsPeriodType, points: List<StatisticsPointDto>): Int {
        val total = points.sumOf { it.valueMl }
        return when (period) {
            StatisticsPeriodType.DAY -> total
            StatisticsPeriodType.WEEK,
            StatisticsPeriodType.MONTH -> if (points.isEmpty()) 0 else total / points.size
        }
    }

    fun bestDay(period: StatisticsPeriodType, byDate: Map<LocalDate, Int>): BestDayDto? {
        if (period == StatisticsPeriodType.DAY) return null
        val best = byDate.entries
            .filter { it.value > 0 }
            .sortedBy { it.key }  // chronologically first
            .maxByOrNull { it.value } ?: return null
        return BestDayDto(date = best.key, valueMl = best.value, weekday = best.key.dayOfWeek)
    }

    private const val HOURS_IN_DAY = 24
    private const val HOUR_LABEL_FORMAT = "%02d:00"

}
