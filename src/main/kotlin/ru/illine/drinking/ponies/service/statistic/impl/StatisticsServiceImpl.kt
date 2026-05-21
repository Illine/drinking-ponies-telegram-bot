package ru.illine.drinking.ponies.service.statistic.impl

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import ru.illine.drinking.ponies.dao.access.NotificationAccessService
import ru.illine.drinking.ponies.dao.access.WaterStatisticAccessService
import ru.illine.drinking.ponies.model.base.AnswerNotificationType
import ru.illine.drinking.ponies.model.dto.StatisticsDto
import ru.illine.drinking.ponies.model.dto.internal.NotificationSettingDto
import ru.illine.drinking.ponies.model.dto.internal.WaterStatisticDto
import ru.illine.drinking.ponies.model.dto.message.InsightStatsContext
import ru.illine.drinking.ponies.service.message.MessageProvider
import ru.illine.drinking.ponies.service.statistic.StatisticsService
import ru.illine.drinking.ponies.util.message.MessageSpec
import ru.illine.drinking.ponies.util.statistics.StatisticsAggregator
import ru.illine.drinking.ponies.util.statistics.StatisticsPeriodHelper
import ru.illine.drinking.ponies.util.statistics.toUtcInstant
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

@Service
class StatisticsServiceImpl(
    private val notificationAccessService: NotificationAccessService,
    private val waterStatisticAccessService: WaterStatisticAccessService,
    private val messageProvider: MessageProvider,
    private val clock: Clock,
) : StatisticsService {

    private val logger = LoggerFactory.getLogger("SERVICE")

    // Returns RAW events (unfiltered by YES) for the home widget's per-event diary.
    override fun getToday(telegramUserId: Long): List<WaterStatisticDto> {
        logger.debug("Getting today entries for telegram user [{}]", telegramUserId)

        val ctx = userTimeContext(telegramUserId)
        val (startInclusive, endExclusive) =
            StatisticsPeriodHelper.localDayBoundsToUtc(ctx.today, ctx.today.plusDays(1), ctx.zone)

        return waterStatisticAccessService.findByUserAndEventTimeBetween(telegramUserId, startInclusive, endExclusive)
    }

    override fun getStatistics(telegramUserId: Long, from: LocalDate, to: LocalDate): StatisticsDto {
        logger.debug("Getting [{} - {}] statistics for telegram user [{}]", from, to, telegramUserId)

        require(from <= to) { "Invalid parameter: 'from' must be before or equal to 'to'" }

        val ctx = userTimeContext(telegramUserId)

        require(from <= ctx.today) { "Invalid parameter: 'from' must not be in the future" }

        val days = ChronoUnit.DAYS.between(from, to).toInt() + 1

        // Streak counts back from today up to STREAK_LIMIT_DAYS, so we must fetch a window
        // wide enough to cover both the requested [from..to] range and the streak look-back.
        // Otherwise byDate would be missing days outside [from..to] and the streak would be wrong
        // whenever `to != today` or `from > today - STREAK_LIMIT_DAYS`.
        val streakWindowStart = ctx.today.minusDays(StatisticsAggregator.STREAK_LIMIT_DAYS)
        val fetchStart = minOf(from, streakWindowStart)
        val fetchEndExclusive = maxOf(to, ctx.today).plusDays(1)

        val allEvents = fetchYesEvents(telegramUserId, fetchStart, fetchEndExclusive, ctx.zone)
        val byDate = StatisticsAggregator.sumByLocalDate(allEvents, ctx.zone)

        // Points are built from events in the requested [from..to] window only.
        val rangeEvents = allEvents.filter {
            val date = StatisticsPeriodHelper.toLocal(it.eventTime, ctx.zone).toLocalDate()
            !date.isBefore(from) && !date.isAfter(to)
        }
        val points = if (from == to) {
            StatisticsAggregator.aggregateByHour(rangeEvents, ctx.zone)
        } else {
            StatisticsAggregator.aggregateByDay(byDate, from, days)
        }

        val bestDay = StatisticsAggregator.bestDay(days, byDate.filterKeys { !it.isBefore(from) && !it.isAfter(to) })
        val averageMlPerDay = StatisticsAggregator.averageMlPerDay(days, points)
        val currentStreakDays = StatisticsAggregator.calculateStreak(byDate, ctx.today, ctx.settings.dailyGoalMl)

        val insight = messageProvider.getMessage(
            MessageSpec.InsightStats,
            InsightStatsContext(
                averageMlPerDay,
                bestDay,
                currentStreakDays,
                ctx.settings.dailyGoalMl
            )
        )
        val firstEntryAt = waterStatisticAccessService.findEarliestEventTimeByUser(telegramUserId)?.toUtcInstant()

        return StatisticsDto(
            points = points,
            dailyGoalMl = ctx.settings.dailyGoalMl,
            averageMlPerDay = averageMlPerDay,
            bestDay = bestDay,
            currentStreakDays = currentStreakDays,
            insightText = insight.text,
            firstEntryAt = firstEntryAt
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

    private fun userTimeContext(telegramUserId: Long): UserTimeContext {
        val settings = notificationAccessService.findNotificationSettingByTelegramUserId(telegramUserId)
        val zone = ZoneId.of(settings.telegramUser.userTimeZone)
        val today = LocalDate.now(clock.withZone(zone))
        return UserTimeContext(settings, zone, today)
    }

    private data class UserTimeContext(
        val settings: NotificationSettingDto,
        val zone: ZoneId,
        val today: LocalDate
    )

}
