package ru.illine.drinking.ponies.service.statistic.impl

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import ru.illine.drinking.ponies.dao.access.NotificationAccessService
import ru.illine.drinking.ponies.dao.access.WaterStatisticAccessService
import ru.illine.drinking.ponies.model.base.AnswerNotificationType
import ru.illine.drinking.ponies.model.base.StatisticsPeriodType
import ru.illine.drinking.ponies.model.dto.StatisticsDto
import ru.illine.drinking.ponies.model.dto.internal.WaterStatisticDto
import ru.illine.drinking.ponies.service.statistic.InsightService
import ru.illine.drinking.ponies.service.statistic.StatisticsService
import ru.illine.drinking.ponies.util.statistics.StatisticsAggregator
import ru.illine.drinking.ponies.util.statistics.StatisticsPeriodHelper
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId

@Service
class StatisticsServiceImpl(
    private val notificationAccessService: NotificationAccessService,
    private val waterStatisticAccessService: WaterStatisticAccessService,
    private val insightService: InsightService,
    private val clock: Clock,
) : StatisticsService {

    private val logger = LoggerFactory.getLogger("SERVICE")

    // Returns RAW events (not filtered by YES) — used by the home widget's per-event diary.
    // For aggregated metrics see getStatistics, which filters by YES.
    override fun getToday(telegramUserId: Long): List<WaterStatisticDto> {
        logger.debug("Getting today entries for telegram user [{}]", telegramUserId)

        val settings = notificationAccessService.findNotificationSettingByTelegramUserId(telegramUserId)
        val zone = ZoneId.of(settings.telegramUser.userTimeZone)
        val today = LocalDate.now(clock.withZone(zone))
        val (startInclusive, endExclusive) = StatisticsPeriodHelper.localDayBoundsToUtc(today, today.plusDays(1), zone)

        return waterStatisticAccessService.findByUserAndEventTimeBetween(telegramUserId, startInclusive, endExclusive)
    }

    override fun getStatistics(telegramUserId: Long, period: StatisticsPeriodType): StatisticsDto {
        logger.debug("Getting [{}] statistics for telegram user [{}]", period, telegramUserId)

        val settings = notificationAccessService.findNotificationSettingByTelegramUserId(telegramUserId)
        val zone = ZoneId.of(settings.telegramUser.userTimeZone)
        val today = LocalDate.now(clock.withZone(zone))

        val (startLocal, endLocalExclusive) = StatisticsPeriodHelper.periodBounds(period, today)
        val events = fetchYesEvents(telegramUserId, startLocal, endLocalExclusive, zone)
        val byDate = StatisticsAggregator.sumByLocalDate(events, zone)

        val points = when (period) {
            StatisticsPeriodType.DAY -> StatisticsAggregator.aggregateByHour(events, zone)
            StatisticsPeriodType.WEEK,
            StatisticsPeriodType.MONTH -> StatisticsAggregator.aggregateByDay(
                byDate, startLocal, StatisticsPeriodHelper.daysIn(period, startLocal)
            )
        }

        val insight = insightService.build(telegramUserId, period, settings.dailyGoalMl, zone, today)

        return StatisticsDto(
            period = period,
            points = points,
            dailyGoalMl = settings.dailyGoalMl,
            averageMlPerDay = StatisticsAggregator.averageMlPerDay(period, points),
            bestDay = StatisticsAggregator.bestDay(period, byDate),
            currentStreakDays = insight.currentStreakDays,
            goalProgress = StatisticsAggregator.goalProgress(period, points, settings.dailyGoalMl, startLocal, today),
            insightText = insight.text,
        )
    }

    private fun fetchYesEvents(
        telegramUserId: Long,
        startLocal: LocalDate,
        endLocalExclusive: LocalDate,
        zone: ZoneId
    ): List<WaterStatisticDto> {
        val (startUtc, endUtc) = StatisticsPeriodHelper.localDayBoundsToUtc(startLocal, endLocalExclusive, zone)
        return waterStatisticAccessService.findByUserAndEventTimeBetween(telegramUserId, startUtc, endUtc)
            .filter { it.eventType == AnswerNotificationType.YES }
    }

}
