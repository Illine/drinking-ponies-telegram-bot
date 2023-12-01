package ru.illine.drinking.ponies.scheduler

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import ru.illine.drinking.ponies.bot.DrinkingPoniesTelegramBot
import ru.illine.drinking.ponies.dao.access.NotificationAccessService
import ru.illine.drinking.ponies.model.dto.NotificationDto
import java.time.OffsetDateTime

@Component
class NotificationScheduler(
    private val notificationAccessService: NotificationAccessService,
    private val drinkingPoniesTelegramBot: DrinkingPoniesTelegramBot
) {

    private val log = LoggerFactory.getLogger("SCHEDULER")

    @Scheduled(fixedRate = 300000)
    fun drinkingNotification() {
        log.info("The Drinking Notification Scheduler is started...")
        notificationAccessService.findAll()
            .stream()
            .filter { compareLastSentNotification(it) }
            .forEach {
                drinkingPoniesTelegramBot.notify(it.chatId)
            }
    }

    private fun compareLastSentNotification(it: NotificationDto): Boolean {
        val whenSendNotification = it.timeOfLastNotification.plusMinutes(it.delayNotification.minutes)
        return whenSendNotification.toEpochSecond() < OffsetDateTime.now().toEpochSecond()
    }

}