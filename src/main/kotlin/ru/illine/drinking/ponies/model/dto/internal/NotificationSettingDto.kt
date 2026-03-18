package ru.illine.drinking.ponies.model.dto.internal

import ru.illine.drinking.ponies.model.base.DelayNotificationType
import java.time.LocalDateTime
import java.time.LocalTime

data class NotificationSettingDto(
    var id: Long? = null,

    val telegramUser: TelegramUserDto,

    val telegramChat: TelegramChatDto,

    val delayNotification: DelayNotificationType,

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
            delayNotification: DelayNotificationType = DelayNotificationType.TWO_HOURS,
            quietModeStart: LocalTime = LocalTime.of(23, 0),
            quietModeEnd: LocalTime = LocalTime.of(11, 0)
        ): NotificationSettingDto = NotificationSettingDto(
            telegramUser = telegramUser,
            telegramChat = telegramChat,
            delayNotification = delayNotification,
            quietModeStart = quietModeStart,
            quietModeEnd = quietModeEnd,
        )
    }
}
