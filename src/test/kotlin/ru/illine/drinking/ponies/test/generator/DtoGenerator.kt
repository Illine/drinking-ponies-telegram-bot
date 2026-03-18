package ru.illine.drinking.ponies.test.generator

import ru.illine.drinking.ponies.model.base.DelayNotificationType
import ru.illine.drinking.ponies.model.dto.internal.NotificationSettingDto
import ru.illine.drinking.ponies.model.dto.internal.TelegramChatDto
import ru.illine.drinking.ponies.model.dto.internal.TelegramUserDto
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.random.Random

class DtoGenerator {

    companion object {

        fun generateNotificationDto(
            externalUserId: Long = Random.nextLong(),
            externalChatId: Long = Random.nextLong(),
            delayNotification: DelayNotificationType = DelayNotificationType.HOUR,
            timeOfLastNotification: LocalDateTime = LocalDateTime.now(),
            notificationAttempts: Int = 0,
            userTimeZone: String = "Europe/Moscow",
            previousNotificationMessageId: Int? = null,
            quietModeStart: LocalTime? = null,
            quietModeEnd: LocalTime? = null,
        ): NotificationSettingDto {
            val user = TelegramUserDto(
                externalUserId = externalUserId,
                userTimeZone = userTimeZone,
            )
            val chat = TelegramChatDto(
                telegramUser = user,
                externalChatId = externalChatId,
                previousNotificationMessageId = previousNotificationMessageId,
            )
            return NotificationSettingDto(
                telegramUser = user,
                telegramChat = chat,
                delayNotification = delayNotification,
                timeOfLastNotification = timeOfLastNotification,
                notificationAttempts = notificationAttempts,
                quietModeStart = quietModeStart,
                quietModeEnd = quietModeEnd,
            )
        }
    }
}
