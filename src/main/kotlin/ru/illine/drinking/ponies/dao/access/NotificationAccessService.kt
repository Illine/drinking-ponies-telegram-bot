package ru.illine.drinking.ponies.dao.access

import ru.illine.drinking.ponies.model.base.TimeNotificationType
import ru.illine.drinking.ponies.model.dto.internal.NotificationSettingDto
import ru.illine.drinking.ponies.model.dto.internal.TelegramChatDto
import ru.illine.drinking.ponies.model.dto.internal.TelegramUserDto
import java.time.LocalDateTime
import java.time.LocalTime

interface NotificationAccessService {

    fun findAllNotificationSettings(): Set<NotificationSettingDto>

    fun findNotificationSettingByTelegramUserId(telegramUserId: Long): NotificationSettingDto

    fun existsByTelegramUserId(telegramUserId: Long): Boolean

    fun save(
        user: TelegramUserDto,
        chat: TelegramChatDto,
        setting: NotificationSettingDto
    ): TelegramUserDto

    fun updateNotificationSettings(
        telegramUserId: Long, telegramChatId: Long, delayNotification: TimeNotificationType
    ): NotificationSettingDto

    fun updateTimeOfLastNotification(telegramUserId: Long, time: LocalDateTime): NotificationSettingDto

    fun updateNotificationSettings(settings: Collection<NotificationSettingDto>): Set<NotificationSettingDto>

    fun isEnabledNotifications(telegramUserId: Long): Boolean

    fun enableNotifications(telegramUserId: Long)

    fun disableNotifications(telegramUserId: Long)

    fun changeQuiteMode(userId: Long, start: LocalTime, end: LocalTime)

    fun disableQuietMode(userId: Long)

}