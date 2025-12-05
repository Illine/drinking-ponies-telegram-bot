package ru.illine.drinking.ponies.service.impl

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import ru.illine.drinking.ponies.model.dto.NotificationDto
import ru.illine.drinking.ponies.service.NotificationTimeService
import java.time.*
import java.time.zone.ZoneRulesException

@Suppress("IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE")
@Service
class NotificationTimeServiceImpl(private val clock: Clock) : NotificationTimeService {

    private val log = LoggerFactory.getLogger("SERVICE")

    override fun isOutsideQuietTime(dto: NotificationDto): Boolean {
        log.debug("Checking quiet mode for user id: [{}]", dto.id)

        val quietStart = dto.quietModeStart
        val quietEnd = dto.quietModeEnd

        if (quietStart == null || quietEnd == null) {
            log.debug("User has no quiet mode set, allowing notification")
            return true
        } else if (quietStart == quietEnd) {
            log.debug("Quiet mode start equals end ({}). Mode effectively disabled.", quietStart)
            return true
        }

        val userTime = getUserLocalTime(dto)

        return if (quietStart.isBefore(quietEnd)) {
            userTime.isBefore(quietStart) || userTime.isAfter(quietEnd)
        } else {
            userTime.isBefore(quietStart) && userTime.isAfter(quietEnd)
        }
    }

    override fun isNotificationDue(dto: NotificationDto): Boolean {
        val now = LocalDateTime.now(clock)

        log.debug("Checking notification due time for user id: [{}]", dto.id)

        log.debug("Current UTC time: [{}]", now)
        log.debug("Last notification time (UTC): [{}]", dto.timeOfLastNotification)

        val nextNotificationTime = dto.timeOfLastNotification.plusMinutes(dto.delayNotification.minutes)
        log.debug("Next scheduled notification (UTC): [{}]", nextNotificationTime)

        val isDue = nextNotificationTime.isBefore(now)
        log.debug("Notification due: [{}]", isDue)

        return isDue
    }

    private fun getUserLocalTime(dto: NotificationDto): LocalTime {
        val userZoneId = try {
            ZoneId.of(dto.userTimeZone)
        } catch (e: ZoneRulesException) {
            log.error("Invalid timezone for user [${dto.id}]: [${dto.userTimeZone}]. Fallback to UTC.")
            ZoneId.of("UTC")
        }

        val userTime = ZonedDateTime.now(clock).withZoneSameInstant(userZoneId).toLocalTime()
        log.debug("User timezone: [{}], current user time: [{}]", userZoneId, userTime)

        return userTime
    }
}