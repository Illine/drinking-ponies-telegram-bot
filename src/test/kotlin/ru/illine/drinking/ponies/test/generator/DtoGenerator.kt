package ru.illine.drinking.ponies.test.generator

import ru.illine.drinking.ponies.model.base.DelayNotificationType
import ru.illine.drinking.ponies.model.dto.NotificationDto
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.random.Random

class DtoGenerator {

    companion object {

        fun generateNotificationDto(
            userId: Long = Random.nextLong(),
            chatId: Long = Random.nextLong(),
            delayNotification: DelayNotificationType = DelayNotificationType.HOUR,
            timeOfLastNotification: LocalDateTime = LocalDateTime.now(),
            notificationAttempts: Int = 0,
            userTimeZone: String = "Europe/Moscow",
            previousNotificationMessageId: Int? = null,
            quietModeStart: LocalTime? = null,
            quietModeEnd: LocalTime? = null,
        ): NotificationDto {
            return NotificationDto(
                userId = userId,
                chatId = chatId,
                delayNotification = delayNotification,
                timeOfLastNotification = timeOfLastNotification,
                notificationAttempts = notificationAttempts,
                userTimeZone = userTimeZone,
                previousNotificationMessageId = previousNotificationMessageId,
                quietModeStart = quietModeStart,
                quietModeEnd = quietModeEnd
            )
        }
    }
}