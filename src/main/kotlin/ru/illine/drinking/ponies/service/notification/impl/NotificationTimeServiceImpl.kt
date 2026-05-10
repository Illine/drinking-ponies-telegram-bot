package ru.illine.drinking.ponies.service.notification.impl

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import ru.illine.drinking.ponies.model.dto.internal.NotificationSettingDto
import ru.illine.drinking.ponies.service.notification.NotificationTimeService
import java.time.*

@Suppress("IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE")
@Service
class NotificationTimeServiceImpl(private val clock: Clock) : NotificationTimeService {

    companion object {
        private const val NEXT_DAY_OFFSET = 1L
    }
    
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

    override fun calculateNextNotificationAt(dto: NotificationSettingDto): Instant {
        logger.debug("Calculating next notification time for user id: [{}]", dto.id)

        val rawNext = dto.timeOfLastNotification.plusMinutes(dto.notificationInterval.minutes)
        val rawNextInstant = rawNext.toInstant(ZoneOffset.UTC)
        logger.debug("Raw next notification (UTC): [{}]", rawNextInstant)

        val quietStart = dto.quietModeStart
        val quietEnd = dto.quietModeEnd

        if (quietStart == null || quietEnd == null || quietStart == quietEnd) {
            logger.debug("No quiet mode or start==end, returning raw next: [{}]", rawNextInstant)
            return rawNextInstant
        }

        val userZoneId = resolveZoneId(dto)
        val rawNextUserTime = rawNextInstant.atZone(userZoneId).toLocalTime()
        logger.debug("Raw next in user timezone [{}]: [{}]", userZoneId, rawNextUserTime)

        val isAtOrAfterStart = !rawNextUserTime.isBefore(quietStart)
        val isAtOrBeforeEnd = !rawNextUserTime.isAfter(quietEnd)

        val isInQuietMode = if (quietStart.isBefore(quietEnd)) {
            isAtOrAfterStart && isAtOrBeforeEnd
        } else {
            isAtOrAfterStart || isAtOrBeforeEnd
        }
        logger.debug("Quiet mode [{}-{}], in quiet mode: [{}]", quietStart, quietEnd, isInQuietMode)

        if (!isInQuietMode) {
            return rawNextInstant
        }

        val rawNextUserDateTime = rawNextInstant.atZone(userZoneId).toLocalDateTime()
        var adjustedUserDateTime = rawNextUserDateTime.toLocalDate().atTime(quietEnd)
        if (adjustedUserDateTime.isBefore(rawNextUserDateTime)) {
            adjustedUserDateTime = adjustedUserDateTime.plusDays(NEXT_DAY_OFFSET)
        }

        val adjustedInstant = adjustedUserDateTime.atZone(userZoneId).toInstant()
        logger.debug("Shifted to quiet mode end: [{}]", adjustedInstant)

        return adjustedInstant
    }

    private fun getUserLocalTime(dto: NotificationSettingDto): LocalTime {
        val userZoneId = resolveZoneId(dto)
        val userTime = ZonedDateTime.now(clock).withZoneSameInstant(userZoneId).toLocalTime()
        logger.debug("User timezone: [{}], current user time: [{}]", userZoneId, userTime)

        return userTime
    }
    
    private fun resolveZoneId(dto: NotificationSettingDto): ZoneId {
        return try {
            ZoneId.of(dto.telegramUser.userTimeZone)
        } catch (e: DateTimeException) {
            logger.error(
                "Invalid timezone for user [{}]: [{}], error: {}. Fallback to UTC.",
                dto.id, dto.telegramUser.userTimeZone, e.message
            )
            ZoneId.of("UTC")
        }
    }
}
