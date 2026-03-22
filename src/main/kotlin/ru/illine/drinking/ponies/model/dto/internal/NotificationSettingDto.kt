package ru.illine.drinking.ponies.model.dto.internal

import ru.illine.drinking.ponies.model.base.IntervalNotificationType
import java.time.LocalDateTime
import java.time.LocalTime

data class NotificationSettingDto(
    var id: Long? = null,

    val telegramUser: TelegramUserDto,

    val telegramChat: TelegramChatDto,

    val notificationInterval: IntervalNotificationType,

    var timeOfLastNotification: LocalDateTime = LocalDateTime.now(),

    var notificationAttempts: Int = 0,

    var quietModeStart: LocalTime? = null,

    var quietModeEnd: LocalTime? = null,

    var enabled: Boolean = true
) {
    companion object {
        fun create(
            telegramUser: TelegramUserDto,
            telegramChat: TelegramChatDto,
            notificationInterval: IntervalNotificationType = IntervalNotificationType.TWO_HOURS,
            quietModeStart: LocalTime = LocalTime.of(23, 0),
            quietModeEnd: LocalTime = LocalTime.of(11, 0)
        ): NotificationSettingDto = NotificationSettingDto(
            telegramUser = telegramUser,
            telegramChat = telegramChat,
            notificationInterval = notificationInterval,
            quietModeStart = quietModeStart,
            quietModeEnd = quietModeEnd,
        )
    }
}
