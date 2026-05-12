package ru.illine.drinking.ponies.service.statistic.impl

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import ru.illine.drinking.ponies.dao.access.WaterStatisticAccessService
import ru.illine.drinking.ponies.model.base.AnswerNotificationType
import ru.illine.drinking.ponies.model.base.StatisticsPeriodType
import ru.illine.drinking.ponies.model.dto.BestDayDto
import ru.illine.drinking.ponies.model.dto.InsightDto
import ru.illine.drinking.ponies.model.dto.message.InsightStatsContext
import ru.illine.drinking.ponies.service.message.MessageProvider
import ru.illine.drinking.ponies.service.statistic.InsightService
import ru.illine.drinking.ponies.util.message.MessageSpec
import ru.illine.drinking.ponies.util.statistics.StatisticsAggregator
import ru.illine.drinking.ponies.util.statistics.StatisticsPeriodHelper
import java.time.LocalDate
import java.time.ZoneId

@Service
class InsightServiceImpl(
    private val waterStatisticAccessService: WaterStatisticAccessService,
    private val messageProvider: MessageProvider,
) : InsightService {

    private val logger = LoggerFactory.getLogger("SERVICE")

    override fun build(
        telegramUserId: Long,
        period: StatisticsPeriodType,
        dailyGoalMl: Int,
        zone: ZoneId,
        today: LocalDate
    ): InsightDto {
        logger.debug("Building [{}] insight for telegram user [{}]", period, telegramUserId)

        // Single fetch covers both streak (up to STREAK_LIMIT_DAYS) and the insight window (current calendar period).
        val fromLocal = today.minusDays(STREAK_LIMIT_DAYS.toLong())
        val (fromUtc, toUtc) = StatisticsPeriodHelper.localDayBoundsToUtc(fromLocal, today.plusDays(1), zone)
        val byDate = waterStatisticAccessService.findByUserAndEventTimeBetween(telegramUserId, fromUtc, toUtc)
            .filter { it.eventType == AnswerNotificationType.YES }
            .let { events -> StatisticsAggregator.sumByLocalDate(events, zone) }

        val streak = calculateStreak(byDate, today, dailyGoalMl)
        val text = renderInsight(byDate, period, today, dailyGoalMl, streak)
        return InsightDto(currentStreakDays = streak, text = text)
    }

    private fun calculateStreak(
        byDate: Map<LocalDate, Int>,
        today: LocalDate,
        dailyGoalMl: Int
    ): Int {
        var streak = if ((byDate[today] ?: 0) >= dailyGoalMl) 1 else 0
        var cursor = today.minusDays(1)
        val earliest = today.minusDays(STREAK_LIMIT_DAYS.toLong())
        while (!cursor.isBefore(earliest)) {
            if ((byDate[cursor] ?: 0) < dailyGoalMl) break
            streak++
            cursor = cursor.minusDays(1)
        }
        return streak
    }

    private fun renderInsight(
        byDate: Map<LocalDate, Int>,
        period: StatisticsPeriodType,
        today: LocalDate,
        dailyGoalMl: Int,
        streak: Int
    ): String {
        // Calendar bounds match the chart, but only elapsed days (period start through today inclusive) carry data.
        val (periodStart, _) = StatisticsPeriodHelper.periodBounds(period, today)
        val elapsedDays = (today.toEpochDay() - periodStart.toEpochDay() + 1).toInt()
        val elapsedTotals = (0 until elapsedDays).map { offset ->
            val date = periodStart.plusDays(offset.toLong())
            date to (byDate[date] ?: 0)
        }
        val avgMl = elapsedTotals.sumOf { it.second } / elapsedDays
        // On DAY view a "best day" is meaningless - the screen shows only one date.
        val bestDay = if (period == StatisticsPeriodType.DAY) {
            null
        } else {
            elapsedTotals
                .filter { it.second > 0 }
                .sortedBy { it.first }
                .maxByOrNull { it.second }
                ?.let { (date, value) -> BestDayDto(date = date, valueMl = value, weekday = date.dayOfWeek) }
        }

        val context = InsightStatsContext(
            period = period,
            avgMlPerDay = avgMl,
            bestDay = bestDay,
            currentStreakDays = streak,
            dailyGoalMl = dailyGoalMl
        )
        return messageProvider.getMessage(MessageSpec.InsightStats, context).text
    }

    private companion object {
        // 366 (not 365) covers a leap year, so a full year of streak isn't off by one
        const val STREAK_LIMIT_DAYS = 366
    }

}
