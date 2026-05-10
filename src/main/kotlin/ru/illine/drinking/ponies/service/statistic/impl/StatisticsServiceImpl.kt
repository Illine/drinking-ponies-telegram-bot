package ru.illine.drinking.ponies.service.statistic.impl

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import ru.illine.drinking.ponies.dao.access.NotificationAccessService
import ru.illine.drinking.ponies.dao.access.WaterStatisticAccessService
import ru.illine.drinking.ponies.model.dto.internal.WaterStatisticDto
import ru.illine.drinking.ponies.service.statistic.StatisticsService
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

@Service
class StatisticsServiceImpl(
    private val notificationAccessService: NotificationAccessService,
    private val waterStatisticAccessService: WaterStatisticAccessService,
    private val clock: Clock,
) : StatisticsService {

    private val logger = LoggerFactory.getLogger("SERVICE")

    override fun getToday(telegramUserId: Long): List<WaterStatisticDto> {
        logger.debug("Getting today entries for telegram user [$telegramUserId]")

        val settings = notificationAccessService.findNotificationSettingByTelegramUserId(telegramUserId)
        val zone = ZoneId.of(settings.telegramUser.userTimeZone)

        // event_time is stored as UTC LocalDateTime, so day boundaries computed in user TZ
        // must be converted to UTC LocalDateTime for the query.
        val today = LocalDate.now(clock.withZone(zone))
        val startInclusive = today.atStartOfDay(zone).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime()
        val endExclusive = today.plusDays(1).atStartOfDay(zone).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime()

        return waterStatisticAccessService.findByUserAndEventTimeBetween(
            telegramUserId, startInclusive, endExclusive
        )
    }
}
