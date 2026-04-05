package ru.illine.drinking.ponies.scheduler

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import ru.illine.drinking.ponies.dao.access.NotificationAccessService
import ru.illine.drinking.ponies.model.dto.internal.NotificationSettingDto
import ru.illine.drinking.ponies.service.notification.NotificationSenderService
import ru.illine.drinking.ponies.service.notification.NotificationTimeService

@Component
class NotificationScheduler(
    private val notificationAccessService: NotificationAccessService,
    private val notificationSenderService: NotificationSenderService,
    private val notificationTimeService: NotificationTimeService
) {

    private val MAX_REMINDED_NOTIFICATION_ATTEMPTS = 3

    private val logger = LoggerFactory.getLogger("SCHEDULER")

    @Scheduled(cron = "\${telegram-bot.schedule.notification.cron}")
    fun sendDrinkingReminders() {
        logger.info("Starting drinking notification scheduler")

        try {
            val (exhaustedNotifications, activeNotifications) = notificationAccessService.findAllNotificationSettings()
                .filter { it.enabled }
                .filter(notificationTimeService::isOutsideQuietTime)
                .filter { notificationTimeService.isNotificationDue(it) }
                .partition { it.notificationAttempts == MAX_REMINDED_NOTIFICATION_ATTEMPTS }

            cancelAll(exhaustedNotifications)
            notifyAll(activeNotifications)

            logger.info("Drinking notification scheduler finished")
        } catch (e: Exception) {
            logger.error("Drinking notification scheduler failed", e)
        }
    }

    private fun notifyAll(notifications: List<NotificationSettingDto>) {
        if (notifications.isEmpty()) {
            return
        }

        logger.info("Sending notifications to [{}] users", notifications.size)
        notificationSenderService.sendNotifications(notifications)
    }

    private fun cancelAll(notifications: List<NotificationSettingDto>) {
        if (notifications.isEmpty()) {
            return
        }

        logger.info("Suspending notifications for [{}] users (max attempts reached)", notifications.size)
        notificationSenderService.suspendNotifications(notifications)
    }
}