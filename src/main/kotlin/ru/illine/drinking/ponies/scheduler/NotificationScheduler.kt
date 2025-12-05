package ru.illine.drinking.ponies.scheduler

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import ru.illine.drinking.ponies.dao.access.NotificationAccessService
import ru.illine.drinking.ponies.model.dto.NotificationDto
import ru.illine.drinking.ponies.service.NotificationService
import ru.illine.drinking.ponies.service.NotificationTimeService

@Component
class NotificationScheduler(
    private val notificationAccessService: NotificationAccessService,
    private val notificationService: NotificationService,
    private val notificationTimeService: NotificationTimeService
) {

    private val MAX_REMINDED_NOTIFICATION_ATTEMPTS = 3

    private val log = LoggerFactory.getLogger("SCHEDULER")

    @Scheduled(cron = "\${telegram-bot.schedule.notification.cron}")
    fun sendDrinkingReminders() {
        log.info("Starting drinking notification scheduler")

        val (exhaustedNotifications, activeNotifications) = notificationAccessService.findAll()
            .filter(notificationTimeService::isOutsideQuietTime)
            .filter { notificationTimeService.isNotificationDue(it) }
            .partition { it.notificationAttempts == MAX_REMINDED_NOTIFICATION_ATTEMPTS }

        cancelAll(exhaustedNotifications)
        notifyAll(activeNotifications)

        log.info("Drinking notification scheduler finished")
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