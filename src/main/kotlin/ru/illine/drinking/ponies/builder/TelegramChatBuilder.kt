package ru.illine.drinking.ponies.builder

import ru.illine.drinking.ponies.model.dto.internal.TelegramChatDto
import ru.illine.drinking.ponies.model.dto.internal.TelegramUserDto
import ru.illine.drinking.ponies.model.entity.TelegramChatEntity
import ru.illine.drinking.ponies.model.entity.TelegramUserEntity

object TelegramChatBuilder {

    fun toDto(chat: TelegramChatEntity, user: TelegramUserDto): TelegramChatDto =
        TelegramChatDto(
            id = chat.id,
            telegramUser = user,
            externalChatId = chat.externalChatId,
            previousNotificationMessageId = chat.previousNotificationMessageId,
        )

    fun toEntity(chat: TelegramChatDto, user: TelegramUserEntity): TelegramChatEntity =
        TelegramChatEntity(
            id = chat.id,
            externalChatId = chat.externalChatId,
            previousNotificationMessageId = chat.previousNotificationMessageId,
            telegramUser = user,
        )
}
