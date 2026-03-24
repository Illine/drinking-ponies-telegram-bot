package ru.illine.drinking.ponies.service.notification.impl

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import ru.illine.drinking.ponies.model.dto.internal.NotificationSettingDto
import ru.illine.drinking.ponies.service.notification.NotificationTimeService
import java.time.*
import java.time.zone.ZoneRulesException

@Suppress("IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE")
@Service
class NotificationTimeServiceImpl(private val clock: Clock) : NotificationTimeService {

    private val logger = LoggerFactory.getLogger("SERVICE")

    override fun isOutsideQuietTime(dto: NotificationSettingDto): Boolean {
        logger.debug("Checking quiet mode for user id: [{}]", dto.id)

        val quietStart = dto.quietModeStart
        val quietEnd = dto.quietModeEnd

        if (quietStart == null || quietEnd == null) {
            logger.debug("User has no quiet mode set, allowing notification")
            return true
        } else if (quietStart == quietEnd) {
            logger.debug("Quiet mode start equals end ({}). Mode effectively disabled.", quietStart)
            return true
        }

        val userTime = getUserLocalTime(dto)

        return if (quietStart.isBefore(quietEnd)) {
            userTime.isBefore(quietStart) || userTime.isAfter(quietEnd)
        } else {
            userTime.isBefore(quietStart) && userTime.isAfter(quietEnd)
        }
    }

    override fun isNotificationDue(dto: NotificationSettingDto): Boolean {
        val now = LocalDateTime.now(clock)

        logger.debug("Checking notification due time for user id: [{}]", dto.id)

        logger.debug("Current UTC time: [{}]", now)
        logger.debug("Last notification time (UTC): [{}]", dto.timeOfLastNotification)

        val nextNotificationTime = dto.timeOfLastNotification.plusMinutes(dto.notificationInterval.minutes)
        logger.debug("Next scheduled notification (UTC): [{}]", nextNotificationTime)

        val isDue =  nextNotificationTime <= now
        logger.debug("Notification due: [{}]", isDue)

        return isDue
    }

    private fun getUserLocalTime(dto: NotificationSettingDto): LocalTime {
        val userZoneId = try {
            ZoneId.of(dto.telegramUser.userTimeZone)
        } catch (e: ZoneRulesException) {
            logger.error("Invalid timezone for user [${dto.id}]: [${dto.telegramUser.userTimeZone}], error: ${e.message}. Fallback to UTC.")
            ZoneId.of("UTC")
        }

        val userTime = ZonedDateTime.now(clock).withZoneSameInstant(userZoneId).toLocalTime()
        logger.debug("User timezone: [{}], current user time: [{}]", userZoneId, userTime)

        return userTime
    }
}
