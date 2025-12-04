package ru.illine.drinking.ponies.scheduler

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import ru.illine.drinking.ponies.dao.access.NotificationAccessService
import ru.illine.drinking.ponies.model.dto.NotificationDto
import ru.illine.drinking.ponies.service.NotificationService
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

@Component
class NotificationScheduler(
    private val notificationAccessService: NotificationAccessService,
    private val notificationService: NotificationService
) {

    private val MAX_REMINDED_NOTIFICATION_ATTEMPTS = 3

    private val log = LoggerFactory.getLogger("SCHEDULER")

    @Scheduled(cron = "\${telegram-bot.schedule.notification.cron}")
    fun sendDrinkingReminders() {
        log.info("Starting drinking notification scheduler")

        val now = LocalDateTime.now()
        val (exhaustedNotifications, activeNotifications) = notificationAccessService.findAll()
            .filter(::isOutsideQuietTime)
            .filter { isNotificationDue(it, now) }
            .partition { it.notificationAttempts == MAX_REMINDED_NOTIFICATION_ATTEMPTS }

        cancelAll(exhaustedNotifications)
        notifyAll(activeNotifications)

        log.info("Drinking notification scheduler finished")
    }

    /**
     * Checks if the user can receive notifications right now (not in Quiet Mode).
     */
    private fun isOutsideQuietTime(dto: NotificationDto): Boolean {
        log.debug("Checking quiet mode for user id: [{}]", dto.id)

        val quietStart = dto.quietModeStart
        val quietEnd = dto.quietModeEnd

        if (quietStart == null || quietEnd == null) {
            log.debug("User has no quiet mode set, allowing notification")
            return true
        }

        val userZoneId = ZoneId.of(dto.userTimeZone)
        val userDateTime = ZonedDateTime.now(userZoneId)
        val userTime = userDateTime.toLocalTime()

        log.debug("User timezone: [{}], current user time: [{}]", userZoneId, userTime)

        val late = userTime.isBefore(quietStart)
        val early = userTime.isAfter(quietEnd)

        log.debug("Time is before quiet start [{}]: [{}]", quietStart, late)
        log.debug("Time is after quiet end [{}]: [{}]", quietEnd, early)

        return late && early
    }

    private fun isNotificationDue(dto: NotificationDto, comparisonTime: LocalDateTime): Boolean {
        log.debug("Checking notification due time for user id: [{}]", dto.id)

        log.debug("Current UTC time: [{}]", comparisonTime)
        log.debug("Last notification time (UTC): [{}]", dto.timeOfLastNotification)

        val nextNotificationTime = dto.timeOfLastNotification.plusMinutes(dto.delayNotification.minutes)
        log.debug("Next scheduled notification (UTC): [{}]", nextNotificationTime)

        val isDue = nextNotificationTime.isBefore(comparisonTime)
        log.debug("Notification due: [{}]", isDue)

        return isDue
    }

    private fun notifyAll(notifications: List<NotificationDto>) {
        if (notifications.isEmpty()) {
            return
        }

        log.info("Sending notifications to [{}] users", notifications.size)
        notificationService.sendNotifications(notifications)
    }

    private fun cancelAll(notifications: List<NotificationDto>) {
        if (notifications.isEmpty()) {
            return
        }

        log.info("Suspending notifications for [{}] users (max attempts reached)", notifications.size)
        notificationService.suspendNotifications(notifications)
    }
}