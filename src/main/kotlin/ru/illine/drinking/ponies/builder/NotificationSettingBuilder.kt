package ru.illine.drinking.ponies.builder

import ru.illine.drinking.ponies.model.dto.internal.NotificationSettingDto
import ru.illine.drinking.ponies.model.dto.internal.TelegramChatDto
import ru.illine.drinking.ponies.model.dto.internal.TelegramUserDto
import ru.illine.drinking.ponies.model.entity.NotificationSettingEntity
import ru.illine.drinking.ponies.model.entity.TelegramChatEntity
import ru.illine.drinking.ponies.model.entity.TelegramUserEntity

object NotificationSettingBuilder {
    fun toDto(
        setting: NotificationSettingEntity,
        user: TelegramUserDto,
        chat: TelegramChatDto
    ): NotificationSettingDto =
        NotificationSettingDto(
            id = setting.id,
            telegramUser = user,
            telegramChat = chat,
            delayNotification = setting.delayNotification,
            timeOfLastNotification = setting.timeOfLastNotification,
            notificationAttempts = setting.notificationAttempts,
            quietModeStart = setting.quietModeStart,
            quietModeEnd = setting.quietModeEnd,
            enabled = setting.enabled,
        )

    fun toEntity(
        setting: NotificationSettingDto,
        user: TelegramUserEntity,
        chat: TelegramChatEntity
    ): NotificationSettingEntity {

        val setting = NotificationSettingEntity(
            id = setting.id,
            telegramUser = user,
            telegramChat = chat,
            delayNotification = setting.delayNotification,
            timeOfLastNotification = setting.timeOfLastNotification,
            notificationAttempts = setting.notificationAttempts,
            quietModeStart = setting.quietModeStart,
            quietModeEnd = setting.quietModeEnd,
            enabled = setting.enabled,
        )

        user.notificationSettings += setting
        user.telegramChats += chat

        return setting
    }
}
