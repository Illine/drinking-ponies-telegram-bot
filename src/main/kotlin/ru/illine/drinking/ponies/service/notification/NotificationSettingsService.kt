package ru.illine.drinking.ponies.service.notification

import ru.illine.drinking.ponies.model.base.IntervalNotificationType
import ru.illine.drinking.ponies.model.dto.internal.NotificationSettingDto
import java.time.LocalDateTime
import java.time.LocalTime

interface NotificationSettingsService {

    fun getNotificationSettings(telegramUserId: Long): NotificationSettingDto

    fun getAllNotificationSettings(): Collection<NotificationSettingDto>

    fun resetNotificationTimer(telegramUserId: Long, time: LocalDateTime): NotificationSettingDto

    fun changeInterval(telegramUserId: Long, telegramChatId: Long, notificationInterval: IntervalNotificationType): NotificationSettingDto

    fun changeQuietMode(userId: Long, messageId: Int, start: LocalTime, end: LocalTime)

    fun disableQuietMode(userId: Long)

}
