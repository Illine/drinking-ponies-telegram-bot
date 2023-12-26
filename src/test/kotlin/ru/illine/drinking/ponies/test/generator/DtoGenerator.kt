package ru.illine.drinking.ponies.test.generator

import net.datafaker.Faker
import ru.illine.drinking.ponies.model.base.DelayNotificationType
import ru.illine.drinking.ponies.model.dto.NotificationDto
import java.time.LocalDateTime
import kotlin.random.Random

class DtoGenerator {

    companion object {

        private val faker = Faker()

        fun generateNotificationDto(
            userId: Long = Random.nextLong(),
            chatId: Long = Random.nextLong(),
            delayNotification: DelayNotificationType = DelayNotificationType.HOUR,
            timeOfLastNotification: LocalDateTime = LocalDateTime.now(),
            notificationAttempts: Int = 0,
            userTimeZone: String = "Europe/Moscow",
            previousNotificationMessageId: Int? = null
        ): NotificationDto {
            return NotificationDto(
                userId = userId,
                chatId = chatId,
                delayNotification = delayNotification,
                timeOfLastNotification = timeOfLastNotification,
                notificationAttempts = notificationAttempts,
                userTimeZone = userTimeZone,
                previousNotificationMessageId = previousNotificationMessageId
            )
        }
    }
}