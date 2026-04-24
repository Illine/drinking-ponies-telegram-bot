package ru.illine.drinking.ponies.service.notification

import ru.illine.drinking.ponies.model.base.IntervalNotificationType
import ru.illine.drinking.ponies.model.dto.SettingDto
import ru.illine.drinking.ponies.model.dto.internal.NotificationSettingDto
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime

interface NotificationSettingsService {

    fun getAllSettings(telegramUserId: Long): SettingDto

    fun getNextNotificationAt(telegramUserId: Long): Instant

    fun getNotificationSettings(telegramUserId: Long): NotificationSettingDto

    fun getAllNotificationSettings(): Collection<NotificationSettingDto>

    fun getQuietMode(telegramUserId: Long): Pair<LocalTime, LocalTime>

    fun resetNotificationTimer(telegramUserId: Long, time: LocalDateTime): NotificationSettingDto

    fun changeInterval(telegramUserId: Long, notificationInterval: IntervalNotificationType): NotificationSettingDto

    fun changeQuietMode(userId: Long, start: LocalTime, end: LocalTime)

    fun disableQuietMode(userId: Long)

    fun isEnabledNotifications(telegramUserId: Long): Boolean

    fun changeNotificationStatus(telegramUserId: Long, active: Boolean)

    fun changeTimezone(telegramUserId: Long, timezone: String)

}
