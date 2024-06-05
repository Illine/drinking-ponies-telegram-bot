package ru.illine.drinking.ponies.scheduler

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import ru.illine.drinking.ponies.dao.access.NotificationAccessService
import ru.illine.drinking.ponies.model.dto.NotificationDto
import ru.illine.drinking.ponies.service.NotificationService
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.stream.Collectors

@Component
class NotificationScheduler(
    private val notificationAccessService: NotificationAccessService,
    private val notificationService: NotificationService
) {

    private val MAX_NOTIFICATION_ATTEMPTS = 3

    private val FOR_NOTIFICATIONS = false
    private val FOR_CANCEL = true

    private val DEFAULT_TOO_LATE_TIME = LocalTime.of(23, 0)
    private val DEFAULT_TOO_EARLY_TIME = LocalTime.of(11, 0)

    private val log = LoggerFactory.getLogger("SCHEDULER")

    @Scheduled(cron = "\${telegram-bot.schedule.notification.cron}")
    fun drinkingNotification() {
        log.info("The Drinking Notification Scheduler is started")

        val now = LocalDateTime.now()
        val notifications = notificationAccessService.findAll()
            .stream()
            .filter { ifNotQuietModeTime(it) }
            .filter { ifTimeOfNotification(it, now) }
            .collect(Collectors.partitioningBy { it.notificationAttempts == MAX_NOTIFICATION_ATTEMPTS })
        notifyAll(notifications)
        cancelAll(notifications)

        log.info("The Drinking Notification Scheduler is finished")
    }

    private fun ifNotQuietModeTime(dto: NotificationDto): Boolean {
        log.debug("Checking of silence mode for a user with id: [{}]", dto.id)

        val quietModeStart = dto.quietModeStart
        val quietModeEnd = dto.quietModeEnd

        if (quietModeStart == null || quietModeEnd == null) {
            log.info("A user doesn't have a quiet mode, return true")
            return true
        }

        val userZoneId = ZoneId.of(dto.userTimeZone)
        val userDateTime = ZonedDateTime.now(userZoneId)

        log.debug("The user has a timezone: [{}] and a current time: [{}]", userZoneId, userDateTime)

        val late = userDateTime.toLocalTime().isBefore(quietModeStart)
        val early = userDateTime.toLocalTime().isAfter(quietModeEnd)

        log.debug("Now is before [{}]: [{}]", quietModeStart, late)
        log.debug("Now is after [{}]: [{}]", quietModeEnd, early)

        return late && early
    }

    private fun ifTimeOfNotification(dto: NotificationDto, timeForComparing: LocalDateTime): Boolean {
        log.debug("Checking of the last time of a notification for a user with id: [{}]", dto.id)
        log.debug("Now (UTC) is: [{}]", timeForComparing)
        log.debug("The last notification was (UTC): [{}]", dto.timeOfLastNotification)

        val expectedSentNotificationTime = dto.timeOfLastNotification.plusMinutes(dto.delayNotification.minutes)
        log.debug("The next notification should be (UTC) [{}]", expectedSentNotificationTime)

        val sending = expectedSentNotificationTime.isBefore(timeForComparing)
        log.debug("The notification should be sent: [{}]", sending)

        return sending
    }

    private fun notifyAll(notifications: Map<Boolean, List<NotificationDto>>) {
        log.info("Sending notifications all users")
        val forNotifying = notifications.getValue(FOR_NOTIFICATIONS)
        log.info("The notifications will be sent [{}] users", forNotifying.size)

        notificationService.sendNotifications(forNotifying)
    }

    private fun cancelAll(notifications: Map<Boolean, List<NotificationDto>>) {
        log.info("Cancel (suspend) notification")
        val forSuspending = notifications.getValue(FOR_CANCEL)
        log.info("The notifications will be suspended for [{}] users", forSuspending.size)

        notificationService.suspendNotifications(forSuspending)
    }
}