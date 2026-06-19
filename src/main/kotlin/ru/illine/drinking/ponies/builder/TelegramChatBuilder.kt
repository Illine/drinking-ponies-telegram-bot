package ru.illine.drinking.ponies.builder

import io.mcarle.konvert.api.Konverter
import ru.illine.drinking.ponies.model.dto.internal.TelegramChatDto
import ru.illine.drinking.ponies.model.dto.internal.TelegramUserDto
import ru.illine.drinking.ponies.model.entity.TelegramChatEntity
import ru.illine.drinking.ponies.model.entity.TelegramUserEntity

@Konverter
interface TelegramChatMapper {
    fun toDto(
        @Konverter.Source chat: TelegramChatEntity,
        telegramUser: TelegramUserDto,
    ): TelegramChatDto

    fun toEntity(
        @Konverter.Source chat: TelegramChatDto,
        telegramUser: TelegramUserEntity,
    ): TelegramChatEntity
}

object TelegramChatBuilder {
    private val mapper: TelegramChatMapper = Konverter.get<TelegramChatMapper>()

    fun toDto(chat: TelegramChatEntity, user: TelegramUserDto): TelegramChatDto =
        mapper.toDto(chat, user)

    fun toEntity(chat: TelegramChatDto, user: TelegramUserEntity): TelegramChatEntity =
        mapper.toEntity(chat, user)
}
