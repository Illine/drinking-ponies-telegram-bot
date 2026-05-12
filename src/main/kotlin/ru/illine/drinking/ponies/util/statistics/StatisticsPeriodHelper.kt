package ru.illine.drinking.ponies.util.statistics

import ru.illine.drinking.ponies.model.base.StatisticsPeriodType
import java.time.*

object StatisticsPeriodHelper {

    fun periodBounds(period: StatisticsPeriodType, today: LocalDate): Pair<LocalDate, LocalDate> =
        when (period) {
            StatisticsPeriodType.DAY -> today to today.plusDays(1)
            StatisticsPeriodType.WEEK -> {
                val monday = today.with(DayOfWeek.MONDAY)
                monday to monday.plusDays(7)
            }
            StatisticsPeriodType.MONTH -> {
                val first = today.withDayOfMonth(1)
                first to first.plusMonths(1)
            }
        }

    fun daysIn(period: StatisticsPeriodType, startLocal: LocalDate): Int =
        when (period) {
            StatisticsPeriodType.DAY -> 1
            StatisticsPeriodType.WEEK -> 7
            StatisticsPeriodType.MONTH -> startLocal.lengthOfMonth()
        }

    fun toLocal(eventTimeUtc: LocalDateTime, zone: ZoneId): LocalDateTime =
        eventTimeUtc.atZone(ZoneOffset.UTC).withZoneSameInstant(zone).toLocalDateTime()

    fun localDayBoundsToUtc(
        startLocal: LocalDate,
        endLocalExclusive: LocalDate,
        zone: ZoneId
    ): Pair<LocalDateTime, LocalDateTime> {
        val startUtc = startLocal.atStartOfDay(zone).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime()
        val endUtc = endLocalExclusive.atStartOfDay(zone).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime()
        return startUtc to endUtc
    }

}
