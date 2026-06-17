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
        setting: NotificationSettingDto
    ): TelegramUserDto

    fun updateNotificationSettings(
        externalUserId: Long, notificationInterval: IntervalNotificationType
    ): NotificationSettingDto

    fun updateTimeOfLastNotification(externalUserId: Long, time: LocalDateTime): NotificationSettingDto

    fun updateNotificationSettings(settings: Collection<NotificationSettingDto>): Set<NotificationSettingDto>

    fun isEnabledNotifications(externalUserId: Long): Boolean

    fun enableNotifications(externalUserId: Long)

    fun disableNotifications(externalUserId: Long)

    fun changeQuietMode(externalUserId: Long, start: LocalTime, end: LocalTime)

    fun disableQuietMode(externalUserId: Long)

    fun changeTimezone(externalUserId: Long, timezone: String)

    fun setPause(externalUserId: Long, pauseUntil: LocalDateTime?): NotificationSettingDto

    fun updateDailyGoal(externalUserId: Long, goalMl: Int)

}
