package ru.illine.drinking.ponies.scheduler

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import ru.illine.drinking.ponies.dao.access.NotificationAccessService
import ru.illine.drinking.ponies.model.dto.NotificationDto
import ru.illine.drinking.ponies.service.NotificationService
import java.time.*
import java.util.stream.Collectors

@Component
class NotificationScheduler(
    private val notificationAccessService: NotificationAccessService,
    private val notificationService: NotificationService
) {

    private val MAX_NOTIFICATION_ATTEMPTS = 3
    private val FOR_NOTIFICATIONS = false
    private val FOR_CANCEL = true

    private val log = LoggerFactory.getLogger("SCHEDULER")

    @Scheduled(cron = "\${telegram-bot.schedule.notification.cron}")
    fun drinkingNotification() {
        log.info("The Drinking Notification Scheduler is started")
        val notifications = notificationAccessService.findAll()
            .stream()
            .filter { ifTimeOfNotification(it) }
            .filter { ifNotSilenceTime(it) }
            .collect(Collectors.partitioningBy { it.notificationAttempts == MAX_NOTIFICATION_ATTEMPTS })
        notifyAll(notifications)
        cancelAll(notifications)

        log.info("The Drinking Notification Scheduler is finished")
    }

    private fun ifTimeOfNotification(dto: NotificationDto): Boolean {
        val expectedSentNotificationTime = dto.timeOfLastNotification.plusMinutes(dto.delayNotification.minutes)
        return expectedSentNotificationTime.isBefore(OffsetDateTime.now())
    }

    private fun ifNotSilenceTime(dto: NotificationDto): Boolean {
        val userZoneId = ZoneId.of(dto.userTimeZone)
        val userDateTime = ZonedDateTime.now(userZoneId)

        return userDateTime.toLocalTime().isBefore(LocalTime.of(23, 0))
                && userDateTime.toLocalTime().isAfter(LocalTime.of(11, 0))
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