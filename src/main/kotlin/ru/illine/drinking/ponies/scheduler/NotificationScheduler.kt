package ru.illine.drinking.ponies.scheduler

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import ru.illine.drinking.ponies.bot.DrinkingPoniesTelegramBot
import ru.illine.drinking.ponies.dao.access.NotificationAccessService
import ru.illine.drinking.ponies.model.dto.NotificationDto
import java.time.OffsetDateTime
import java.util.stream.Collectors

@Component
class NotificationScheduler(
    private val notificationAccessService: NotificationAccessService,
    private val drinkingPoniesTelegramBot: DrinkingPoniesTelegramBot
) {

    private val MAX_NOTIFICATION_ATTEMPTS = 3
    private val FOR_NOTIFICATIONS = false
    private val FOR_CANCEL = true

    private val log = LoggerFactory.getLogger("SCHEDULER")

    //@Scheduled(fixedRate = 300000)
    @Scheduled(fixedRate = 30000)
    fun drinkingNotification() {
        log.info("The Drinking Notification Scheduler is started...")
        val notifications = notificationAccessService.findAll()
            .stream()
            .filter { compareLastSentNotification(it) }
            .collect(Collectors.partitioningBy { it.notificationAttempts == MAX_NOTIFICATION_ATTEMPTS })
        notifyAll(notifications)
        cancelAll(notifications)

        log.info("The Drinking Notification Scheduler is finished")
    }

    private fun compareLastSentNotification(it: NotificationDto): Boolean {
        val whenSendNotification = it.timeOfLastNotification.plusMinutes(it.delayNotification.minutes)
        return whenSendNotification.toEpochSecond() < OffsetDateTime.now().toEpochSecond()
    }

    private fun notifyAll(notifications: Map<Boolean, List<NotificationDto>>) {
        log.info("Sending notifications all users")
        val forNotifying = notifications.getValue(FOR_NOTIFICATIONS)
        log.info("The notifications will be sent [{}] users", forNotifying.size)

        drinkingPoniesTelegramBot.sendNotifications(forNotifying)
    }

    private fun cancelAll(notifications: Map<Boolean, List<NotificationDto>>) {
        log.info("Cancel (suspend) notification")
        val forSuspending = notifications.getValue(FOR_CANCEL)
        log.info("The notifications will be suspended for [{}] users", forSuspending.size)

        drinkingPoniesTelegramBot.suspendNotifications(forSuspending)
    }
}