package ru.illine.drinking.ponies.test.generator

import ru.illine.drinking.ponies.model.base.AnswerNotificationType
import ru.illine.drinking.ponies.model.base.IntervalNotificationType
import ru.illine.drinking.ponies.model.dto.internal.NotificationSettingDto
import ru.illine.drinking.ponies.model.dto.internal.TelegramChatDto
import ru.illine.drinking.ponies.model.dto.internal.TelegramUserDto
import ru.illine.drinking.ponies.model.dto.internal.WaterStatisticDto
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.random.Random

class DtoGenerator {

    companion object {

        fun generateNotificationDto(
            externalUserId: Long = Random.nextLong(),
            externalChatId: Long = Random.nextLong(),
            notificationInterval: IntervalNotificationType = IntervalNotificationType.HOUR,
            timeOfLastNotification: LocalDateTime = LocalDateTime.now(),
            notificationAttempts: Int = 0,
            userTimeZone: String = "Europe/Moscow",
            previousNotificationMessageId: Int? = null,
            quietModeStart: LocalTime? = null,
            quietModeEnd: LocalTime? = null,
            pauseUntil: LocalDateTime? = null,
            dailyGoalMl: Int = 2000,
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
                notificationInterval = notificationInterval,
                timeOfLastNotification = timeOfLastNotification,
                notificationAttempts = notificationAttempts,
                quietModeStart = quietModeStart,
                quietModeEnd = quietModeEnd,
                pauseUntil = pauseUntil,
                dailyGoalMl = dailyGoalMl,
            )
        }

        fun generateWaterStatisticDto(
            externalUserId: Long = Random.nextLong(),
            eventTime: LocalDateTime = LocalDateTime.now(),
            eventType: AnswerNotificationType = AnswerNotificationType.YES,
            waterAmountMl: Int = 250,
            userTimeZone: String = "Europe/Moscow",
        ): WaterStatisticDto {
            val user = TelegramUserDto(
                externalUserId = externalUserId,
                userTimeZone = userTimeZone,
            )
            return WaterStatisticDto(
                telegramUser = user,
                eventTime = eventTime,
                eventType = eventType,
                waterAmountMl = waterAmountMl,
            )
        }
    }
}
