package ru.illine.drinking.ponies.dao.access

import ru.illine.drinking.ponies.model.base.IntervalNotificationType
import ru.illine.drinking.ponies.model.dto.internal.NotificationSettingDto
import ru.illine.drinking.ponies.model.dto.internal.TelegramChatDto
import ru.illine.drinking.ponies.model.dto.internal.TelegramUserDto
import java.time.LocalDateTime
import java.time.LocalTime

interface NotificationAccessService {
    fun findAllNotificationSettings(): Set<NotificationSettingDto>

    fun findNotificationSettingByExternalUserId(externalUserId: Long): NotificationSettingDto

    fun existsByExternalUserId(externalUserId: Long): Boolean

    fun save(
        user: TelegramUserDto,
        chat: TelegramChatDto,
        setting: NotificationSettingDto,
    ): TelegramUserDto

    fun updateNotificationSettings(
        externalUserId: Long,
        notificationInterval: IntervalNotificationType,
    ): NotificationSettingDto

    fun updateTimeOfLastNotification(
        externalUserId: Long,
        time: LocalDateTime,
    ): NotificationSettingDto

    fun updateNotificationSettings(settings: Collection<NotificationSettingDto>): Set<NotificationSettingDto>

    fun findIsEnabledNotificationsByExternalUserId(externalUserId: Long): Boolean

    fun updateNotificationsEnabled(externalUserId: Long)

    fun updateNotificationsDisabled(externalUserId: Long)

    fun updateQuietMode(
        externalUserId: Long,
        start: LocalTime,
        end: LocalTime,
    )

    fun updateQuietModeDisabled(externalUserId: Long)

    fun updateTimezone(
        externalUserId: Long,
        timezone: String,
    )

    fun updatePause(
        externalUserId: Long,
        pauseUntil: LocalDateTime?,
    ): NotificationSettingDto

    fun updateDailyGoal(
        externalUserId: Long,
        goalMl: Int,
    )
}
